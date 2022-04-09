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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

class LruMultimap<K, V> {
  private final Map<K, List<V>> m;
  private final int maxSize;
  private Consumer<V> evictListener;
  private int size;

  LruMultimap(int maxSize, Consumer<V> evictListener) {
    m = new LinkedHashMap<>(maxSize + 1, 0.75f, true);
    this.maxSize = maxSize;
    this.evictListener = evictListener;
  }

  int size() {
    return size;
  }

  int mapSize() {
    return m.size();
  }

  V getSingle(K k, ToIntFunction<List<V>> idxFn) {
    List<V> vs = m.get(k);

    V v = null;
    int i;
    if (vs != null && (i = idxFn.applyAsInt(vs)) >= 0 && i < vs.size()) {
      v = vs.get(i);

      // Bring element to the front
      // if it already isn't there
      if (i != 0) {
        vs.remove(i);
        vs.add(0, v);
      }
    }

    return v;
  }

  List<V> putSingle(K key, V value, ToIntFunction<List<V>> idxFn) {
    List<V> vs = m.computeIfAbsent(key, k -> new ArrayList<>(1));

    int i;
    if (!vs.isEmpty() && (i = idxFn.applyAsInt(vs)) != -1) {
      notifyEvicted(vs.set(i, value));
    } else {
      vs.add(0, value);
      size++;

      if (size > maxSize) {
        evictEldest();
      }
    }

    return Collections.unmodifiableList(vs);
  }

  boolean evictEldest() {
    return size != 0 && evictEldest(m.entrySet().iterator(), false);
  }

  private boolean evictEldest(Iterator<Map.Entry<K, List<V>>> it, boolean batch) {
    boolean evicted = false;
    if (it.hasNext()) {
      var eldest = it.next();
      var ev = eldest.getValue();
      do {
        notifyEvicted(removeEldest(ev));
        size--;
      } while (batch && !ev.isEmpty());

      if (ev.isEmpty()) {
        it.remove();
      }
      evicted = true;
    }

    return evicted;
  }

  private V removeEldest(List<V> vs) {
    return vs.remove(vs.size() - 1);
  }

  void evictAll(K k) {
    List<V> old = m.remove(k);
    if (old != null) {
      int len = old.size();
      for (int i = len - 1; i >= 0; i--) {
        notifyEvicted(old.get(i));
      }

      this.size -= len;
    }
  }

  void addEvictionListener(Consumer<V> l) {
    if (evictListener == null) {
      evictListener = l;
    } else {
      evictListener = evictListener.andThen(l);
    }
  }

  V remove(K k, ToIntFunction<List<V>> idxFn) {
    List<V> vs = m.get(k);

    V v = null;
    int i = idxFn.applyAsInt(vs);
    if (i != -1) {
      v = vs.remove(i);
      notifyEvicted(v);
      size--;
      if (vs.isEmpty()) {
        m.remove(k);
      }
    }

    return v;
  }

  private void notifyEvicted(V v) {
    if (evictListener != null)
      evictListener.accept(v);
  }

  void clear() {
    if (size != 0) {
      var it = m.entrySet().iterator();
      while (evictEldest(it, true)) {
      }
    }
  }
}
