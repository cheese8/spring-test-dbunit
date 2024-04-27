/*
 * Copyright 2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.springtestdbunit.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

public class DatabaseSetupTearDownAnnotationAttributes extends AbstractDatabaseAnnotationAttributes {

	private final DatabaseOperation type;

	private final String[] value;

	public DatabaseSetupTearDownAnnotationAttributes(final Annotation annotation) {

		super(annotation);

		Assert.state((annotation instanceof DatabaseSetup) || (annotation instanceof DatabaseTearDown),
				"Only DatabaseSetup and DatabaseTearDown annotations are supported");

		Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
		type = (DatabaseOperation) attributes.get("type");
		value = (String[]) attributes.get("value");
	}

	public static <T extends Annotation> Collection<DatabaseSetupTearDownAnnotationAttributes> get(
			final Annotations<T> annotations) {

		List<DatabaseSetupTearDownAnnotationAttributes> annotationAttributes = new ArrayList<DatabaseSetupTearDownAnnotationAttributes>();

		for (T annotation : annotations) {
			annotationAttributes.add(new DatabaseSetupTearDownAnnotationAttributes(annotation));
		}

		return annotationAttributes;
	}

	public DatabaseOperation getType() {
		return type;
	}

	public String[] getValue() {
		return value;
	}

}