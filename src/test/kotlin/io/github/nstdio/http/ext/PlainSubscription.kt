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

import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit

internal class PlainSubscription @JvmOverloads constructor(
  private val subscriber: Flow.Subscriber<List<ByteBuffer>>,
  private val buffers: MutableList<ByteBuffer>,
  async: Boolean = true
) : Flow.Subscription {
  private val executor: Optional<ExecutorService>
  private val it: Iterator<ByteBuffer>
  private var completed = false

  init {
    it = buffers.iterator()
    executor = if (async) Optional.of(Executors.newSingleThreadExecutor()) else Optional.empty()
  }

  override fun request(n: Long) {
    if (completed) return
    request0(n)
  }

  private fun request0(r: Long) {
    var n = r
    while (n != 0L && it.hasNext()) {
      val next = listOf(it.next())
      execute { subscriber.onNext(next) }
      n--
    }
    if (!it.hasNext()) {
      completed = true
      clean()
      subscriber.onComplete()
    }
  }

  private fun execute(cmd: Runnable) {
    executor.ifPresentOrElse({ service: ExecutorService -> service.execute(cmd) }, cmd)
  }

  override fun cancel() {
    if (completed) {
      return
    }
    completed = true
    request0(Int.MAX_VALUE.toLong())
    clean()
  }

  private fun clean() {
    buffers.clear()
    executor.ifPresent { service: ExecutorService ->
      service.shutdown()
      try {
        service.awaitTermination(5, TimeUnit.SECONDS)
      } catch (e: InterruptedException) {
        // noop
      }
    }
  }
}