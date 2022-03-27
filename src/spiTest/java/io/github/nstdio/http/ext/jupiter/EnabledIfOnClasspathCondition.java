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

package io.github.nstdio.http.ext.jupiter;

import java.util.List;

class EnabledIfOnClasspathCondition extends IfOnClasspathCondition<EnabledIfOnClasspath> {
  EnabledIfOnClasspathCondition() {
    super(EnabledIfOnClasspath.class, EnabledIfOnClasspath::value);
  }

  @Override
  boolean enabled(List<String> present, List<String> notPresent) {
    return notPresent.isEmpty();
  }
}
