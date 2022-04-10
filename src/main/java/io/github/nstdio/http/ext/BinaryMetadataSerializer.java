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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers.noBody;

class BinaryMetadataSerializer implements MetadataSerializer {
  private final StreamFactory streamFactory;

  BinaryMetadataSerializer(StreamFactory streamFactory) {
    this.streamFactory = streamFactory;
  }

  @Override
  public void write(CacheEntryMetadata metadata, Path path) {
    try (var out = new ObjectOutputStream(streamFactory.output(path))) {
      out.writeObject(new ExternalizableMetadata(metadata));
    } catch (IOException ignored) {
    }
  }

  @Override
  public CacheEntryMetadata read(Path path) {
    try (var input = new ObjectInputStream(streamFactory.input(path))) {
      return ((ExternalizableMetadata) input.readObject()).metadata;
    } catch (IOException | ClassNotFoundException ignored) {
      return null;
    }
  }

  static final class ExternalizableMetadata implements Externalizable {
    private static final long serialVersionUID = 15052410042022L;
    private CacheEntryMetadata metadata;

    public ExternalizableMetadata() {
    }

    ExternalizableMetadata(CacheEntryMetadata metadata) {
      this.metadata = metadata;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeLong(metadata.requestTime());
      out.writeLong(metadata.responseTime());

      out.writeObject(new ExternalizableResponseInfo(metadata.response()));
      out.writeObject(new ExternalizableHttpRequest(metadata.request()));
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      final long requestTime = in.readLong();
      final long responseTime = in.readLong();
      ResponseInfo responseInfo = ((ExternalizableResponseInfo) in.readObject()).responseInfo;
      HttpRequest request = ((ExternalizableHttpRequest) in.readObject()).request;

      metadata = CacheEntryMetadata.of(requestTime, responseTime, responseInfo, request, Clock.systemUTC());
    }
  }

  static class ExternalizableHttpRequest implements Externalizable {
    private static final long serialVersionUID = 15052410042022L;
    private HttpRequest request;

    public ExternalizableHttpRequest() {
    }

    ExternalizableHttpRequest(HttpRequest request) {
      this.request = request;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeObject(request.uri());
      out.writeUTF(request.method());
      out.writeObject(request.version().orElse(null));
      out.writeObject(request.timeout().orElse(null));
      out.writeObject(new ExternalizableHttpHeaders(request.headers()));
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      Builder builder = HttpRequest.newBuilder()
          .uri((URI) in.readObject())
          .method(in.readUTF(), noBody());

      Version v = (Version) in.readObject();
      if (v != null) {
        builder.version(v);
      }
      Duration t = (Duration) in.readObject();
      if (t != null) {
        builder.timeout(t);
      }

      Map<String, List<String>> headers = ((ExternalizableHttpHeaders) in.readObject()).headers.map();
      for (var entry : headers.entrySet()) {
        for (String value : entry.getValue()) {
          builder.header(entry.getKey(), value);
        }
      }

      request = builder.build();
    }
  }

  static class ExternalizableResponseInfo implements Externalizable {
    private static final long serialVersionUID = 15052410042022L;
    private ResponseInfo responseInfo;

    public ExternalizableResponseInfo() {
    }

    ExternalizableResponseInfo(ResponseInfo responseInfo) {
      this.responseInfo = responseInfo;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeInt(responseInfo.statusCode());
      out.writeObject(new ExternalizableHttpHeaders(responseInfo.headers()));
      out.writeObject(responseInfo.version());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      responseInfo = ImmutableResponseInfo.builder()
          .statusCode(in.readInt())
          .headers(((ExternalizableHttpHeaders) in.readObject()).headers)
          .version((Version) in.readObject())
          .build();
    }
  }

  static class ExternalizableHttpHeaders implements Externalizable {
    private static final long serialVersionUID = 15052410042022L;
    private static final int maxMapSize = 1024;
    private static final int maxListSize = 256;

    private final boolean respectLimits;
    HttpHeaders headers;

    public ExternalizableHttpHeaders() {
      this(null, true);
    }

    ExternalizableHttpHeaders(HttpHeaders headers) {
      this(headers, true);
    }

    ExternalizableHttpHeaders(HttpHeaders headers, boolean respectLimits) {
      this.headers = headers;
      this.respectLimits = respectLimits;
    }

    private static String mapSizeExceedMessage(int mapSize) {
      return String.format("The headers size exceeds max allowed number. Size: %d, Max:%d", mapSize, maxMapSize);
    }

    private static String listSizeExceedMessage(String headerName, int size) {
      return String.format("The values for header '%s' exceeds maximum allowed number. Size:%d, Max:%d",
          headerName, size, maxListSize);
    }

    private static IOException corruptedStream(String desc) {
      return new IOException("Corrupted stream: " + desc);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      Map<String, List<String>> map = headers.map();

      int mapSize = map.size();
      if (respectLimits && mapSize > maxMapSize) {
        throw new IOException(mapSizeExceedMessage(mapSize));
      }

      // write map size
      out.writeInt(mapSize);
      for (var entry : map.entrySet()) {
        // header name
        String headerName = entry.getKey();
        out.writeUTF(headerName);

        List<String> values = entry.getValue();
        int valuesSize = values.size();
        checkValuesSize(headerName, valuesSize);

        // write values size
        out.writeInt(valuesSize);
        // header values
        for (String value : values) out.writeUTF(value);
      }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
      final int mapSize = in.readInt();
      checkMapSize(mapSize);

      if (mapSize == 0) {
        headers = HttpHeaders.of(Map.of(), Headers.ALLOW_ALL);
        return;
      }

      var map = new HashMap<String, List<String>>(mapSize, 1.0f);
      for (int i = 0; i < mapSize; i++) {
        String headerName = in.readUTF();

        int valuesSize = in.readInt();
        checkValuesSize(headerName, valuesSize);

        var values = new ArrayList<String>(valuesSize);
        for (int j = 0; j < valuesSize; j++) values.add(in.readUTF());

        map.put(headerName, values);
      }

      headers = HttpHeaders.of(map, Headers.ALLOW_ALL);
    }

    private void checkValuesSize(String headerName, int valuesSize) throws IOException {
      if (valuesSize <= 0) throw corruptedStream("list size should be positive");
      else if (respectLimits && valuesSize > maxListSize)
        throw new IOException(listSizeExceedMessage(headerName, valuesSize));
    }

    private void checkMapSize(int mapSize) throws IOException {
      if (mapSize < 0) throw corruptedStream("map size cannot be negative");
      else if (respectLimits && mapSize > maxMapSize) throw new IOException(mapSizeExceedMessage(mapSize));
    }
  }
}
