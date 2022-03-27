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

import java.util.concurrent.atomic.LongAdder;

class DefaultCacheStats implements TrackableCacheStats {
  private final LongAdder hit = new LongAdder();
  private final LongAdder miss = new LongAdder();

  @Override
  public long hit() {
    return hit.longValue();
  }

  @Override
  public long miss() {
    return miss.longValue();
  }

  @Override
  public void trackHit() {
    hit.increment();
  }

  @Override
  public void trackMiss() {
    miss.increment();
  }
}
