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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.time.Clock;

class DiskExtendedHttpClientIntegrationTest implements ExtendedHttpClientContract {
  private final HttpClient delegate = HttpClient.newHttpClient();
  private File cacheDir;
  private ExtendedHttpClient client;
  private DiskCache cache;

  @BeforeEach
  void setUp() throws IOException {
    cacheDir = Files.createTempDirectory("diskcache").toFile();
    cacheDir.deleteOnExit();

    cache = new DiskCache(cacheDir.toPath());
    client = new ExtendedHttpClient(delegate, cache, Clock.systemUTC());
  }

  @AfterEach
  void tearDown() {
    cache.evictAll();

    for (File file : cacheDir.listFiles()) file.delete();
  }

  @Override
  public ExtendedHttpClient client() {
    return client;
  }

  @Override
  public Cache cache() {
    return cache;
  }

  @Override
  public ExtendedHttpClient client(Clock clock) {
    return new ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock);
  }
}
