/*
 * Copyright (C) 2022 Edgar Asatryan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.nstdio.http.ext;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;

import static io.github.nstdio.http.ext.Preconditions.checkArgument;
import static io.github.nstdio.http.ext.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("WeakerAccess")
public interface Cache {
  /**
   * Creates a new {@code InMemoryCacheBuilder} instance.
   *
   * @return the new {@code  InMemoryCacheBuilder}.
   */
  static InMemoryCacheBuilder newInMemoryCacheBuilder() {
    return new InMemoryCacheBuilder();
  }

  /**
   * Creates a new {@code DiskCacheBuilder} instance. Requires Jackson form dumping cache files on disk.
   *
   * @return the new {@code  DiskCacheBuilder}.
   *
   * @throws IllegalStateException When Jackson (a.k.a. ObjectMapper) is not in classpath.
   */
  static DiskCacheBuilder newDiskCacheBuilder() {
    MetadataSerializer.requireAvailability();

    return new DiskCacheBuilder();
  }

  /**
   * Gets the {@code Cache} effectively does not do anything.
   *
   * @return a stub cache.
   */
  static Cache noop() {
    return NullCache.INSTANCE;
  }

  /**
   * Gets the cache entry associated with {@code request}.
   *
   * @param request The request.
   *
   * @return the cache entry associated with {@code request} or {@code null}.
   */
  CacheEntry get(HttpRequest request);

  /**
   * Associates {@code request} with {@code entry} and stores it in this cache. The previously (if any) associated entry
   * will be evicted.
   *
   * @param request The request.
   * @param entry   The entry.
   */
  void put(HttpRequest request, CacheEntry entry);

  /**
   * Evicts cache entry (if any) associated with {@code  request}.
   *
   * @param request The request.
   */
  void evict(HttpRequest request);

  /**
   * Evicts all cache entries associated with {@code request}.
   *
   * @param request The request.
   */
  void evictAll(HttpRequest request);

  /**
   * Removes all cache entries. This cache will be empty after method invocation.
   */
  void evictAll();

  /**
   * Gets the statistics for this cache.
   *
   * @return The statistics for this cache.
   *
   * @see CacheStats
   */
  CacheStats stats();

  /**
   * Creates a {@code Writer}.
   *
   * @param metadata The metadata.
   * @param <T>      The response body type.
   *
   * @return a {@code Writer}.
   */
  <T> Writer<T> writer(CacheEntryMetadata metadata);

  interface Writer<T> {
    /**
     * The body subscriber to collect response body.
     *
     * @return The body subscriber.
     */
    BodySubscriber<T> subscriber();

    /**
     * The consumer to be invoked after response body fully read.
     *
     * @return The response body consumer.
     */
    Consumer<T> finisher();
  }

  interface CacheEntry {
    void subscribeTo(Subscriber<List<ByteBuffer>> sub);

    CacheEntryMetadata metadata();

    default long bodySize() {
      return -1;
    }
  }

  interface CacheBuilder {
    Cache build();
  }

  interface CacheStats {
    /**
     * The number the cache serves stored response.
     *
     * @return The number of times the cache serves stored response.
     */
    long hit();

    /**
     * The number the cache does not have stored response which resulted in network call.
     *
     * @return The number the cache does not have stored response which resulted in network call.
     */
    long miss();
  }

  /**
   * The builder for in memory cache.
   */
  class InMemoryCacheBuilder extends ConstrainedCacheBuilder<InMemoryCacheBuilder> {
    InMemoryCacheBuilder() {
    }

    @Override
    public Cache build() {
      return build(new InMemoryCache(maxItems, size));
    }
  }

  /**
   * The builder for in persistent cache.
   */
  class DiskCacheBuilder extends ConstrainedCacheBuilder<DiskCacheBuilder> {
    Path dir;

    DiskCacheBuilder() {
    }

    /**
     * Sets the directory to store cache files.
     *
     * @param dir The directory to store cache files.
     *
     * @return builder itself.
     */
    public DiskCacheBuilder dir(Path dir) {
      this.dir = requireNonNull(dir);
      return this;
    }

    /**
     * Creates a new {@code EncryptedDiskCacheBuilder} instance which will create {@link Cache} that stores all cache
     * files encrypted by provided keys.
     *
     * @return a newly created builder.
     */
    public EncryptedDiskCacheBuilder encrypted() {
      return new EncryptedDiskCacheBuilder(this);
    }

    StreamFactory newStreamFactory() {
      return new SimpleStreamFactory();
    }

    @Override
    public Cache build() {
      checkState(dir != null, "dir cannot be null");

      var streamFactory = newStreamFactory();
      var serializer = MetadataSerializer.findAvailable(streamFactory);

      return build(new DiskCache(size, maxItems, serializer, streamFactory, dir));
    }
  }

  /**
   * The {@link DiskCacheBuilder} that will create {@link Cache} that maintains all files in encrypted manner. The
   * request, response body, response headers all will be stored encrypted by user provided keys.
   */
  final class EncryptedDiskCacheBuilder extends DiskCacheBuilder {
    private Key publicKey;
    private Key privateKey;
    private String cipherAlgorithm;
    private String provider;

    EncryptedDiskCacheBuilder(DiskCacheBuilder b) {
      this.dir = b.dir;
      this.size = b.size;
      this.maxItems = b.maxItems;
      this.responseFilter = b.responseFilter;
      this.requestFilter = b.requestFilter;
    }

    /**
     * {@inheritDoc}
     */
    public EncryptedDiskCacheBuilder encrypted() {
      return this;
    }

    /**
     * The secret key to encrypt/decrypt all files that this cache maintains.
     *
     * @param key The encryption key. Never null.
     *
     * @return builder itself.
     */
    public EncryptedDiskCacheBuilder key(SecretKey key) {
      this.privateKey = requireNonNull(key, "key cannot be null");
      this.publicKey = key;
      return this;
    }

    /**
     * Sets the public key to use.
     *
     * @param key The encryption key. Never null.
     *
     * @return builder itself.
     */
    public EncryptedDiskCacheBuilder publicKey(PublicKey key) {
      this.publicKey = requireNonNull(key, "publicKey cannot be null");
      return this;
    }

    /**
     * Sets the private key to use.
     *
     * @param key The encryption key. Never null.
     *
     * @return builder itself.
     */
    public EncryptedDiskCacheBuilder privateKey(PrivateKey key) {
      this.privateKey = requireNonNull(key, "privateKey cannot be null");
      return this;
    }

    /**
     * The cipher algorithm to use. Typically, has form of "algorithm/mode/padding" or "algorithm". Note that
     * "NoPadding" padding scheme is not supported.
     *
     * @param algorithm The algorithm name.
     *
     * @return builder itself.
     *
     * @throws IllegalArgumentException if {@code algorithm} is null, or contains "NoPadding" padding scheme.
     * @throws NoSuchAlgorithmException if algorithm empty, is in an invalid format, or if no Provider supports a
     *                                  CipherSpi implementation for the specified algorithm.
     * @throws NoSuchPaddingException   if algorithm contains a padding scheme that is not available.
     * @see javax.crypto.Cipher#getInstance(String)
     */
    public EncryptedDiskCacheBuilder cipherAlgorithm(String algorithm) throws NoSuchPaddingException, NoSuchAlgorithmException {
      this.cipherAlgorithm = validAlgorithm(algorithm);

      return this;
    }

    private String validAlgorithm(String algo) throws NoSuchPaddingException, NoSuchAlgorithmException {
      checkArgument(algo != null, "algorithm cannot be null");
      String[] parts = algo.split("/");
      if (parts.length == 3 && parts[2].equals("NoPadding")) {
        throw new IllegalArgumentException("NoPadding transformations are not supported");
      }

      Cipher.getInstance(algo);
      return algo;
    }

    /**
     * The {@link java.security.Provider} name to use.
     *
     * @param provider The provider name.
     *
     * @return builder itself.
     *
     * @see javax.crypto.Cipher#getInstance(String, String)
     */
    public EncryptedDiskCacheBuilder provider(String provider) {
      checkArgument(provider != null, "provider cannot be null");
      this.provider = provider;
      return this;
    }

    @Override
    StreamFactory newStreamFactory() {
      checkState(publicKey != null && privateKey != null, "specify keypair or secret");
      checkState(cipherAlgorithm != null, "algorithm cannot be null");

      var delegate = super.newStreamFactory();
      return new EncryptedStreamFactory(delegate, publicKey, privateKey, cipherAlgorithm, provider);
    }
  }
}
