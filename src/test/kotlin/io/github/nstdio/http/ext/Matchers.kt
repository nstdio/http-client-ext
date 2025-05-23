/*
 * Copyright (C) 2022-2025 the original author or authors.
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
package io.github.nstdio.http.ext

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import java.net.http.HttpResponse

object Matchers {
  @JvmStatic
  fun <T> isCached(): BaseMatcher<HttpResponse<T>?> {
    return object : BaseMatcher<HttpResponse<T>?>() {
      override fun matches(actual: Any): Boolean {
        return actual is CachedHttpResponse<*>
      }

      override fun describeTo(description: Description) {
        description.appendText("a cached response")
      }
    }
  }
}