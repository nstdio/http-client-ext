/*
 * Copyright (C) 2023 Edgar Asatryan
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
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito.argThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.util.concurrent.Flow.Subscriber

@ExtendWith(MockitoExtension::class)
class PathReadingSubscriptionTest {
  @TempDir
  private lateinit var baseDir: Path

  @Mock
  lateinit var mockSub: Subscriber<List<ByteBuffer>>

  @Test
  fun `Should read content of file`() {
    //given
    val content = "abcdef"
    val file = baseDir.resolve("text")
    Files.write(file, content.toByteArray(), CREATE)

    val sub = PathReadingSubscription(mockSub, SimpleStreamFactory(), file)

    //when
    sub.request(1)
    sub.request(1)

    //then
    verify(mockSub).onNext(listOf(content.toByteBuffer()))
    verify(mockSub).onComplete()
    verifyNoMoreInteractions(mockSub)
  }

  @Test
  fun `Should report error if IO error occures`() {
    //given
    val sub = PathReadingSubscription(mockSub, SimpleStreamFactory(), baseDir.resolve("text"))

    //when
    sub.request(1)
    
    //then
    verify(mockSub).onError(argThat { it is IOException })
  }
  
  @Test
  fun `Should not invoke sub when canceled`() {
    //given
    val sub = PathReadingSubscription(mockSub, SimpleStreamFactory(), baseDir.resolve("text"))

    //when
    sub.cancel()
    sub.request(1)
    
    //then
    verifyNoInteractions(mockSub)
  }

  @Test
  fun `Should report error if negative requested`() {
    //given
    @Suppress("UNCHECKED_CAST")
    val mockSub = mock(Subscriber::class.java) as Subscriber<List<ByteBuffer>>
    val sub = PathReadingSubscription(mockSub, SimpleStreamFactory(), baseDir.resolve("text"))

    //when
    sub.request(-1)

    //then
    verify(mockSub).onError(argThat { it.message == "non-positive request" })
  }
}