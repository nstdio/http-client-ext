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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Buffers {
  private Buffers() {
  }

  static ByteBuffer duplicate(ByteBuffer buf) {
    var dup = buf.asReadOnlyBuffer();
    return dup.hasRemaining() ? dup : dup.flip();
  }

  static List<ByteBuffer> duplicate(List<ByteBuffer> item) {
    int s = item.size();
    switch (s) {
      case 0:
        return Collections.emptyList();
      case 1:
        return List.of(duplicate(item.get(0)));
      case 2:
        return List.of(duplicate(item.get(0)), duplicate(item.get(1)));
      default: {
        List<ByteBuffer> list = new ArrayList<>(s);
        for (ByteBuffer byteBuffer : item) {
          list.add(duplicate(byteBuffer));
        }

        return Collections.unmodifiableList(list);
      }
    }
  }
}
