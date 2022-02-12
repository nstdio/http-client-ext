package io.github.nstdio.http.ext;

import static io.github.nstdio.http.ext.Headers.splitComma;
import static java.util.Map.entry;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class Helpers {
    public static HttpResponse.ResponseInfo responseInfo(Map<String, String> headers) {
        return new HttpResponse.ResponseInfo() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpHeaders headers() {
                return headers.entrySet()
                        .stream()
                        .map(e -> entry(e.getKey(), List.of(e.getValue())))
                        .collect(collectingAndThen(toMap(Map.Entry::getKey, Map.Entry::getValue), map -> HttpHeaders.of(map, (s, s2) -> true)));
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }
}
