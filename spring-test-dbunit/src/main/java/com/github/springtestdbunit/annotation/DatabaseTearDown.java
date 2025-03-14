/*
 * Copyright 2002-2023 the original author or authors
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.dataset.DataSetLoader;
import org.dbunit.dataset.IDataSet;

/**
 * Test annotation which indicates how to put a database into a know state after tests have run. This annotation can be
 * placed on a class or on methods. When placed on a class the setup is applied after each test methods is executed.
 *
 * @see DatabaseSetup
 * @see ExpectedDatabase
 * @see DbUnitConfiguration
 * @see DbUnitTestExecutionListener
 *
 * @author Phillip Webb
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Repeatable(DatabaseTearDowns.class)
public @interface DatabaseTearDown {

	/**
	 * The name of the connection that should be used. Can refer to a connection specified in
	 * {@link DbUnitConfiguration @DbUnitConfiguration} or left blank to use the default connection.
	 * @return the connection
	 */
	String connection() default "";

	/**
	 * Determines the type of {@link DatabaseOperation operation} that will be used to reset the database.
	 * @return The type of operation used to reset the database
	 */
	DatabaseOperation type() default DatabaseOperation.CLEAN_INSERT;

	/**
	 * Provides the locations of the datasets that will be used to reset the database. If no locations are specified the
	 * full database dataset is used.
	 * @return The dataset locations
	 * @see DbUnitConfiguration#dataSetLoader()
	 */
	String[] value() default {};

	/**
	 * Returns the class that will be used to load {@link IDataSet} resources. The specified class must implement
	 * {@link DataSetLoader} and must have a default constructor. If not provided, the one defined on the
	 * {@link DbUnitConfiguration} will be used instead.
	 *
	 * Leave blank to use the default dataset loader. If both the {@code dataSetLoader} and {@code dataSetLoaderBean}
	 * are defined, the latter will be used.
	 *
	 * @return The data set loader class.
	 */
	Class<? extends DataSetLoader> dataSetLoader() default DataSetLoader.class;

	/**
	 * Returns the name of the bean that will be used to load {@link IDataSet} resources. The specified bean must
	 * implement {@link DataSetLoader}. If not provided, the one defined on the {@link DbUnitConfiguration} will be used
	 * instead.
	 *
	 * Leave blank to use the default dataset loader. If both the {@code dataSetLoader} and {@code dataSetLoaderBean}
	 * are defined, the latter will be used.
	 *
	 * @return The data set loader bean name.
	 */
	String dataSetLoaderBean() default "";

	String datasetId() default "";

}
