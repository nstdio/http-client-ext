package io.github.nstdio.http.ext;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
final class RequestContext {
    @With
    private final HttpRequest request;
    private final BodyHandler<?> bodyHandler;
    private final CacheControl cacheControl;
    private final AtomicLong requestTime = new AtomicLong();
    private final AtomicLong responseTime = new AtomicLong();

    static RequestContext of(HttpRequest request, BodyHandler<?> responseBodyHandler) {
        return new RequestContext(request, responseBodyHandler, CacheControl.of(request));
    }

    boolean isCacheable() {
        if (!"GET".equals(request.method())) return false;

        return !cacheControl.noStore();
    }

    long requestTimeLong() {
        return requestTime.get();
    }

    long responseTimeLong() {
        return responseTime.get();
    }

    @SuppressWarnings("unchecked")
    <T> BodyHandler<T> bodyHandler() {
        return (BodyHandler<T>) bodyHandler;
    }
}
