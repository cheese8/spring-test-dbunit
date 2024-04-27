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

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.springframework.core.Conventions;

/**
 * Constants related to the DBUnit test context.
 *
 * @author Paul Podgorsek
 */
public final class DbUnitTestContextConstants {

	public static final String CONNECTION_ATTRIBUTE = Conventions
			.getQualifiedAttributeName(DbUnitTestExecutionListener.class, "connection");

	public static final String DATA_SET_LOADER_ATTRIBUTE = Conventions
			.getQualifiedAttributeName(DbUnitTestExecutionListener.class, "dataSetLoader");

	public static final String DATABASE_OPERATION_LOOKUP_ATTRIBUTE = Conventions
			.getQualifiedAttributeName(DbUnitTestExecutionListener.class, "databseOperationLookup");

	/**
	 * Default private constructor to avoid instantiating this class.
	 */
	private DbUnitTestContextConstants() {
		super();
	}

}