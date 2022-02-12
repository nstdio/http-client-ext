package io.github.nstdio.http.ext;

import static io.github.nstdio.http.ext.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

class LruMultimapTest {

    private static final Consumer<String> NOOP = s -> {
    };
    private static final ToIntFunction<List<String>> ADD_FN = s -> -1;
    private static final ToIntFunction<List<String>> THROWING_FN = s -> {
        throw new IllegalStateException("Should not be invoked!");
    };

    @Test
    void shouldReplaceAndNotify() {
        //given
        @SuppressWarnings("unchecked")
        Consumer<String> mockEvictionListener = Mockito.mock(Consumer.class);

        var map = new LruMultimap<String, String>(512, mockEvictionListener);

        //when
        var putResult1 = map.putSingle("a", "1", strings -> 0);
        var putResult2 = map.putSingle("a", "2", strings -> 0);
        var getResult = map.getSingle("a", strings -> 0);

        //then
        assertThat(map)
                .hasMapSize(1)
                .hasSize(1)
                .hasOnlyValue("a", "2", 0);
        assertThat(putResult2).containsOnly("2");
        assertThat(getResult).isEqualTo("2");

        verify(mockEvictionListener).accept("1");
        verifyNoMoreInteractions(mockEvictionListener);
    }

    @Test
    void shouldMaintainLruForLists() {
        //given
        var map = new LruMultimap<String, String>(512, s -> {
        });

        //when + then
        map.putSingle("a", "1", ADD_FN);
        var putResult = map.putSingle("a", "2", ADD_FN);

        map.getSingle("a", s -> 0);
        assertThat(putResult).containsExactly("2", "1");
        map.getSingle("a", s -> 1);
        assertThat(putResult).containsExactly("1", "2");

        assertThat(map).hasMapSize(1).hasSize(2);
    }

    @Test
    void shouldNotEvictEldestWhenEmpty() {
        //given
        var map = new LruMultimap<String, String>(512, NOOP);

        //when + then
        for (int i = 0; i < 32; i++) {
            assertFalse(map.evictEldest());
        }
    }

    @Test
    void shouldEvictEldest() {
        //given
        @SuppressWarnings("unchecked")
        Consumer<String> mockEvictionListener = Mockito.mock(Consumer.class);
        var map = new LruMultimap<String, String>(512, mockEvictionListener);

        //when
        map.putSingle("a", "1", ADD_FN);
        map.putSingle("a", "2", ADD_FN);

        map.putSingle("b", "1", ADD_FN);
        map.putSingle("b", "2", ADD_FN);

        var evictionResult = map.evictEldest();

        //then
        assertTrue(evictionResult);
        assertThat(map).hasMapSize(2).hasSize(3)
                .hasOnlyValue("a", "2", 0);

        verify(mockEvictionListener).accept("1");
        verifyNoMoreInteractions(mockEvictionListener);
    }

    @Test
    void shouldRemoveMapEntryWhenLastRemoved() {
        //given
        @SuppressWarnings("unchecked")
        Consumer<String> mockEvictionListener = Mockito.mock(Consumer.class);
        var map = new LruMultimap<String, String>(512, mockEvictionListener);

        //when
        map.putSingle("a", "1", ADD_FN);

        map.putSingle("b", "1", ADD_FN);
        map.putSingle("b", "2", ADD_FN);

        var evictionResult = map.evictEldest();

        //then
        assertTrue(evictionResult);
        assertThat(map).hasMapSize(1).hasSize(2);

        verify(mockEvictionListener).accept("1");
        verifyNoMoreInteractions(mockEvictionListener);
    }

    @Test
    void shouldRespectMaxSize() {
        //given
        @SuppressWarnings("unchecked")
        Consumer<String> mockEvictionListener = Mockito.mock(Consumer.class);
        var map = new LruMultimap<String, String>(2, mockEvictionListener);

        //when
        map.putSingle("b", "2", ADD_FN);
        map.putSingle("b", "1", ADD_FN);
        map.putSingle("a", "1", ADD_FN);

        //then
        assertThat(map)
                .hasMapSize(2)
                .hasSize(2)
                .hasOnlyValue("a", "1", 0)
                .hasOnlyValue("b", "1", 0);
        verify(mockEvictionListener).accept("2");
        verifyNoMoreInteractions(mockEvictionListener);
    }

    @Test
    void shouldClearAll() {
        //given
        @SuppressWarnings("unchecked")
        Consumer<String> mockEl = Mockito.mock(Consumer.class);
        var map = new LruMultimap<String, String>(23, mockEl);

        //when
        map.putSingle("b", "1", ADD_FN);
        map.putSingle("b", "2", ADD_FN);
        map.putSingle("b", "3", ADD_FN);
        map.putSingle("a", "4", ADD_FN);

        map.clear();

        //then
        assertThat(map).hasMapSize(0).hasSize(0);

        var inOrder = inOrder(mockEl);
        inOrder.verify(mockEl).accept("1");
        inOrder.verify(mockEl).accept("2");
        inOrder.verify(mockEl).accept("3");
        inOrder.verify(mockEl).accept("4");

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldClearWhenEmpty() {
        //given
        @SuppressWarnings("unchecked")
        Consumer<String> mockEl = Mockito.mock(Consumer.class);
        var map = new LruMultimap<String, String>(23, mockEl);

        //when
        map.clear();

        //then
        assertThat(map).isEmpty();
        verifyNoInteractions(mockEl);
    }

    @Test
    void shouldNotGetWhenIndexIncorrect() {
        var map = new LruMultimap<String, String>(23, NOOP);

        //when
        map.putSingle("a", "2", ADD_FN);
        map.putSingle("a", "1", ADD_FN);

        //then
        assertThat(map.getSingle("b", THROWING_FN)).isNull();
        assertThat(map.getSingle("a", s -> 5)).isNull();
        assertThat(map.getSingle("a", s -> -1)).isNull();
    }

    @Test
    void shouldEvictAllForExistingKey() {
        //given
        @SuppressWarnings("unchecked")
        Consumer<String> mockEl = Mockito.mock(Consumer.class);
        var map = new LruMultimap<String, String>(23, mockEl);

        //when
        map.putSingle("a", "1", ADD_FN);
        map.putSingle("a", "2", ADD_FN);

        map.evictAll("a");

        //then
        assertThat(map).isEmpty();
        var inOrder = inOrder(mockEl);
        inOrder.verify(mockEl).accept("1");
        inOrder.verify(mockEl).accept("2");

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldEvictAllForNonExistingKey() {
        //given
        @SuppressWarnings("unchecked")
        Consumer<String> mockEl = Mockito.mock(Consumer.class);
        var map = new LruMultimap<String, String>(23, mockEl);

        //when
        map.putSingle("a", "1", ADD_FN);
        map.putSingle("a", "2", ADD_FN);

        map.evictAll("b");

        //then
        assertThat(map).hasMapSize(1).hasSize(2);
        verifyNoInteractions(mockEl);
    }
}