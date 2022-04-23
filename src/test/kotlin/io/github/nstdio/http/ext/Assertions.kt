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
package io.github.nstdio.http.ext

import io.github.nstdio.http.ext.Properties.duration
import io.github.nstdio.http.ext.Responses.DelegatingHttpResponse
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.ThrowingConsumer
import org.awaitility.Awaitility
import org.awaitility.core.ConditionFactory
import org.awaitility.core.ThrowingRunnable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import java.net.URI
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object Assertions {
  val POLL_INTERVAL = duration("client.test.pool.interval")
    .orElseGet { Duration.ofMillis(5) }

  /**
   * The maximum time to wait until cache gets written.
   */
  val CACHE_WRITE_DELAY = duration("client.test.cache.write.delay")
    .orElseGet { Duration.ofMillis(500) }

  fun <T> assertThat(r: HttpResponse<T>): HttpResponseAssertion<T> {
    return HttpResponseAssertion(r)
  }

  fun assertThat(h: HttpHeaders): HttpHeadersAssertion {
    return HttpHeadersAssertion(h)
  }

  fun assertThat(c: Cache): CacheAssertion {
    return CacheAssertion(c)
  }

  internal fun <K, V> assertThat(m: LruMultimap<K, V>): LruMultimapAssertion<K, V> {
    return LruMultimapAssertion(m)
  }

  @JvmStatic
  fun awaitFor(r: ThrowingRunnable?) {
    await().untilAsserted(r)
  }

  @JvmStatic
  fun await(): ConditionFactory {
    return Awaitility.await().pollInterval(POLL_INTERVAL).atMost(CACHE_WRITE_DELAY)
  }

  internal class LruMultimapAssertion<K, V>(m: LruMultimap<K, V>?) : ObjectAssert<LruMultimap<K, V>?>(m) {
    fun hasSize(size: Int): LruMultimapAssertion<K, V> {
      assertThat(actual!!.size()).isEqualTo(size)
      return this
    }

    fun hasMapSize(size: Int): LruMultimapAssertion<K, V> {
      assertThat(actual!!.mapSize()).isEqualTo(size)
      return this
    }

    fun hasOnlyValue(k: K, v: V, index: Int): LruMultimapAssertion<K, V> {
      assertThat(actual!!.getSingle(k) { index })
        .isEqualTo(v)
      return this
    }

    val isEmpty: LruMultimapAssertion<K, V>
      get() = hasSize(0).hasMapSize(0)
  }

  class HttpHeadersAssertion internal constructor(headers: HttpHeaders?) : ObjectAssert<HttpHeaders?>(headers) {
    fun hasHeaderWithValues(header: String?, vararg values: String?): HttpHeadersAssertion {
      assertThat(actual!!.allValues(header))
        .containsExactlyInAnyOrder(*values)
      return this
    }

    fun isEmpty(): HttpHeadersAssertion {
      assertThat(actual!!.map()).isEmpty()
      return this
    }

    fun hasHeaderWithOnlyValue(header: String?, value: String?): HttpHeadersAssertion {
      assertThat(actual!!.allValues(header))
        .containsExactly(value)
      return this
    }

    fun hasNoHeader(header: String?): HttpHeadersAssertion {
      assertThat(actual!!.allValues(header))
        .isEmpty()
      return this
    }

    fun isEqualTo(other: HttpHeaders?): HttpHeadersAssertion {
      assertThat(actual).isEqualTo(other)
      return this
    }
  }

  class HttpRequestAssertion(httpRequest: HttpRequest?) : ObjectAssert<HttpRequest?>(httpRequest) {
    fun isEqualTo(other: HttpRequest?): HttpRequestAssertion {
      assertThat(actual).isEqualTo(other)
      return this
    }
  }

  class HttpResponseAssertion<T> internal constructor(tHttpResponse: HttpResponse<T>) :
    ObjectAssert<HttpResponse<T>>(tHttpResponse) {
    fun hasStatusCode(statusCode: Int): HttpResponseAssertion<T> {
      assertThat(actual!!.statusCode())
        .withFailMessage {
          String.format(
            "Expecting %s to have status code %d, but has %d",
            actual,
            statusCode,
            actual.statusCode()
          )
        }
        .isEqualTo(statusCode)
      return this
    }

    val isNetwork: HttpResponseAssertion<T>
      get() {
        assertThat(actual)
          .satisfiesAnyOf(
            ThrowingConsumer { response: HttpResponse<T>? -> assertJdk(response) },
            ThrowingConsumer { response: HttpResponse<T>? ->
              assertInstanceOf(DelegatingHttpResponse::class.java, response)
              assertJdk((response as DelegatingHttpResponse<T>).delegate())
            }
          )
        return this
      }

    private fun assertJdk(response: HttpResponse<T>?) {
      assertEquals("jdk.internal.net.http", response!!.javaClass.packageName)
    }

    val isNotNetwork: HttpResponseAssertion<T>
      get() {
        assertThat(actual!!.javaClass.packageName)
          .withFailMessage("The response is network!")
          .isNotEqualTo("jdk.internal.net.http")
        return this
      }
    val isNotCached: HttpResponseAssertion<T>
      get() {
        assertThat(actual!!.javaClass.canonicalName)
          .withFailMessage("The response is cached!")
          .isNotEqualTo("io.github.nstdio.http.ext.CachedHttpResponse")
        return this
      }
    val isCached: HttpResponseAssertion<T>
      get() {
        Assertions.assertThat(actual)
          .satisfiesAnyOf(
            ThrowingConsumer { response: HttpResponse<T>? ->
              assertInstanceOf(
                CachedHttpResponse::class.java, response
              )
            },
            ThrowingConsumer { r: HttpResponse<T>? ->
              assertInstanceOf(DelegatingHttpResponse::class.java, r)
              val delegate = (r as DelegatingHttpResponse<T>).delegate()
              assertInstanceOf(CachedHttpResponse::class.java, delegate)
            }
          )
        return this
      }

    fun hasBody(body: T): HttpResponseAssertion<T> {
      assertThat(actual!!.body()).isEqualTo(body)
      return this
    }
    
    fun hasURI(uri: URI): HttpResponseAssertion<T> {
      assertThat(actual!!.uri()).isEqualTo(uri)
      return this
    }

    fun isSemanticallyEqualTo(other: HttpResponse<T>): HttpResponseAssertion<T> {
      SoftAssertions.assertSoftly { softly: SoftAssertions ->
        softly.assertThat(actual!!.statusCode())
          .describedAs("status code doesn't equal")
          .isEqualTo(other.statusCode())
        softly.assertThat(actual.headers())
          .describedAs("headers doesn't equal")
          .isEqualTo(other.headers())
        softly.assertThat(actual.body())
          .describedAs("body doesn't equal")
          .isEqualTo(other.body())
        softly.assertThat(actual.uri())
          .describedAs("uri doesn't equal")
          .isEqualTo(other.uri())
        softly.assertThat(actual.version())
          .describedAs("version doesn't equal")
          .isEqualTo(other.version())
      }
      return this
    }

    fun hasHeader(header: String?, value: String?): HttpResponseAssertion<T> {
      assertThat(actual!!.headers())
        .hasHeaderWithValues(header, value)
      return this
    }

    fun hasNoHeader(headers: String?): HttpResponseAssertion<T> {
      assertThat(actual!!.headers())
        .hasNoHeader(headers)
      return this
    }

    fun hasRequest(request: HttpRequest): HttpResponseAssertion<T> {
      assertThat(actual!!.request())
        .isEqualTo(request)
      return this
    }
  }

  class CacheAssertion(cache: Cache?) : ObjectAssert<Cache?>(cache) {
    fun hasHits(expectedHit: Long): CacheAssertion {
      assertEquals(
        expectedHit,
        stats().hit(),
        "cache does not have expected hit count"
      )
      return this
    }

    private fun stats(): Cache.CacheStats {
      return actual!!.stats()
    }

    fun hasNoHits(): CacheAssertion {
      assertEquals(
        0,
        stats().hit(),
        "cache does expected to have hits, but there is some"
      )
      return this
    }

    fun hasMiss(expectedMiss: Long): CacheAssertion {
      assertEquals(
        expectedMiss,
        stats().miss(),
        "cache does not have expected miss count"
      )
      return this
    }

    fun hasAtLeastMiss(expectedMiss: Long): CacheAssertion {
      assertThat(stats().miss())
        .isGreaterThanOrEqualTo(expectedMiss)
      return this
    }
  }
}