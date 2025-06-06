/*
 * Copyright (C) 2022-2025 the original author or authors.
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

import java.util.function.Supplier;

class Lazy<T> implements Supplier<T> {
  private final Supplier<T> delegate;
  private volatile T value;

  Lazy(Supplier<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public T get() {
    if (value == null) {
      synchronized (this) {
        if (value == null) {
          value = delegate.get();
        }
      }
    }

    return value;
  }

  @Override
  public String toString() {
    return "Lazy{" +
        "value=" + (value != null ? value : "<not computed>") +
        '}';
  }
}
