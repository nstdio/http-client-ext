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

import io.github.nstdio.http.ext.spi.JsonMappingProvider;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;

/**
 * Implementations of various useful {@link BodyPublisher}s.
 */
public final class BodyPublishers {
  private BodyPublishers() {
  }

  /**
   * Returns a request body publisher whose body is JSON representation of {@code body}. The conversion will be done
   * using {@code JsonMappingProvider} default provider retrieved using {@link JsonMappingProvider#provider()}.
   *
   * @param body The body.
   *
   * @return a BodyPublisher
   */
  public static BodyPublisher ofJson(Object body) {
    return ofJson(body, JsonMappingProvider.provider());
  }

  /**
   * Returns a request body publisher whose body is JSON representation of {@code body}. The conversion will be done
   * using {@code jsonProvider}.
   *
   * @param body         The body.
   * @param jsonProvider The JSON mapping provider to use when creating JSON presentation of {@code body}.
   *
   * @return a BodyPublisher
   */
  public static BodyPublisher ofJson(Object body, JsonMappingProvider jsonProvider) {
    return ofJson(body, jsonProvider, null);
  }

  /**
   * Returns a request body publisher whose body is JSON representation of {@code body}. The conversion will be done
   * using {@code JsonMappingProvider} default provider retrieved using {@link JsonMappingProvider#provider()}.
   *
   * @param body     The body.
   * @param executor The scheduler to use to publish body to subscriber. If {@code null} the *
   *                 {@link ForkJoinPool#commonPool()} will be used.
   *
   * @return a BodyPublisher
   */
  public static BodyPublisher ofJson(Object body, Executor executor) {
    return ofJson(body, JsonMappingProvider.provider(), executor);
  }

  /**
   * Returns a request body publisher whose body is JSON representation of {@code body}. The conversion will be done *
   * using {@code jsonProvider}.
   *
   * @param body         The body.
   * @param jsonProvider The JSON mapping provider to use when creating JSON presentation of {@code body}.
   * @param executor     The scheduler to use to publish body to subscriber. If {@code null} the
   *                     {@link ForkJoinPool#commonPool()} will be used.
   *
   * @return a BodyPublisher
   */
  public static BodyPublisher ofJson(Object body, JsonMappingProvider jsonProvider, Executor executor) {
    return new JsonPublisher(body, jsonProvider, Optional.ofNullable(executor).orElseGet(ForkJoinPool::commonPool));
  }

  /**
   * The {@code BodyPublisher} that converts objects to JSON.
   */
  static final class JsonPublisher implements BodyPublisher {
    private final Object body;
    private final JsonMappingProvider provider;
    private final Executor executor;
    private volatile CompletableFuture<byte[]> result;

    JsonPublisher(Object body, JsonMappingProvider provider, Executor executor) {
      this.body = body;
      this.provider = provider;
      this.executor = executor;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
      var subscription = ByteArraySubscription.ofByteBuffer(subscriber, this::resultUncheckedGet, executor);

      subscriber.onSubscribe(subscription);
    }

    private byte[] resultUncheckedGet() {
      try {
        return result.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    private byte[] json() {
      var os = new ByteArrayOutputStream();
      try {
        provider.get().write(body, os);
      } catch (Throwable e) {
        Throwables.sneakyThrow(e);
      }

      return os.toByteArray();
    }

    @Override
    public long contentLength() {
      if (result == null) {
        synchronized (this) {
          if (result == null) {
            result = CompletableFuture.supplyAsync(this::json, executor);
          }
        }
      }

      return resultUncheckedGet().length;
    }
  }

}
