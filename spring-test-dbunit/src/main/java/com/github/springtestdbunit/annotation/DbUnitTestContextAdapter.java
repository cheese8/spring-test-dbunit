/*
 * Copyright 2002-2019 the original author or authors
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

import java.lang.reflect.Method;

import com.github.springtestdbunit.DatabaseConnections;
import com.github.springtestdbunit.DbUnitTestContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.util.ReflectionUtils;

import com.github.springtestdbunit.dataset.DataSetLoader;
import com.github.springtestdbunit.operation.DatabaseOperationLookup;

/**
 * Adapter class to convert Spring's {@link TestContext} to a {@link DbUnitTestContext}. Since Spring 4.0 change the
 * TestContext class from a class to an interface this method uses reflection.
 *
 * @author Phillip Webb
 */
public class DbUnitTestContextAdapter implements DbUnitTestContext {

	private static final Method GET_TEST_CLASS;
	private static final Method GET_TEST_INSTANCE;
	private static final Method GET_TEST_METHOD;
	private static final Method GET_TEST_EXCEPTION;
	private static final Method GET_APPLICATION_CONTEXT;
	private static final Method GET_ATTRIBUTE;
	private static final Method SET_ATTRIBUTE;

	static {
		try {
			GET_TEST_CLASS = TestContext.class.getMethod("getTestClass");
			GET_TEST_INSTANCE = TestContext.class.getMethod("getTestInstance");
			GET_TEST_METHOD = TestContext.class.getMethod("getTestMethod");
			GET_TEST_EXCEPTION = TestContext.class.getMethod("getTestException");
			GET_APPLICATION_CONTEXT = TestContext.class.getMethod("getApplicationContext");
			GET_ATTRIBUTE = TestContext.class.getMethod("getAttribute", String.class);
			SET_ATTRIBUTE = TestContext.class.getMethod("setAttribute", String.class, Object.class);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private TestContext testContext;

	public DbUnitTestContextAdapter(TestContext testContext) {
		this.testContext = testContext;
	}

	public DatabaseConnections getConnections() {
		return (DatabaseConnections) getAttribute(DbUnitTestContextConstants.CONNECTION_ATTRIBUTE);
	}

	public DataSetLoader getDataSetLoader() {
		return (DataSetLoader) getAttribute(DbUnitTestContextConstants.DATA_SET_LOADER_ATTRIBUTE);
	}

	public DatabaseOperationLookup getDatabaseOperationLookup() {
		return (DatabaseOperationLookup) getAttribute(DbUnitTestContextConstants.DATABASE_OPERATION_LOOKUP_ATTRIBUTE);
	}

	public Class<?> getTestClass() {
		return (Class<?>) ReflectionUtils.invokeMethod(GET_TEST_CLASS, testContext);
	}

	public Method getTestMethod() {
		return (Method) ReflectionUtils.invokeMethod(GET_TEST_METHOD, testContext);
	}

	public Object getTestInstance() {
		return ReflectionUtils.invokeMethod(GET_TEST_INSTANCE, testContext);
	}

	public Throwable getTestException() {
		return (Throwable) ReflectionUtils.invokeMethod(GET_TEST_EXCEPTION, testContext);
	}

	public ApplicationContext getApplicationContext() {
		return (ApplicationContext) ReflectionUtils.invokeMethod(GET_APPLICATION_CONTEXT, testContext);
	}

	public Object getAttribute(String name) {
		return ReflectionUtils.invokeMethod(GET_ATTRIBUTE, testContext, name);
	}

	public void setAttribute(String name, Object value) {
		ReflectionUtils.invokeMethod(SET_ATTRIBUTE, testContext, name, value);
	}

}