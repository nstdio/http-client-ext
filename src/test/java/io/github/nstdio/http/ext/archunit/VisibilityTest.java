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

package io.github.nstdio.http.ext.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.nstdio.http.ext.*;
import io.github.nstdio.http.ext.spi.CompressionFactory;
import io.github.nstdio.http.ext.spi.IdentityCompressionFactory;
import io.github.nstdio.http.ext.spi.JdkCompressionFactory;
import io.github.nstdio.http.ext.spi.OptionalBrotliCompressionFactory;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "io.github.nstdio.http.ext", importOptions = {ImportOption.DoNotIncludeTests.class})
class VisibilityTest {
  @ArchTest
  static ArchRule all_classes_expect_listed_should_not_be_public =
      classes()
          .that(not(ExtendedHttpClient.class)
              .and(not(ExtendedHttpClient.Builder.class))
              .and(not(BodyHandlers.DecompressingBodyHandlerBuilder.class))
              .and(not(BodyHandlers.class))
              .and(not(BodySubscribers.class))
              .and(not(Cache.CacheBuilder.class))
              .and(not(Cache.CacheEntry.class))
              .and(not(Cache.InMemoryCacheBuilder.class))
              .and(not(Cache.DiskCacheBuilder.class))
              .and(not(Cache.Writer.class))
              .and(not(CacheEntryMetadata.class))
              .and(not(Cache.class))
              .and(not(Cache.CacheStats.class))
              .and(not(CacheControl.CacheControlBuilder.class))
              .and(not(CacheControl.class))
              .and(not(Predicates.class))
              .and(not(CompressionFactory.class))
              .and(not(JdkCompressionFactory.class))
              .and(not(IdentityCompressionFactory.class))
              .and(not(OptionalBrotliCompressionFactory.class))
          )
          .should()
          .notBePublic();

  private static DescribedPredicate<JavaClass> not(Class<?> cls) {
    return are(DescribedPredicate.not(equivalentTo(cls)));
  }
}
