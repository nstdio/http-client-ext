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

import static io.github.nstdio.http.ext.ExtendedHttpClient.toBuilder;
import static io.github.nstdio.http.ext.Headers.HEADER_CONTENT_ENCODING;
import static io.github.nstdio.http.ext.Headers.HEADER_CONTENT_LENGTH;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

class CompressionInterceptor implements Interceptor {
    @Override
    public <T> Chain<T> intercept(Chain<T> in) {
        var handler = decompressingHandler(in.ctx().<T>bodyHandler());

        AsyncHandler<T> fn = AsyncHandler.of(r -> {
            List<String> directives = handler.directives(r.headers());
            if (!directives.isEmpty()) {
                return removeCompressionHeaders(r, directives);
            }

            return r;
        });

        RequestContext ctx = in.ctx()
                .withRequest(preProcessRequest(in.ctx().request()))
                .withBodyHandler(handler);

        AsyncHandler<T> asyncHandler = in.asyncHandler().andThen(fn);
        Optional<HttpResponse<T>> response = in.response();

        return Chain.of(ctx, asyncHandler, response);
    }

    private HttpRequest preProcessRequest(HttpRequest request) {
        HttpRequest.Builder builder = toBuilder(request);
        builder.setHeader("Accept-Encoding", "gzip,deflate");

        return builder.build();
    }

    private <T> DecompressingBodyHandler<T> decompressingHandler(HttpResponse.BodyHandler<T> bodyHandler) {
        return new DecompressingBodyHandler<>(bodyHandler, DecompressingBodyHandler.Options.LENIENT);
    }

    private <T> HttpResponse<T> removeCompressionHeaders(HttpResponse<T> response, List<String> directives) {
        String contentEncoding = response.headers()
                .firstValue(HEADER_CONTENT_ENCODING)
                .stream()
                .flatMap(Headers::splitComma)
                .filter(not(directives::contains))
                .collect(joining(","));

        var headersBuilder = new HttpHeadersBuilder(response.headers())
                .remove(HEADER_CONTENT_LENGTH);

        if (contentEncoding.isBlank()) {
            headersBuilder.remove(HEADER_CONTENT_ENCODING);
        } else {
            headersBuilder.set(HEADER_CONTENT_ENCODING, contentEncoding);
        }

        return Responses.headersReplacing(response, headersBuilder.build());
    }
}
