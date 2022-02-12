package io.github.nstdio.http.ext;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.net.http.HttpResponse;

public class Matchers {
    private Matchers() {
    }

    public static <T> Matcher<HttpResponse<T>> isCached() {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object actual) {
                return actual instanceof CachedHttpResponse;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a cached response");
            }
        };
    }
}
