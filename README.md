[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nstdio_http-client-ext&metric=alert_status)](https://sonarcloud.io/dashboard?id=nstdio_http-client-ext)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=nstdio_http-client-ext&metric=coverage)](https://sonarcloud.io/dashboard?id=nstdio_http-client-ext)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=nstdio_http-client-ext&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=nstdio_http-client-ext)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=nstdio_http-client-ext&metric=code_smells)](https://sonarcloud.io/dashboard?id=nstdio_http-client-ext)

# HttpClient Extensions

The project provides useful extensions to
JDK's [HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html).

### Gradle

```
implementation 'io.github.nstdio:http-client-ext:1.0.3'
```

### Maven

```
<dependency>
    <groupId>io.github.nstdio</groupId>
    <artifactId>http-client-ext</artifactId>
    <version>1.0.3</version>
</dependency>
```

### Caching

The `ExtendedHttpClient` implements client part of [RFC7234](https://datatracker.ietf.org/doc/html/rfc7234)

Here is the example of creating client with in memory cache:

```java
HttpClient client=ExtendedHttpClient.newBuilder()
        .cache(Cache.newInMemoryCacheBuilder().build())
        .build();

URI uri = URI.create("https://api.github.com/users/defunkt");
HttpRequest request = HttpRequest.newBuilder(uri).build();

HttpResponse<String> networkResponse = client.send(request, ofString());
HttpResponse<String> cachedResponse = client.send(request, ofString());
```

and all available configurations for in memory cache:

```java
Cache cache = Cache.newInMemoryCacheBuilder()
        .maxItems(4096) // number of responses can be cached
        .size(10 * 1000 * 1000) // maximum size of the entire cache in bytes, -1 for no constraint
        .requestFilter(request -> request.uri().getHost().equals("api.github.com")) // cache only requests that match given predicate
        .responseFilter(response -> response.statusCode() == 200) // cache only responses that match given predicate
        .build();
```
