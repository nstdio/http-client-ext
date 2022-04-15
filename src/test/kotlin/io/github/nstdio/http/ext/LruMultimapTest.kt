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

import io.github.nstdio.http.ext.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.function.Consumer
import java.util.function.ToIntFunction

class LruMultimapTest {
  private val addFn = ToIntFunction { _: List<String?>? -> -1 }
  private val throwingFn = ToIntFunction { _: List<String?>? ->
    throw IllegalStateException(
      "Should not be invoked!"
    )
  }

  @Test
  fun shouldReplaceAndNotify() {
    //given
    val mockEvictionListener: Consumer<String?> = mockConsumer()
    val map = LruMultimap<String, String>(512, mockEvictionListener)

    //when
    @Suppress("UNUSED_VARIABLE")
    val putResult1 = map.putSingle("a", "1") { 0 }
    val putResult2 = map.putSingle("a", "2") { 0 }
    val getResult = map.getSingle("a") { 0 }

    //then
    assertThat(map)
      .hasMapSize(1)
      .hasSize(1)
      .hasOnlyValue("a", "2", 0)
    assertThat(putResult2).containsOnly("2")
    assertThat(getResult).isEqualTo("2")
    verify(mockEvictionListener).accept("1")
    verifyNoMoreInteractions(mockEvictionListener)
  }

  @Test
  fun shouldMaintainLruForLists() {
    //given
    val map = LruMultimap<String, String?>(512, null)

    //when + then
    map.putSingle("a", "1", addFn)
    val putResult = map.putSingle("a", "2", addFn)
    map.getSingle("a") { 0 }
    assertThat(putResult).containsExactly("2", "1")
    map.getSingle("a") { 1 }
    assertThat(putResult).containsExactly("1", "2")
    assertThat(map).hasMapSize(1).hasSize(2)
  }

  @Test
  fun shouldNotEvictEldestWhenEmpty() {
    //given
    val map = LruMultimap<String, String>(512, null)

    //when + then
    for (i in 0..31) {
      assertFalse(map.evictEldest())
    }
  }

  @Test
  fun shouldEvictEldest() {
    //given
    val mockEvictionListener: Consumer<String?> = mockConsumer()
    val map = LruMultimap<String, String?>(512, mockEvictionListener)

    //when
    map.putSingle("a", "1", addFn)
    map.putSingle("a", "2", addFn)
    map.putSingle("b", "1", addFn)
    map.putSingle("b", "2", addFn)
    val evictionResult = map.evictEldest()

    //then
    assertTrue(evictionResult)
    assertThat(map).hasMapSize(2).hasSize(3)
      .hasOnlyValue("a", "2", 0)
    verify(mockEvictionListener).accept("1")
    verifyNoMoreInteractions(mockEvictionListener)
  }

  @Test
  fun shouldRemoveMapEntryWhenLastRemoved() {
    //given
    val mockEvictionListener: Consumer<String?> = mockConsumer()
    val map = LruMultimap<String, String?>(512, null)
    map.addEvictionListener(mockEvictionListener)

    //when
    map.putSingle("a", "1", addFn)
    map.putSingle("b", "1", addFn)
    map.putSingle("b", "2", addFn)
    val evictionResult = map.evictEldest()

    //then
    assertTrue(evictionResult)
    assertThat(map).hasMapSize(1).hasSize(2)
    verify(mockEvictionListener).accept("1")
    verifyNoMoreInteractions(mockEvictionListener)
  }

  @Test
  fun shouldRespectMaxSize() {
    //given
    val mockEvictionListener: Consumer<String?> = mockConsumer()
    val map = LruMultimap<String, String?>(2, mockEvictionListener)

    //when
    map.putSingle("b", "2", addFn)
    map.putSingle("b", "1", addFn)
    map.putSingle("a", "1", addFn)

    //then
    assertThat(map)
      .hasMapSize(2)
      .hasSize(2)
      .hasOnlyValue("a", "1", 0)
      .hasOnlyValue("b", "1", 0)
    verify(mockEvictionListener).accept("2")
    verifyNoMoreInteractions(mockEvictionListener)
  }

  @Test
  fun shouldClearAll() {
    //given
    val mockEl: Consumer<String?> = mockConsumer()
    val map = LruMultimap<String, String?>(23, mockEl)

    //when
    map.putSingle("b", "1", addFn)
    map.putSingle("b", "2", addFn)
    map.putSingle("b", "3", addFn)
    map.putSingle("a", "4", addFn)
    map.clear()

    //then
    assertThat(map).hasMapSize(0).hasSize(0)
    val inOrder = Mockito.inOrder(mockEl)
    inOrder.verify(mockEl).accept("1")
    inOrder.verify(mockEl).accept("2")
    inOrder.verify(mockEl).accept("3")
    inOrder.verify(mockEl).accept("4")
    inOrder.verifyNoMoreInteractions()
  }

  @Test
  fun shouldClearWhenEmpty() {
    //given
    val mockEl: Consumer<String?> = mockConsumer()
    val map = LruMultimap<String, String>(23, mockEl)

    //when
    map.clear()

    //then
    assertThat(map).isEmpty
    Mockito.verifyNoInteractions(mockEl)
  }

  @Test
  fun shouldNotGetWhenIndexIncorrect() {
    val map = LruMultimap<String, String?>(23, null)

    //when
    map.putSingle("a", "2", addFn)
    map.putSingle("a", "1", addFn)

    //then
    assertThat(map.getSingle("b", throwingFn)).isNull()
    assertThat(map.getSingle("a") { 5 }).isNull()
    assertThat(map.getSingle("a") { -1 }).isNull()
  }

  @Test
  fun shouldEvictAllForExistingKey() {
    //given
    val mockEl: Consumer<String?> = mockConsumer()
    val map = LruMultimap<String, String?>(23, mockEl)

    //when
    map.putSingle("a", "1", addFn)
    map.putSingle("a", "2", addFn)
    map.evictAll("a")

    //then
    assertThat(map).isEmpty
    val inOrder = Mockito.inOrder(mockEl)
    inOrder.verify(mockEl).accept("1")
    inOrder.verify(mockEl).accept("2")
    inOrder.verifyNoMoreInteractions()
  }

  @Suppress("UNCHECKED_CAST")
  private fun mockConsumer() = Mockito.mock(Consumer::class.java) as Consumer<String?>

  @Test
  fun shouldEvictAllForNonExistingKey() {
    //given
    val mockEl: Consumer<String?> = mockConsumer()
    val map = LruMultimap<String, String?>(23, mockEl)

    //when
    map.putSingle("a", "1", addFn)
    map.putSingle("a", "2", addFn)
    map.evictAll("b")

    //then
    assertThat(map).hasMapSize(1).hasSize(2)
    Mockito.verifyNoInteractions(mockEl)
  }
}