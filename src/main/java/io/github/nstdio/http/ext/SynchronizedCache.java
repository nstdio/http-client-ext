package io.github.nstdio.http.ext;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

class SynchronizedCache implements Cache {
    private final Cache delegate;

    SynchronizedCache(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized CacheEntry get(HttpRequest request) {
        return delegate.get(request);
    }

    @Override
    public synchronized void put(HttpRequest request, CacheEntry entry) {
        delegate.put(request, entry);
    }

    @Override
    public synchronized void evict(HttpRequest request) {
        delegate.evict(request);
    }

    @Override
    public synchronized void evictAll(HttpRequest request) {
        delegate.evictAll(request);
    }

    @Override
    public synchronized void evictAll() {
        delegate.evictAll();
    }

    @Override
    public <T> Writer<T> writer(CacheEntryMetadata metadata) {
        Writer<T> writer = delegate.writer(metadata);
        return new Writer<>() {
            @Override
            public HttpResponse.BodySubscriber<T> subscriber() {
                return writer.subscriber();
            }

            @Override
            public Consumer<T> finisher() {
                return t -> {
                    synchronized (this) {
                        writer.finisher().accept(t);
                    }
                };
            }
        };
    }
}
