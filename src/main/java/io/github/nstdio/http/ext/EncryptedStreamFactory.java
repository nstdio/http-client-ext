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
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.github.nstdio.http.ext.IOUtils.closeQuietly;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * The strategy to create on-the-fly encrypted/decrypted InputStream/OutputStreams from provided paths. The algorithm
 * parameters (if any) are stores within the file itself as first few bytes.
 */
class EncryptedStreamFactory implements StreamFactory {
  private static final ThreadLocal<Map<CipherCacheKey, Cipher>> threadLocalCipher = ThreadLocal.withInitial(() -> new HashMap<>(1));

  private final StreamFactory delegate;
  private final Key publicKey;
  private final Key privateKey;
  private final String transformation;
  private final String algorithm;
  private final String provider;
  private final CipherCacheKey cacheKey;

  public EncryptedStreamFactory(StreamFactory delegate, Key publicKey, Key privateKey, String transformation, String provider) {
    this.delegate = delegate;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.transformation = transformation;
    this.algorithm = algo(transformation);
    this.provider = provider;
    this.cacheKey = new CipherCacheKey(transformation, provider);
  }

  private static String algo(String transformation) {
    int i = transformation.indexOf('/');
    return i == -1 ? transformation : transformation.substring(0, i);
  }

  static void clear() {
    threadLocalCipher.get().clear();
  }

  private Cipher createCipher() throws IOException {
    try {
      return hasProvider()
          ? Cipher.getInstance(transformation, provider)
          : Cipher.getInstance(transformation);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new IOException(e);
    }
  }

  private Cipher cipher() throws IOException {
    Map<CipherCacheKey, Cipher> cipherCache = threadLocalCipher.get();
    CipherCacheKey k = cacheKey;

    // cannot use computeIfAbsent because if checked exception
    Cipher c = cipherCache.get(k);
    if (c == null) {
      c = createCipher();
      cipherCache.put(k, c);
    }

    return c;
  }

  private boolean hasProvider() {
    return provider != null;
  }

  private AlgorithmParameters algorithmParameters() throws NoSuchAlgorithmException, NoSuchProviderException {
    return hasProvider()
        ? AlgorithmParameters.getInstance(algorithm, provider)
        : AlgorithmParameters.getInstance(algorithm);
  }

  private Cipher encryptCipher() throws IOException {
    Cipher cipher = cipher();
    try {
      cipher.init(ENCRYPT_MODE, privateKey);
    } catch (InvalidKeyException e) {
      throw new IOException(e);
    }

    return cipher;
  }

  @Override
  public OutputStream output(Path path, OpenOption... options) throws IOException {
    Cipher cipher = encryptCipher();
    OutputStream out = delegate.output(path, options);

    writeParams(cipher, out);

    return new CipherOutputStream(out, cipher);
  }

  @Override
  public InputStream input(Path path, OpenOption... options) throws IOException {
    InputStream is = delegate.input(path, options);
    Cipher c;
    try {
      c = cipher();
      c.init(DECRYPT_MODE, publicKey, readParams(is, c));
    } catch (Exception e) {
      closeQuietly(is, e);

      throw asIOException(e);
    }

    return new CipherInputStream(is, c);
  }

  private void writeParams(Cipher cipher, OutputStream out) throws IOException {
    var params = cipher.getParameters();

    if (params != null) {
      try {
        byte[] encodedParams = params.getEncoded();
        var dataOut = new DataOutputStream(out);
        dataOut.writeInt(encodedParams.length);
        dataOut.write(encodedParams);

      } catch (IOException e) {
        closeQuietly(out, e);
        throw e;
      }
    }
  }

  private AlgorithmParameters readParams(InputStream is, Cipher c) throws Exception {
    if (c.getParameters() != null) {
      try {
        int len = readInt(is);
        byte[] encodedParams = is.readNBytes(len);
        var parameters = algorithmParameters();
        parameters.init(encodedParams);

        return parameters;
      } catch (Exception e) {
        closeQuietly(is, e);
        throw e;
      }
    }

    return null;
  }

  /**
   * It's not worth to instantiate {@link DataInputStream}.
   */
  private int readInt(InputStream is) throws IOException {
    int ch1 = is.read();
    int ch2 = is.read();
    int ch3 = is.read();
    int ch4 = is.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0)
      throw new EOFException();

    return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
  }

  private IOException asIOException(Exception e) {
    if (e instanceof IOException)
      return ((IOException) e);

    return new IOException(e);
  }

  static final class CipherCacheKey {
    private final String transformation;
    private final String provider;

    private CipherCacheKey(String transformation, String provider) {
      this.transformation = transformation;
      this.provider = provider;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CipherCacheKey that = (CipherCacheKey) o;
      return transformation.equals(that.transformation) && Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
      return Objects.hash(transformation, provider);
    }
  }
}
