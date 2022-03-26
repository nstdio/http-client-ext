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

import static io.github.nstdio.http.ext.Helpers.toBuffers;
import static org.apache.commons.lang3.RandomUtils.nextBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class ByteBufferInputStreamTest {

    static Stream<Named<byte[]>> fullReadingData() {
        return IntStream.of(100, 8192, 16384, 65536, 131072)
                .mapToObj(n -> Named.named("Size: " + n, nextBytes(n)));
    }

    @ParameterizedTest
    @MethodSource("fullReadingData")
    void fullReading(byte[] bytes) throws IOException {
        //given
        var is = new ByteBufferInputStream();
        toBuffers(bytes, false).forEach(is::add);

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
        toBuffers(bytes, false).forEach(is::add);

        //when
        var actual = is.readAllBytes();

        //then
        assertArrayEquals(bytes, actual);
    }

    @Test
    void shouldReturnNegativeWhenInputIsEmpty() throws IOException {
        //given
        var is = new ByteBufferInputStream();
        byte[] bytes = nextBytes(8);
        is.add(ByteBuffer.wrap(bytes));

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
        toBuffers(randomString.getBytes(), false).forEach(is::add);
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
        byte[] bytes = nextBytes(32);
        var is = new ByteBufferInputStream();
        toBuffers(bytes, false)
                .stream()
                .map(buffer -> buffer.position(buffer.limit()))
                .forEach(is::add);

        is.add(ByteBuffer.wrap(new byte[0]));

        //when
        byte[] actual = is.readAllBytes();

        //then
        assertArrayEquals(bytes, actual);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void shouldThrowWhenClosed() {
        //given
        var is = new ByteBufferInputStream();

        //when
        is.close();

        //then
        assertThatIOException().isThrownBy(is::read);
        assertThatIOException().isThrownBy(() -> is.read(new byte[0]));
        assertThatIOException().isThrownBy(() -> is.read(new byte[5], 0, 5));
        assertThatIOException().isThrownBy(is::readAllBytes);
        assertThatIOException().isThrownBy(is::available);
    }

    @Test
    void shouldReportAvailable() throws Exception {
        //given
        byte[] bytes = nextBytes(32);
        var is = new ByteBufferInputStream();
        toBuffers(bytes, false).forEach(is::add);

        //when
        int actual = is.available();

        //then
        assertEquals(bytes.length, actual);
    }

    @Test
    void shouldReadAllBytes() throws Exception {
        //given
        byte[] bytes = nextBytes(32);
        var is = new ByteBufferInputStream();
        toBuffers(bytes, false).forEach(is::add);

        //when
        byte[] actual = is.readAllBytes();

        //then
        assertArrayEquals(bytes, actual);
    }

    @Test
    void shouldThrowWhenRequestedBytesNegative() {
        //given
        var is = new ByteBufferInputStream();

        //when + then
        assertThrows(IllegalArgumentException.class, () -> is.readNBytes(-1));
    }

    @Test
    void shouldReadUpToNBytes() throws IOException {
        //given
        var count = 16;
        byte[] bytes = nextBytes(count);
        var is = new ByteBufferInputStream();
        toBuffers(bytes, false).forEach(is::add);

        //when
        byte[] actual = is.readNBytes(count + 1);

        //then
        assertArrayEquals(bytes, actual);
    }

    @Test
    void shouldSupportMark() {
        //given + when + then
        assertTrue(new ByteBufferInputStream().markSupported());
    }

    @Test
    void shouldDumpBuffersToList() throws IOException {
        //given
        var is = new ByteBufferInputStream();
        List<ByteBuffer> buffers = toBuffers(nextBytes(96), false);
        buffers.forEach(is::add);

        //when
        List<ByteBuffer> actual = is.drainToList();

        //then
        assertEquals(-1, is.read());
        assertThat(actual)
                .hasSameSizeAs(buffers)
                .containsExactlyElementsOf(buffers);
    }
}