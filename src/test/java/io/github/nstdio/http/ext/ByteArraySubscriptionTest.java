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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse.BodySubscriber;

class ByteArraySubscriptionTest {

    @Test
    void shouldInvokeOnCompleteAfterFirstRequest() {
        //given
        byte[] bytes = Helpers.randomString(10, 20).getBytes(UTF_8);

        @SuppressWarnings("unchecked")
        var mockSubscriber = (BodySubscriber<byte[]>) mock(BodySubscriber.class);
        var subscription = new ByteArraySubscription(mockSubscriber, bytes);

        //when
        for (int i = 0; i < 64; i++) {
            subscription.request(1);
        }

        //then
        verify(mockSubscriber).onNext(anyList());
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    void shouldReportErrorWhenRequestedIsNegative() {
        //given
        byte[] bytes = Helpers.randomString(10, 20).getBytes(UTF_8);

        @SuppressWarnings("unchecked")
        var mockSubscriber = (BodySubscriber<byte[]>) mock(BodySubscriber.class);
        var subscription = new ByteArraySubscription(mockSubscriber, bytes);

        //when
        subscription.request(0);
        subscription.request(-1);

        //then
        verify(mockSubscriber, times(2)).onError(any(IllegalArgumentException.class));
    }

    @Test
    void shouldNotInvokeSubscriberWhenCanceled() {
        //given
        @SuppressWarnings("unchecked")
        var mockSubscriber = (BodySubscriber<byte[]>) mock(BodySubscriber.class);
        var subscription = new ByteArraySubscription(mockSubscriber, new byte[0]);

        //when
        subscription.cancel();
        for (int i = 0; i < 64; i++) {
            subscription.request(1);
        }

        //then
        verifyNoInteractions(mockSubscriber);
    }
}