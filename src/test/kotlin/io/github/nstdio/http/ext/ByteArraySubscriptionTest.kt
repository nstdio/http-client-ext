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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.net.http.HttpResponse.BodySubscriber
import java.nio.charset.StandardCharsets

@ExtendWith(MockitoExtension::class)
internal class ByteArraySubscriptionTest {
  @Mock
  lateinit var mockSubscriber: BodySubscriber<ByteArray>

  @Test
  fun shouldInvokeOnCompleteAfterFirstRequest() {
    //given
    val bytes = Helpers.randomString(10, 20).toByteArray(StandardCharsets.UTF_8)
    val subscription = ByteArraySubscription(mockSubscriber, bytes)

    //when
    for (i in 0..63) {
      subscription.request(1)
    }

    //then
    verify(mockSubscriber).onNext(ArgumentMatchers.anyList())
    verify(mockSubscriber).onComplete()
    verifyNoMoreInteractions(mockSubscriber)
  }

  @Test
  fun shouldReportErrorWhenRequestedIsNegative() {
    //given
    val bytes = Helpers.randomString(10, 20).toByteArray(StandardCharsets.UTF_8)
    val subscription = ByteArraySubscription(mockSubscriber, bytes)

    //when
    subscription.request(0)
    subscription.request(-1)

    //then
    verify(mockSubscriber, times(2)).onError(
      ArgumentMatchers.any(
        IllegalArgumentException::class.java
      )
    )
  }

  @Test
  fun shouldNotInvokeSubscriberWhenCanceled() {
    //given
    val subscription = ByteArraySubscription(mockSubscriber, ByteArray(0))

    //when
    subscription.cancel()
    for (i in 0..63) {
      subscription.request(1)
    }

    //then
    verifyNoInteractions(mockSubscriber)
  }
}