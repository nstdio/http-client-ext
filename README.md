# HttpClient Extensions

The project provides useful extensions to
JDK's [HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html).

### Status
| Type          | Status                                                                                                                                                                                                                                                                                                                                                       |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Build         | [![Build](https://github.com/nstdio/http-client-ext/actions/workflows/build.yaml/badge.svg)](https://github.com/nstdio/http-client-ext/actions/workflows/build.yaml)                                                                                                                                                                                         |
| Artifact      | [![Maven Central](https://img.shields.io/maven-central/v/io.github.nstdio/http-client-ext.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.nstdio%22%20AND%20a:%22http-client-ext%22)                                                                                                                                            |
| Javadoc       | [![javadoc](https://javadoc.io/badge2/io.github.nstdio/http-client-ext/javadoc.svg)](https://javadoc.io/doc/io.github.nstdio/http-client-ext)                                                                                                                                                                                                                |
| Code coverage | [![codecov](https://codecov.io/gh/nstdio/http-client-ext/branch/main/graph/badge.svg)](https://codecov.io/gh/nstdio/http-client-ext)                                                                                                                                                                                                                         |
| LGTM          | [![Total alerts](https://img.shields.io/lgtm/alerts/g/nstdio/http-client-ext.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/nstdio/http-client-ext/alerts/) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/nstdio/http-client-ext.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/nstdio/http-client-ext/context:java) |

### Requirements

- Java 11+ 
- [Gradle](https://gradle.org/) for building the project.

### Gradle

```
implementation 'io.github.nstdio:http-client-ext:2.3.0'
```

### Maven

```
<dependency>
    <groupId>io.github.nstdio</groupId>
    <artifactId>http-client-ext</artifactId>
    <version>2.3.0</version>
</dependency>
```
### Features

- [Caching](#Caching), both in memory and disk.
- [Decompression](#Decompression): `gzip, deflate`
- [JSON](#JSON) mappings

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
Cache inMemory = Cache.newInMemoryCacheBuilder()
        .maxItems(4096) // number of responses can be cached
        .size(10 * 1000 * 1000) // maximum size of the entire cache in bytes, -1 for no constraint
        .requestFilter(request -> request.uri().getHost().equals("api.github.com")) // cache only requests that match given predicate
        .responseFilter(response -> response.statusCode() == 200) // cache only responses that match given predicate
        .build();
```

Above-mentioned configurations also applies to persistent cache with some additions

```java
Path cacheDir = ...
Cache disk = Cache.newDiskCacheBuilder()
        .dir(cacheDir)
        .build();        
```
If request/response contains sensitive information one might want to store it encrypted:

```java
Path cacheDir = ...
SecretKey secretKey = ...

Cache encrypted = Cache.newDiskCacheBuilder()
        .dir(cacheDir)
        .encrypted()
        .key(secretKey)
        .cipherAlgorithm("AES")
        .build();
```
will create persistent cache which encrypts data by user provided key.

### Decompression
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
Out of the box support for `gzip` and `deflate` is provided by JDK itself. For `br` (brotli) compression please add
one of following dependencies to you project:

- [org.brotli:dec](https://mvnrepository.com/artifact/org.brotli/dec/0.1.2)
- [Brotli4j](https://github.com/hyperxpro/Brotli4j)

service loader will pick up correct dependency. If none of these preferred there is always an options to extend via [SPI](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html)
by providing [CompressionFactory](https://github.com/nstdio/http-client-ext/blob/main/src/main/java/io/github/nstdio/http/ext/spi/CompressionFactory.java)

### JSON
Currently, two libraries are supported

- [Jackson](https://github.com/FasterXML/jackson-databind)
- [Gson](https://github.com/google/gson)

just drop one of them as dependency and voilà

```java
// will create object using Jackson or Gson
User user = client.send(request, BodyHandlers.ofJson(User.class));

// or send JSON
Object user = null;
HttpRequest request = HttpRequest.newBuilder()
  .uri(URI.create("https://example.com/users"))
  .POST(BodyPublishers.ofJson(user))
  .build();

ExtendedHttpClient client = ExtendedHttpClient.newHttpClient();
HttpResponse<User> response = client.send(request, BodyHandlers.ofJson(User.class));
```

And if special configuration required
```java
package com.example;

class JacksonMappingProvider implements JsonMappingProvider {
  @Override
  public JsonMapping get() {
    // configure jackson
    ObjectMapper mapper = ...

    return new JacksonJsonMapping(mapper);
  }
}
```
then  standard SPI registration can be used or custom provider can be registered manually:

```java
JacksonMappingProvider jackson = ...

JsonMappingProvider.addProvider(jackson);
```
