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
@file:Suppress("PrivatePropertyName")

package io.github.nstdio.http.ext.archunit

import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThat
import com.tngtech.archunit.lang.conditions.ArchPredicates.are
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.GeneralCodingRules
import java.nio.file.Files

@AnalyzeClasses(packages = ["io.github.nstdio.http.ext"], importOptions = [ImportOption.DoNotIncludeTests::class])
internal object CodingRulesTest {

  @ArchTest
  @Suppress("unused")
  private val no_access_to_standard_streams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS

  @Suppress("unused")
  private val no_direct_access_to_files = noClasses()
    .that(are(not(simpleName("SimpleStreamFactory"))))
    .should(accessToIO())


  private fun accessToIO(): ArchCondition<JavaClass> {
    return accessClassesThat(are(equivalentTo(Files::class.java)))
  }
}