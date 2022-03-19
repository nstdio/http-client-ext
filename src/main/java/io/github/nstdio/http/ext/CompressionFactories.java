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

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

import io.github.nstdio.http.ext.spi.CompressionFactory;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;

class CompressionFactories {
    private static final ServiceLoader<CompressionFactory> loader = ServiceLoader.load(CompressionFactory.class);
    private static final List<String> allSupported;
    private static final String SPI_PACKAGE = "io.github.nstdio.http.ext.spi";
    static final Comparator<String> USERS_FIRST_COMPARATOR = comparingInt(o -> o.startsWith(SPI_PACKAGE) ? 1 : 0);
    private static final Comparator<Provider<CompressionFactory>> PROVIDER_COMPARATOR = (o1, o2) -> USERS_FIRST_COMPARATOR.compare(o1.type().getName(), o2.type().getName());

    static {
        allSupported = factory()
                .flatMap(factory -> factory.supported().stream())
                .collect(collectingAndThen(toCollection(LinkedHashSet::new), List::copyOf));
    }

    private CompressionFactories() {
    }

    static CompressionFactory firstSupporting(String directive) {
        return factory()
                .filter(factory -> factory.supported().contains(directive))
                .findFirst()
                .orElse(null);
    }

    private static Stream<CompressionFactory> factory() {
        return loader.stream().sorted(PROVIDER_COMPARATOR).map(Provider::get);
    }

    static List<String> allSupported() {
        return allSupported;
    }
}
