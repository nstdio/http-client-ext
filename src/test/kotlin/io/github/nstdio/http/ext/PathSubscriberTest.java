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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;

class PathSubscriberTest {

  @Test
  void shouldBeCompletedWhenOnError() {
    //given
    PathSubscriber subscriber = new PathSubscriber(Path.of("abc"));
    IOException th = new IOException();

    //when
    subscriber.onError(th);

    //then
    assertThat(subscriber.getBody())
        .isCompletedExceptionally();
  }

  @Test
  void shouldCompleteExceptionallyWhenPathDoesNotExist() {
    //given
    PathSubscriber subscriber = new PathSubscriber(Path.of("abc"));

    //when
    subscriber.onSubscribe(new PlainSubscription(subscriber, List.of(), false));
    CompletionStage<Path> body = subscriber.getBody();

    //then
    assertThat(body)
        .isCompletedExceptionally();
  }
}