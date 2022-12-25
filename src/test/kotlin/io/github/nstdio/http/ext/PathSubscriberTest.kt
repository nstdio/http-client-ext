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

import io.kotest.matchers.future.shouldBeCompletedExceptionally
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.nio.file.Path
import java.util.concurrent.Flow

internal class PathSubscriberTest {
  @Test
  fun shouldBeCompletedWhenOnError() {
    //given
    val subscriber = PathSubscriber(Path.of("abc"))
    val th = IOException()

    //when
    subscriber.onError(th)
    val body = subscriber.body.toCompletableFuture()

    //then
    body.shouldBeCompletedExceptionally()
  }

  @Test
  fun shouldCompleteExceptionallyWhenPathDoesNotExist() {
    //given
    val subscriber = PathSubscriber(Path.of("abc"))

    //when
    subscriber.onSubscribe(PlainSubscription(subscriber, mutableListOf(), false))
    val body = subscriber.body.toCompletableFuture()

    //then
    body.shouldBeCompletedExceptionally()
  }

  @Test
  fun `Should complete exceptional when next throws`() {
    //given
    val mockStreamFactory = mock(StreamFactory::class.java)
    val mockSub = mock(Flow.Subscription::class.java)
    val mockChannel = mock(WritableByteChannel::class.java)
    val subscriber = PathSubscriber(mockStreamFactory, Path.of("abc"))

    given(mockStreamFactory.writable(any(), any()))
      .willReturn(mockChannel)
    given(mockChannel.write(any())).willThrow(IOException())

    //when
    subscriber.onSubscribe(mockSub)
    subscriber.onNext(listOf(ByteBuffer.allocate(1)))

    val body = subscriber.body.toCompletableFuture()

    //then
    body.shouldBeCompletedExceptionally()
    verify(mockChannel).close()
  }
}