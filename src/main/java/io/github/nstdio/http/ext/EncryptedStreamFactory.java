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

import static io.github.nstdio.http.ext.IOUtils.closeQuietly;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * The strategy to create on-the-fly encrypted/decrypted InputStream/OutputStreams from provided paths. The algorithm
 * parameters (if any) are stores within the file itself as first few bytes.
 */
class EncryptedStreamFactory implements StreamFactory {
  private final StreamFactory delegate;
  private final Key publicKey;
  private final Key privateKey;
  private final String transformation;
  private final String algorithm;
  private final String provider;
  private final ThreadLocal<Cipher> threadLocalCipher;

  public EncryptedStreamFactory(StreamFactory delegate, Key publicKey, Key privateKey, String transformation, String provider) {
    this.delegate = delegate;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.transformation = transformation;
    this.algorithm = algo(transformation);
    this.provider = provider;
    this.threadLocalCipher = new ThreadLocal<>();
  }

  private static String algo(String transformation) {
    int i = transformation.indexOf('/');
    return i == -1 ? transformation : transformation.substring(0, i);
  }

  private Cipher createCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
    return hasProvider()
        ? Cipher.getInstance(transformation, provider)
        : Cipher.getInstance(transformation);
  }

  private Cipher cipher() throws IOException {
    var tl = threadLocalCipher;

    if (tl.get() == null) {
      Cipher c;
      try {
        c = createCipher();
      } catch (NoSuchPaddingException | NoSuchAlgorithmException | NoSuchProviderException e) {
        throw new IOException(e);
      }
      tl.set(c);
      return c;
    }

    return tl.get();
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
        int len = new DataInputStream(is).readInt();
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

  private IOException asIOException(Exception e) {
    if (e instanceof IOException)
      return ((IOException) e);

    return new IOException(e);
  }
}
