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
package io.github.nstdio.http.ext

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.net.http.HttpRequest
import java.time.Clock

@ExtendWith(MockitoExtension::class)
internal class FilteringCacheTest {
  @Mock
  private lateinit var mockDelegate: Cache

  @Mock
  private lateinit var mockRequest: HttpRequest

  @Mock
  private lateinit var mockEntry: Cache.CacheEntry
  private lateinit var metadata: CacheEntryMetadata

  @BeforeEach
  fun setup() {
    metadata = CacheEntryMetadata.of(0, 0, Helpers.responseInfo(mapOf()), mockRequest, Clock.systemUTC())
  }

  @Test
  fun `Should call delegate`() {
    //given
    val cache = FilteringCache(mockDelegate, { true }, { true })

    //when
    cache.put(mockRequest, mockEntry)
    cache.evict(mockRequest)
    cache.evictAll(mockRequest)
    cache.evictAll()
    cache.get(mockRequest)
    cache.stats()
    cache.writer<String>(metadata)
    cache.close()

    //then
    verify(mockDelegate).put(mockRequest, mockEntry)
    verify(mockDelegate).evict(mockRequest)
    verify(mockDelegate).evictAll(mockRequest)
    verify(mockDelegate).evictAll()
    verify(mockDelegate).get(mockRequest)
    verify(mockDelegate).stats()
    verify(mockDelegate).writer<String>(metadata)
    verify(mockDelegate).close()
  }

  @Test
  fun `Should not call delegate when request does not match`() {
    //given
    val cache = FilteringCache(mockDelegate, { false }, { true })

    //when
    cache.put(mockRequest, mockEntry)
    cache.evict(mockRequest)
    cache.evictAll(mockRequest)
    cache.get(mockRequest)
    cache.writer<String>(metadata)

    //then
    verifyNoInteractions(mockDelegate)
  }

  @Test
  fun `Should not call delegate when response does not match`() {
    //given
    val cache = FilteringCache(mockDelegate, { true }, { false })

    //when
    cache.writer<String>(metadata)

    //then
    verifyNoInteractions(mockDelegate)
  }
}