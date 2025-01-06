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
import java.util.Map;

import org.dbunit.dataset.IDataSet;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

import com.github.springtestdbunit.dataset.DataSetLoader;

public abstract class AbstractDatabaseAnnotationAttributes {

	private final String connection;

	/**
	 * The class that will be used to load {@link IDataSet} resources. The specified class must implement
	 * {@link DataSetLoader} and must have a default constructor. If not provided, the one defined on the
	 * {@link DbUnitConfiguration} will be used instead.
	 */
	private final Class<? extends DataSetLoader> dataSetLoader;

	/**
	 * The name of the bean that will be used to load {@link IDataSet} resources. The specified bean must implement
	 * {@link DataSetLoader}. If not provided, the one defined on the {@link DbUnitConfiguration} will be used instead.
	 */
	private final String dataSetLoaderBean;
	
	private final String datasetId;

	@SuppressWarnings("unchecked")
	public AbstractDatabaseAnnotationAttributes(final Annotation annotation) {

		Assert.state(
				(annotation instanceof DatabaseSetup) || (annotation instanceof DatabaseTearDown)
						|| (annotation instanceof ExpectedDatabase),
				"Only DatabaseSetup, DatabaseTearDown and ExpectedDatabase annotations are supported");

		Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
		connection = (String) attributes.get("connection");
		dataSetLoader = (Class<? extends DataSetLoader>) attributes.get("dataSetLoader");
		dataSetLoaderBean = (String) attributes.get("dataSetLoaderBean");
		datasetId = (String) attributes.get("datasetId");
	}

	public String getConnection() {
		return connection;
	}

	public Class<? extends DataSetLoader> getDataSetLoader() {
		return dataSetLoader;
	}

	public String getDataSetLoaderBean() {
		return dataSetLoaderBean;
	}

	public String getDatasetId() {
		return this.datasetId;
	}
}