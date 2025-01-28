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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

public final class FilteredClassLoader extends URLClassLoader {

  private final List<Class<?>> classes;

  public FilteredClassLoader(Class<?>... classes) {
    this(FilteredClassLoader.class.getClassLoader(), classes);
  }

  public FilteredClassLoader(ClassLoader parent, Class<?>... classes) {
    super(new URL[0], parent);
    this.classes = Arrays.asList(classes);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    for (var cls : classes) {
      if (cls.getName().equals(name)) {
        throw new ClassNotFoundException();
      }
    }

    return super.loadClass(name, resolve);
  }
}
