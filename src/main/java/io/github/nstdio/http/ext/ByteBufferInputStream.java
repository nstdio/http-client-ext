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
    private ByteBuffer mark;
    private boolean closed;

    @Override
    public int read() throws IOException {
        ensureOpen();
        ByteBuffer buf = nextBuffer();

        if (buf == null) {
            return -1;
        }

        int r = buf.get() & 0xff;
        mark0(r);
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        Objects.checkFromIndexSize(off, len, b.length);

        if (len == 0) {
            return 0;
        }

        ByteBuffer buf = nextBuffer();
        if (buf == null) {
            return -1;
        }
        int end = off + len;
        int i = off;
        for (; i < end; i++) {
            if (!buf.hasRemaining()) {
                buf = nextBuffer();
                if (buf == null) break;
            }

            b[i] = buf.get();
        }

        int read = i - off;

        mark0(b, off, read);
        return read;
    }

    private ByteBuffer nextBuffer() {
        Deque<ByteBuffer> buffs = buffers;
        ByteBuffer buf = buffs.peek();

        while (buf != null && !buf.hasRemaining()) {
            buffs.poll();
            buf = buffs.peek();
        }

        return buf;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();

        int a = 0;
        for (var buffer : buffers) a += buffer.remaining();

        return a;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (mark == null) {
            throw new IOException("nothing to reset");
        }

        buffers.push(mark.flip());
        mark = null;
    }

    @Override
    public synchronized void mark(int readlimit) {
        if (readlimit <= 0) {
            mark = null;
        } else {
            mark = ByteBuffer.allocate(readlimit);
        }
    }

    private void mark0(int b) {
        ByteBuffer m;
        if ((m = mark) != null) {
            if (m.hasRemaining()) {
                m.put((byte) b);
            } else {
                mark = null;
            }
        }
    }

    private void mark0(byte[] b, int off, int len) {
        ByteBuffer m;
        if ((m = mark) != null) {
            if (len <= m.remaining()) {
                m.put(b, off, len);
            } else {
                mark = null;
            }
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void close() {
        buffers.clear();
        mark = null;
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
