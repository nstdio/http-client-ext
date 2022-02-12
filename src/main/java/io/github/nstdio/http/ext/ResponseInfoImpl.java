package io.github.nstdio.http.ext;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.ResponseInfo;

@Builder(builderClassName = "ResponseInfoBuilder")
@RequiredArgsConstructor
class ResponseInfoImpl implements ResponseInfo {
    private final int statusCode;
    private final HttpHeaders headers;
    private final Version version;

    static ResponseInfoBuilder toBuilder(ResponseInfo info) {
        return builder()
                .version(info.version())
                .headers(info.headers())
                .statusCode(info.statusCode());
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public Version version() {
        return version;
    }
}
