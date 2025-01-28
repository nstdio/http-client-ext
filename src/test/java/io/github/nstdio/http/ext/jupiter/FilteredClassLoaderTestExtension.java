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

package io.github.nstdio.http.ext.jupiter;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

public class FilteredClassLoaderTestExtension implements BeforeEachCallback, AfterEachCallback {

  private static final Namespace NAMESPACE = Namespace.create(FilteredClassLoaderTestExtension.class);

  private static Store getStore(ExtensionContext context) {
    return context.getStore(NAMESPACE);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    findAnnotation(context.getElement(), FilteredClassLoaderTest.class)
        .filter(ann -> ann.value().length > 0)
        .ifPresent(ann -> {
          var thread = Thread.currentThread();
          var threadContextClassLoader = thread.getContextClassLoader();
          getStore(context).put(thread, threadContextClassLoader);

          thread.setContextClassLoader(new FilteredClassLoader(threadContextClassLoader, ann.value()));
        });
  }

  @Override
  public void afterEach(ExtensionContext context) {
    var thread = Thread.currentThread();

    thread.setContextClassLoader(getStore(context).get(thread, ClassLoader.class));
  }
}
