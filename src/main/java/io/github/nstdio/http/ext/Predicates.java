package io.github.nstdio.http.ext;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The request/response predicates to use with {@link io.github.nstdio.http.ext.Cache.InMemoryCacheBuilder#requestFilter(Predicate)}
 * and {@link io.github.nstdio.http.ext.Cache.InMemoryCacheBuilder#responseFilter(Predicate)}.
 */
public final class Predicates {
    private Predicates() {
    }

    static <T> Predicate<T> alwaysTrue() {
        return t -> true;
    }

    /**
     * The {@code Predicate} that matches {@code HttpRequest}s with given {@code uri}.
     */
    public static Predicate<HttpRequest> uri(URI uri) {
        Objects.requireNonNull(uri);
        return r -> r.uri().equals(uri);
    }
}
