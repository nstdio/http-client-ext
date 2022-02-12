package io.github.nstdio.http.ext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;

public class DiskExtendedHttpClientIntegrationTest implements ExtendedHttpClientContract {
    private ExtendedHttpClient client;

    @BeforeEach
    void setUp(@TempDir Path cacheDir) {
        var cache = new DiskCache(cacheDir);
        client = new ExtendedHttpClient(HttpClient.newHttpClient(), cache, Clock.systemUTC());
    }

    @Override
    public ExtendedHttpClient client() {
        return client;
    }
}
