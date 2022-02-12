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
    private final Consumer<V> evictListener;
    private final int maxSize;
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
        List<V> vs = m.computeIfAbsent(key, k -> new ArrayList<>());

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
            while (!old.isEmpty()) {
                notifyEvicted(removeEldest(old));
                size--;
            }
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
            while (evictEldest(it, true)) {}
        }
    }
}
