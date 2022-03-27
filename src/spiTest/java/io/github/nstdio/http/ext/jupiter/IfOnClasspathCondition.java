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

import io.github.nstdio.http.ext.spi.Classpath;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

abstract class IfOnClasspathCondition<A extends Annotation> implements ExecutionCondition {
  private final Class<A> annotationType;
  private final Function<A, String[]> className;

  IfOnClasspathCondition(Class<A> annotationType, Function<A, String[]> className) {
    this.annotationType = annotationType;
    this.className = className;
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<A> annotation = findAnnotation(context.getElement(), annotationType);
    return annotation
        .map(className)
        .map(List::of)
        .filter(not(List::isEmpty))
        .map(this::evaluate)
        .orElseGet(this::enabledByDefault);
  }

  private ConditionEvaluationResult evaluate(List<String> classNames) {
    List<String> notPresent = notPresent(classNames);
    List<String> present = classNames.stream()
        .filter(name -> !notPresent.contains(name))
        .collect(toUnmodifiableList());

    return enabled(present, notPresent)
        ? ConditionEvaluationResult.enabled(format("%s is on classpath", classNames))
        : disabled(format("%s is not on classpath", classNames));
  }

  private List<String> notPresent(List<String> classNames) {
    return classNames.stream()
        .filter(not(String::isBlank))
        .filter(not(Classpath::isPresent))
        .collect(toUnmodifiableList());
  }

  private ConditionEvaluationResult enabledByDefault() {
    return ConditionEvaluationResult.enabled(format("@%s is not present", annotationType.getSimpleName()));
  }

  abstract boolean enabled(List<String> present, List<String> notPresent);
}
