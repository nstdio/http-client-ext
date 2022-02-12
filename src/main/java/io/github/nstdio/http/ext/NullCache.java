package io.github.nstdio.http.ext;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.function.Consumer;

class NullCache implements Cache {
    static final NullCache INSTANCE = new NullCache();

    private static final Writer<Object> WRITER = new Writer<>() {
        @Override
        public HttpResponse.BodySubscriber<Object> subscriber() {
            return BodySubscribers.mapping(BodySubscribers.discarding(), unused -> null);
        }

        @Override
        public Consumer<Object> finisher() {
            return t -> {
            };
        }
    };

    private NullCache() {
    }

    @SuppressWarnings("unchecked")
    static <T> Writer<T> writer() {
        return (Writer<T>) WRITER;
    }

    @Override
    public CacheEntry get(HttpRequest request) {
        return null;
    }

    @Override
    public void put(HttpRequest request, CacheEntry entry) {
        // intentional noop
    }

    @Override
    public void evict(HttpRequest request) {
        // intentional noop
    }

    @Override
    public void evictAll(HttpRequest request) {
        // intentional noop
    }

    @Override
    public void evictAll() {
        // intentional noop
    }

    @Override
    public <T> Writer<T> writer(CacheEntryMetadata metadata) {
        return writer();
    }
}
