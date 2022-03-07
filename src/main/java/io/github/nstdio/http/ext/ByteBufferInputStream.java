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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

class ByteBufferInputStream extends InputStream {
    private final Deque<ByteBuffer> buffers = new ArrayDeque<>();
    private boolean closed;

    @Override
    public int read() throws IOException {
        ensureOpen();
        Deque<ByteBuffer> buffs = buffers;
        ByteBuffer buf = buffs.peek();
        int r;

        if (buf == null) {
            r = -1;
        } else if (!buf.hasRemaining()) {
            // current buffer fully consumed, so remove it
            buffs.poll();
            r = (buf = buffs.peek()) == null ? -1 : buf.get();
        } else {
            r = buf.get();
        }

        return r != -1 ? r & 0xff : -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        Objects.checkFromIndexSize(off, len, b.length);

        if (len == 0) {
            return 0;
        }

        Deque<ByteBuffer> buffs = buffers;
        ByteBuffer buf = buffs.peek();
        if (buf == null) {
            return -1;
        }
        int end = off + len;
        int i = off;
        for (; i < end; i++) {
            if (!buf.hasRemaining()) {
                while (buf != null && !buf.hasRemaining()) {
                    buffs.poll();
                    buf = buffs.peek();
                }

                if (buf == null) {
                    break;
                }
            }

            b[i] = buf.get();
        }

        return i - off;
    }

    @Override
    public void close() {
        buffers.clear();
        closed = true;
    }

    private void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("closed");
    }

    void addBuffer(ByteBuffer b) {
        if (!closed) {
            b = b.duplicate().asReadOnlyBuffer();
            if (!b.hasRemaining()) {
                b.flip();
            }

            if (b.hasRemaining()) {
                buffers.offer(b);
            }
        }
    }
}
