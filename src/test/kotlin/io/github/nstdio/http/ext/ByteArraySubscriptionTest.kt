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

import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.io.IOException
import java.net.http.HttpResponse.BodySubscriber
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import kotlin.time.Duration.Companion.seconds

@ExtendWith(MockitoExtension::class)
internal class ByteArraySubscriptionTest {
  @Mock
  lateinit var mockSubscriber: BodySubscriber<List<ByteArray>>

  @Test
  fun shouldInvokeOnCompleteAfterFirstRequest() {
    //given
    val bytes = Arb.string(10, 20).next().toByteArray(StandardCharsets.UTF_8)
    val subscription = ByteArraySubscription.ofByteBufferList(mockSubscriber, bytes)

    //when
    runAsyncAwait { subscription.request(1) }

    //then
    verify(mockSubscriber).onNext(ArgumentMatchers.anyList())
    verify(mockSubscriber).onComplete()
    verifyNoMoreInteractions(mockSubscriber)
  }

  @Test
  fun shouldReportErrorWhenRequestedIsNegative() {
    //given
    val bytes = Arb.string(10, 20).next().toByteArray(StandardCharsets.UTF_8)
    val subscription = ByteArraySubscription.ofByteBufferList(mockSubscriber, bytes)

    //when
    subscription.request(0)
    subscription.request(-1)

    //then
    verify(mockSubscriber).onError(
      any(
        IllegalArgumentException::class.java
      )
    )
    verifyNoMoreInteractions(mockSubscriber)
  }

  @Test
  fun shouldNotInvokeSubscriberWhenCanceled() {
    //given
    val subscription = ByteArraySubscription.ofByteBufferList(mockSubscriber, ByteArray(0))

    //when
    subscription.cancel()

    runAsyncAwait {
      subscription.request(1)
    }

    //then
    verifyNoInteractions(mockSubscriber)
  }

  @Test
  fun `Should invoke subscribers onError`() {
    //given
    val exc = IOException("hey!")
    val subscription = ByteArraySubscription(mockSubscriber, { it.run() }, { throw exc }, { listOf<ByteBuffer>() })

    //when
    subscription.request(1)

    //then
    verify(mockSubscriber).onError(exc)
    verifyNoMoreInteractions(mockSubscriber)
  }

  @Test
  fun `Should eventually submit data to subscriber`() {
    //given
    val executor = ForkJoinPool.commonPool()
    val bytes = Arb.byteArray(8).next()
    val item = listOf(bytes.toBuffer())
    val subscription = ByteArraySubscription(mockSubscriber, executor, { bytes }, { item })

    //when
    subscription.request(1)

    //then
    runBlocking {
      eventually(10.seconds) {
        val inOrder = inOrder(mockSubscriber)
        inOrder.verify(mockSubscriber).onNext(item)
        inOrder.verify(mockSubscriber).onComplete()
        inOrder.verifyNoMoreInteractions()

        true
      }
    }
  }

  @Test
  fun `Should cancel result`() {
    //given
    val mockExecutor = mock(ExecutorService::class.java)

    val bytes = Arb.byteArray(8).next()
    val item = listOf(bytes.toBuffer())
    val subscription = ByteArraySubscription(mockSubscriber, mockExecutor, { bytes }, { item })

    //when
    subscription.request(1)
    subscription.cancel()

    //then
    subscription.result.isCancelled shouldBe true
  }

  private fun runAsyncAwait(block: suspend CoroutineScope.() -> Unit) {
    runBlocking {
      coroutineScope {
        (0..64).map {
          async(block = block)
        }.awaitAll()
      }
    }
  }
}
