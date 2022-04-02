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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(MILLISECONDS)
@Fork(value = 1, warmups = 1)
@Warmup(time = 3, iterations = 2)
@Measurement(iterations = 4, time = 5)
public class HeadersBenchmark {
  private static final String LONG_HEADER_VALUE = "no-cache, no-store, must-revalidate, no-transform, immutable, only-if-cached, max-age=150, max-stale=150, min-fresh=150, stale-if-error=200, stale-while-revalidate=900";

  @Benchmark
  public List<String> splitCommaList() {
    return Headers.splitComma(LONG_HEADER_VALUE);
  }
}
