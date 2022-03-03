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

import java.time.Duration;
import java.util.Optional;

class Properties {
    private static final String NAMESPACE = "io.github.nstdio.http.ext";

    static Optional<Duration> duration(String propertyName) {
        return property(propertyName).map(Duration::parse);
    }

    static Optional<String> property(String propertyName) {
        final String prop = withNamespace(propertyName);
        return Optional.ofNullable(System.getProperty(prop))
                .or(() -> Optional.ofNullable(System.getenv(propertyToEnv(prop))));
    }

    private static String withNamespace(String propertyName) {
        return propertyName.startsWith(NAMESPACE) ? propertyName : NAMESPACE + '.' + propertyName;
    }

    private static String propertyToEnv(String prop) {
        return prop.replace('.', '_').toUpperCase();
    }
}
