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

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.LongAdder
import java.util.function.Supplier

internal class LazyTest {
  @Test
  fun `Should invoke delegate once`() {
    //given
    val delegate = CountingSupplier(1)
    val lazy = Lazy(delegate)

    //when
    runBlocking {
      coroutineScope {
        (0..5000).map {
          withContext(Dispatchers.Default) { lazy.get() }
        }
      }
    }

    //then
    delegate.count() shouldBe 1L
  }

  @Test
  fun `Should not resolve value when toString() called`() {
    //given
    val delegate = CountingSupplier(1)
    val lazy = Lazy(delegate)

    //when
    val actual = lazy.toString()

    //then
    actual.shouldBe("Lazy{value=<not computed>}")
    delegate.count() shouldBe 0L
  }

  @Test
  fun `Should have proper toString`() {
    //given
    val lazy = Lazy { 10 }

    //when
    lazy.get()
    val actual = lazy.toString()

    //then
    actual.shouldBe("Lazy{value=10}")
  }

  class CountingSupplier<T>(private val value: T) : Supplier<T> {
    private val count = LongAdder()

    override fun get(): T {
      count.increment()
      return value
    }

    fun count() = count.toLong()
  }
}