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

import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodySubscriber
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal class CachingBodySubscriberTest {
  @Test
    fun shouldWriteAndReadFromEmptyFile(@TempDir dir: Path) {
    //given
    val p1 = dir.resolve("p1")
    val p2 = dir.resolve("p2")
    Files.createFile(p2)
    Assertions.assertThat(p2).isEmptyFile
    assertShouldReadAndWriteResponse(p1, p2)
  }

  @Test
    fun shouldWriteAndReadFromNotEmptyFile(@TempDir dir: Path) {
    //given
    val p1 = dir.resolve("p1")
    val p2 = dir.resolve("p2")
    Files.write(p2, "random-stuff".repeat(300).toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    Assertions.assertThat(p2).isNotEmptyFile
    assertShouldReadAndWriteResponse(p1, p2)
  }

  private fun assertShouldReadAndWriteResponse(p1: Path, p2: Path) {
    //given
    val body = Arb.string(32, 128).next()
    val original = HttpResponse.BodySubscribers.ofFile(p1)
    val other = PathSubscriber(p2)
    val subscriber = CachingBodySubscriber(original, other) { }

    //when
    val buffers = Helpers.toBuffers(body)
    val subscription = PlainSubscription(subscriber, buffers, false)
    subscriber.onSubscribe(subscription)
    val actual1 = subscriber.body.toCompletableFuture().join()
    val actual2 = other.body.toCompletableFuture().join()

    //then
    Assertions.assertThat(actual1).isEqualTo(p1)
    Assertions.assertThat(actual2).isEqualTo(p2)
    Assertions.assertThat(actual1).hasContent(body)
    Assertions.assertThat(actual2).hasContent(body)
  }

  @Test
  fun shouldInvokeDelegatesInOrder() {
    @Suppress("UNCHECKED_CAST")
    val original: BodySubscriber<String> = Mockito.mock(BodySubscriber::class.java) as BodySubscriber<String>

    @Suppress("UNCHECKED_CAST")
    val other: BodySubscriber<String> = Mockito.mock(BodySubscriber::class.java) as BodySubscriber<String>
    val subscriber = CachingBodySubscriber(original, other) { }

    //when
    subscriber.onSubscribe(null)
    subscriber.onNext(listOf())
    subscriber.onError(null)
    subscriber.onComplete()

    //then
    val inOrder = Mockito.inOrder(original, other)
    inOrder.verify(other).onSubscribe(ArgumentMatchers.any())
    inOrder.verify(original).onSubscribe(ArgumentMatchers.any())
    inOrder.verify(other).onNext(ArgumentMatchers.any())
    inOrder.verify(original).onNext(ArgumentMatchers.any())
    inOrder.verify(other).onError(ArgumentMatchers.any())
    inOrder.verify(original).onError(ArgumentMatchers.any())
    inOrder.verify(other).onComplete()
    inOrder.verify(original).onComplete()
  }
}