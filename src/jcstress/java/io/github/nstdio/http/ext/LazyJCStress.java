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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class LazyJCStress {
  @JCStressTest
  @Outcome(id = "1, 1, 1", expect = ACCEPTABLE, desc = "All good.")
  @Outcome(expect = FORBIDDEN, desc = "Other cases are forbidden.")
  @State
  public static class Plain {
    private final CountingSupplier<Integer> delegate = new CountingSupplier<>(1);
    private final Lazy<Integer> lazy = new Lazy<>(delegate);

    @Actor
    public void reader1(III_Result r) {
      r.r1 = lazy.get();
      r.r3 = delegate.count.intValue();
    }

    @Actor
    public void reader2(III_Result r) {
      r.r2 = lazy.get();
      r.r3 = delegate.count.intValue();
    }
  }

  static class CountingSupplier<T> implements Supplier<T> {
    private final LongAdder count = new LongAdder();
    private final T value;

    CountingSupplier(T value) {
      this.value = value;
    }

    @Override
    public T get() {
      count.increment();

      return value;
    }
  }
}