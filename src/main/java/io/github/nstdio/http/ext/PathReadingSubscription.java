package io.github.nstdio.http.ext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

class PathReadingSubscription implements Subscription {
    public static final int DEFAULT_BUFF_CAPACITY = 8192;
    private final Subscriber<List<ByteBuffer>> subscriber;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final Path path;
    private ReadableByteChannel channel;

    PathReadingSubscription(Subscriber<List<ByteBuffer>> subscriber, Path path) {
        this.subscriber = subscriber;
        this.path = path;
    }

    @Override
    public void request(long n) {
        if (completed.get()) {
            return;
        }

        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException("non-positive request"));
            return;
        }

        try {
            if (channel == null) {
                channel = Files.newByteChannel(path);
            }

            while (n-- > 0) {
                ByteBuffer buff = ByteBuffer.allocate(DEFAULT_BUFF_CAPACITY);
                int r = channel.read(buff);
                if (r > 0) {
                    buff.flip();
                    subscriber.onNext(List.of(buff));
                } else {
                    subscriber.onComplete();
                    completed.set(true);
                    break;
                }
            }

        } catch (IOException e) {
            subscriber.onError(e);
        }
    }

    @Override
    public void cancel() {
        completed.set(true);
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }
}
