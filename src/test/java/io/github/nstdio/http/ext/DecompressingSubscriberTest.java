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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.RepeatedTest;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;

class DecompressingSubscriberTest {
    static <T> ScatteringSubscription scatteringSubscription(BodySubscriber<T> sub, byte[] body) {
        return new ScatteringSubscription(sub, body);
    }

    @RepeatedTest(1)
    void expected1() {
        //given
        String bodyString = Helpers.randomString(8192, 40960);
        byte[] body = bodyString.getBytes(UTF_8);
        byte[] gzip = Compression.gzip(bodyString);

        var subscriber = new DecompressingSubscriber<>(HttpResponse.BodySubscribers.ofByteArray());

        //when
        var sub = scatteringSubscription(subscriber, gzip);
        subscriber.onSubscribe(sub);
        sub.cancel();
        subscriber.onComplete();

        byte[] actual = subscriber.getBody().toCompletableFuture().join();

        //then
        assertArrayEquals(body, actual);
    }
}