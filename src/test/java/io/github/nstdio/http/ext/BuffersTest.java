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
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Stream;

class BuffersTest {

    static Stream<List<ByteBuffer>> listBuffersData() {
        return Stream.of(
                List.of(),
                List.of(ByteBuffer.wrap("abcde".repeat(16).getBytes(UTF_8))),
                List.of(ByteBuffer.wrap("ab".repeat(16).getBytes(UTF_8)), ByteBuffer.wrap("cd".repeat(16).getBytes(UTF_8))),
                Helpers.toBuffers(RandomUtils.nextBytes(96), true)
        );
    }

    @ParameterizedTest
    @MethodSource("listBuffersData")
    void shouldDuplicatedBufferList(List<ByteBuffer> buffers) {
        //when
        List<ByteBuffer> actual = Buffers.duplicate(buffers);

        //then
        assertThat(actual)
                .isNotSameAs(buffers)
                .hasSameSizeAs(buffers)
                .allMatch(Buffer::isReadOnly, "Expecting duplicated buffet to be read-only")
                .containsExactlyElementsOf(buffers);
    }

    @Test
    void shouldDuplicatedSingleBuffer() {
        //given
        ByteBuffer buffer = ByteBuffer.wrap(RandomUtils.nextBytes(16));

        //when
        ByteBuffer actual = Buffers.duplicate(buffer);
        buffer.get();

        //then
        assertThat(actual.position()).isZero();
    }
}