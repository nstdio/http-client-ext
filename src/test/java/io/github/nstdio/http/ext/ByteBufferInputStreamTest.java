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

import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class ByteBufferInputStreamTest {
    static List<ByteBuffer> toBuffers(byte[] bytes, boolean checkResult) {
        List<ByteBuffer> ret = new ArrayList<>();
        int start = 0;
        int stop = bytes.length + 1;
        int end = Math.max(1, RandomUtils.nextInt(start, stop));

        while (end != stop) {
            byte[] copy = Arrays.copyOfRange(bytes, start, end);

            ret.add(ByteBuffer.wrap(copy));

            start = end;
            end = RandomUtils.nextInt(start + 1, stop);
        }

        if (checkResult) {
            int i = 0;
            for (ByteBuffer b : ret) {
                while (b.hasRemaining()) {
                    assertEquals(bytes[i++], b.get());
                }
            }
        }

        return ret;
    }

    private static byte[] nextPositiveBytes(int n) {
        var bytes = RandomUtils.nextBytes(n);

        for (int i = 0; i < n; i++) {
            if (bytes[i] < 0) {
                bytes[i] *= -1;
            }
        }

        return bytes;
    }

    static Stream<Named<byte[]>> fullReadingData() {
        return IntStream.of(100, 8192, 16384, 65536, 131072)
                .mapToObj(n -> Named.named("Size: " + n, nextPositiveBytes(n)));
    }

    @ParameterizedTest
    @MethodSource("fullReadingData")
    void fullReading(byte[] bytes) throws IOException {
        //given
        var is = new ByteBufferInputStream();
        toBuffers(bytes, false).forEach(is::addBuffer);

        //when
        var actual = IOUtils.toByteArray(is);

        //then
        assertArrayEquals(bytes, actual);
    }

    @ParameterizedTest
    @MethodSource("fullReadingData")
    void shouldReadAllBytes(byte[] bytes) throws IOException {
        //given
        var is = new ByteBufferInputStream();
        toBuffers(bytes, false).forEach(is::addBuffer);

        //when
        var actual = is.readAllBytes();

        //then
        assertArrayEquals(bytes, actual);
    }

    @Test
    void shouldReturnNegativeWhenInputIsEmpty() throws IOException {
        //given
        var is = new ByteBufferInputStream();
        byte[] bytes = nextPositiveBytes(8);
        is.addBuffer(ByteBuffer.wrap(bytes));

        //when
        int actual = is.read(new byte[0]);

        //then
        assertEquals(0, actual);
    }

    @RepeatedTest(4)
    void shouldReadSingleProperly() throws IOException {
        //given
        var is = new ByteBufferInputStream();
        String randomString = RandomStringUtils.randomAlphabetic(64);
        toBuffers(randomString.getBytes(), false).forEach(is::addBuffer);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //when
        int read;
        while ((read = is.read()) != -1) {
            out.write(read);
        }

        //then
        assertEquals(-1, is.read());
        assertEquals(randomString, out.toString());
    }

    @Test
    void shouldFlipBuffer() throws IOException {
        //given
        byte[] bytes = nextPositiveBytes(32);
        var is = new ByteBufferInputStream();
        toBuffers(bytes, false)
                .stream()
                .map(buffer -> buffer.position(buffer.limit()))
                .forEach(is::addBuffer);

        is.addBuffer(ByteBuffer.wrap(new byte[0]));

        //when
        byte[] actual = is.readAllBytes();

        //then
        assertArrayEquals(bytes, actual);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void shouldThrowWhenClosed() {
        var is = new ByteBufferInputStream();
        is.close();

        assertThatIOException().isThrownBy(is::read);
        assertThatIOException().isThrownBy(() -> is.read(new byte[0]));
        assertThatIOException().isThrownBy(() -> is.read(new byte[5], 0, 5));
        assertThatIOException().isThrownBy(is::readAllBytes);
    }
}