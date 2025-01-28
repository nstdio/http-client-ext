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
package io.github.nstdio.http.ext.archunit

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.conditions.ArchPredicates.are
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import io.github.nstdio.http.ext.BodyHandlers
import io.github.nstdio.http.ext.BodyHandlers.DecompressingBodyHandlerBuilder
import io.github.nstdio.http.ext.BodyPublishers
import io.github.nstdio.http.ext.BodySubscribers
import io.github.nstdio.http.ext.Cache
import io.github.nstdio.http.ext.Cache.DiskCacheBuilder
import io.github.nstdio.http.ext.Cache.EncryptedDiskCacheBuilder
import io.github.nstdio.http.ext.Cache.InMemoryCacheBuilder
import io.github.nstdio.http.ext.CacheControl
import io.github.nstdio.http.ext.CacheControl.CacheControlBuilder
import io.github.nstdio.http.ext.CacheEntryMetadata
import io.github.nstdio.http.ext.ExtendedHttpClient
import io.github.nstdio.http.ext.Predicates
import io.github.nstdio.http.ext.spi.Classpath
import io.github.nstdio.http.ext.spi.CompressionFactory
import io.github.nstdio.http.ext.spi.GsonJsonMapping
import io.github.nstdio.http.ext.spi.IdentityCompressionFactory
import io.github.nstdio.http.ext.spi.JacksonJsonMapping
import io.github.nstdio.http.ext.spi.JdkCompressionFactory
import io.github.nstdio.http.ext.spi.JsonMapping
import io.github.nstdio.http.ext.spi.JsonMappingProvider
import io.github.nstdio.http.ext.spi.JsonMappingProviderNotFoundException
import io.github.nstdio.http.ext.spi.OptionalBrotliCompressionFactory
import io.github.nstdio.http.ext.spi.OptionalZstdCompressionFactory


@AnalyzeClasses(packages = ["io.github.nstdio.http.ext"], importOptions = [DoNotIncludeTests::class])
internal object VisibilityTest {
  @ArchTest
  @Suppress("unused")
  var all_classes_expect_listed_should_not_be_public: ArchRule = ArchRuleDefinition.classes()
    .that(
      not(ExtendedHttpClient::class.java)
        .and(not(ExtendedHttpClient.Builder::class.java))
        .and(not(DecompressingBodyHandlerBuilder::class.java))
        .and(not(BodyHandlers::class.java))
        .and(not(BodySubscribers::class.java))
        .and(not(BodyPublishers::class.java))
        .and(not(Cache.CacheBuilder::class.java))
        .and(not(Cache.CacheEntry::class.java))
        .and(not(InMemoryCacheBuilder::class.java))
        .and(not(DiskCacheBuilder::class.java))
        .and(not(EncryptedDiskCacheBuilder::class.java))
        .and(not(Cache.Writer::class.java))
        .and(not(CacheEntryMetadata::class.java))
        .and(not(Cache::class.java))
        .and(not(Cache.CacheStats::class.java))
        .and(not(CacheControlBuilder::class.java))
        .and(not(CacheControl::class.java))
        .and(not(Predicates::class.java))
        .and(not(CompressionFactory::class.java))
        .and(not(JdkCompressionFactory::class.java))
        .and(not(IdentityCompressionFactory::class.java))
        .and(not(OptionalBrotliCompressionFactory::class.java))
        .and(not(OptionalZstdCompressionFactory::class.java))
        .and(not(JsonMappingProvider::class.java))
        .and(not(JsonMapping::class.java))
        .and(not(JacksonJsonMapping::class.java))
        .and(not(GsonJsonMapping::class.java))
        .and(not(JsonMappingProviderNotFoundException::class.java))
        .and(not(Classpath::class.java))
    )
    .should()
    .notBePublic()

  private fun not(cls: Class<*>): DescribedPredicate<JavaClass> {
    return are(not(equivalentTo(cls)))
  }
}