[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nstdio_http-client-ext&metric=alert_status)](https://sonarcloud.io/dashboard?id=nstdio_http-client-ext)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=nstdio_http-client-ext&metric=coverage)](https://sonarcloud.io/dashboard?id=nstdio_http-client-ext)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=nstdio_http-client-ext&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=nstdio_http-client-ext)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=nstdio_http-client-ext&metric=code_smells)](https://sonarcloud.io/dashboard?id=nstdio_http-client-ext)

# HttpClient Extensions

The project provides useful extensions to
JDK's [HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html).

### Gradle

```
implementation 'io.github.nstdio:http-client-ext:1.2.2'
```

### Maven

```
<dependency>
    <groupId>io.github.nstdio</groupId>
    <artifactId>http-client-ext</artifactId>
    <version>1.2.2</version>
</dependency>
```

### Caching

The `ExtendedHttpClient` implements client part of [RFC7234](https://datatracker.ietf.org/doc/html/rfc7234)

There are two types of cache:
```
// backed by volotile in-memory storage
Cache mem = Cache.newInMemoryCacheBuilder().build()

// and persistent storage
Cache disk = Cache.newDiskCacheBuilder().dir(Path.of("...")).build()
```

Here is the example of creating client with in memory cache:

```java
HttpClient client = ExtendedHttpClient.newBuilder()
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

the exact same configuration applies to persistent builder with addition of [DiskCacheBuilder#dir](https://github.com/nstdio/http-client-ext/blob/main/src/main/java/io/github/nstdio/http/ext/Cache.java#L163)

### Decompression (gzip, deflate)
Here is an example of transparent encoding feature

```java
HttpClient client = ExtendedHttpClient.newBuilder()
        .transparentEncoding(true)
        .build();

URI uri = URI.create("https://api.github.com/users/defunkt");
HttpRequest request = HttpRequest.newBuilder(uri).build();

// Client will add `Accept-Encoding: gzip, deflate` header to the request
// then decompress response body transparently of the user        
HttpResponse<String> response = client.send(request, ofString());
```

there is also dedicated `BodyHandler` for that, but in this case user should add `Accept-Encoding` header manually

```java
HttpClient client = HttpClient.newClient();

URI uri = URI.create("https://api.github.com/users/defunkt");
HttpRequest request = HttpRequest.newBuilder(uri)
        .header("Accept-Encoding", "gzip, deflate")
        .build();

HttpResponse<String> response = client.send(request, BodyHandlers.ofDecompressing(ofString()));
```

### JSON
We are using [Jackson](https://github.com/FasterXML/jackson-databind) to create Java objects from JSON.

```java
// simple type
client.send(request, BodyHandlers.ofJson(User.class));

// complex adhoc type
client.send(request, BodyHandlers.ofJson(new TypeReference<Map<String, List<String>>>() {}));

// with existing mapper
ObjectMapper objectMapper = ...
client.send(request, responseInfo -> BodySubscribers.ofJson(objectMapper, User.class));
```