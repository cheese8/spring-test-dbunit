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

package com.github.springtestdbunit;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import com.github.springtestdbunit.annotation.*;
import com.github.springtestdbunit.operation.DatabaseOperationLookup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.assertion.FailureHandler;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.filter.IColumnFilter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.github.springtestdbunit.assertion.DatabaseAssertion;
import com.github.springtestdbunit.dataset.DataSetLoader;
import com.github.springtestdbunit.dataset.DataSetModifier;

/**
 * Internal delegate class used to run tests with support for {@link DatabaseSetup &#064;DatabaseSetup},
 * {@link DatabaseTearDown &#064;DatabaseTearDown} and {@link ExpectedDatabase &#064;ExpectedDatabase} annotations.
 *
 * @author Phillip Webb
 * @author Mario Zagar
 * @author Sunitha Rajarathnam
 * @author Oleksii Lomako
 */
public class DbUnitRunner {

	private static final Log logger = LogFactory.getLog(DbUnitTestExecutionListener.class);

	/**
	 * Called before a test method is executed to perform any database setup.
	 * @param testContext The test context
	 * @throws Exception
	 */
	public void beforeTestMethod(DbUnitTestContext testContext) throws Exception {
		Annotations<DatabaseSetup> annotations = Annotations.get(testContext, DatabaseSetups.class,
				DatabaseSetup.class);
		setupOrTeardown(testContext, true, AnnotationAttributes.get(annotations));
	}

	/**
	 * Called after a test method is executed to perform any database teardown and to check expected results.
	 * @param testContext The test context
	 * @throws Exception
	 */
	public void afterTestMethod(DbUnitTestContext testContext) throws Exception {
		try {
			try {
				verifyExpected(testContext,
						Annotations.get(testContext, ExpectedDatabases.class, ExpectedDatabase.class));
			} finally {
				Annotations<DatabaseTearDown> annotations = Annotations.get(testContext, DatabaseTearDowns.class,
						DatabaseTearDown.class);
				try {
					setupOrTeardown(testContext, false, AnnotationAttributes.get(annotations));
				} catch (RuntimeException ex) {
					if (testContext.getTestException() == null) {
						throw ex;
					}
					if (logger.isWarnEnabled()) {
						logger.warn("Unable to throw database cleanup exception due to existing test error", ex);
					}
				}
			}
		} finally {
			testContext.getConnections().closeAll();
		}
	}

	private void verifyExpected(DbUnitTestContext testContext, Annotations<ExpectedDatabase> annotations)
			throws Exception {
		if (testContext.getTestException() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping @DatabaseTest expectation due to test exception "
						+ testContext.getTestException().getClass());
			}
			return;
		}
		DatabaseConnections connections = testContext.getConnections();
		DataSetModifier modifier = getModifier(testContext, annotations);
		boolean override = false;
		for (ExpectedDatabase annotation : annotations.getMethodAnnotations()) {
			verifyExpected(testContext, connections, modifier, annotation);
			override |= annotation.override();
		}
		if (!override) {
			for (ExpectedDatabase annotation : annotations.getClassAnnotations()) {
				verifyExpected(testContext, connections, modifier, annotation);
			}
		}
	}

	private void verifyExpected(DbUnitTestContext testContext, DatabaseConnections connections,
			DataSetModifier modifier, ExpectedDatabase annotation)
					throws Exception {
		String query = annotation.query();
		String table = annotation.table();
		IDataSet expectedDataSet = loadDataset(testContext, annotation.value(), modifier);
		IDatabaseConnection connection = connections.get(annotation.connection());
		FailureHandler failureHandler = getFailureHandler(testContext);
		if (expectedDataSet != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Verifying @DatabaseTest expectation using " + annotation.value());
			}
			DatabaseAssertion assertion = annotation.assertionMode().getDatabaseAssertion();
			List<IColumnFilter> columnFilters = getColumnFilters(testContext, annotation);
			List<String> ignoredColumns = getIgnoredColumns(annotation);
			if (StringUtils.hasLength(query)) {
				Assert.hasLength(table, "The table name must be specified when using a SQL query");
				ITable expectedTable = expectedDataSet.getTable(table);
				ITable actualTable = connection.createQueryTable(table, query);
				assertion.assertEquals(expectedTable, actualTable, columnFilters, ignoredColumns, failureHandler);
			} else if (StringUtils.hasLength(table)) {
				ITable actualTable = connection.createTable(table);
				ITable expectedTable = expectedDataSet.getTable(table);
				assertion.assertEquals(expectedTable, actualTable, columnFilters, ignoredColumns, failureHandler);
			} else {
				IDataSet actualDataSet = connection.createDataSet();
				assertion.assertEquals(expectedDataSet, actualDataSet, columnFilters, ignoredColumns, failureHandler);
			}
		}
	}

	private FailureHandler getFailureHandler(DbUnitTestContext testContext) throws Exception {
		DbUnitConfiguration configuration = testContext.getTestClass().getAnnotation(DbUnitConfiguration.class);
		if (configuration == null) {
			return null;
		}
		Class<? extends FailureHandler> failureHandlerClass = configuration.failureHandler();
		return failureHandlerClass.getDeclaredConstructor().newInstance();
	}

	private DataSetModifier getModifier(DbUnitTestContext testContext, Annotations<ExpectedDatabase> annotations) {
		DataSetModifiers modifiers = new DataSetModifiers();
		for (ExpectedDatabase annotation : annotations) {
			for (Class<? extends DataSetModifier> modifierClass : annotation.modifiers()) {
				modifiers.add(testContext.getTestInstance(), modifierClass);
			}
		}
		return modifiers;
	}

	private void setupOrTeardown(DbUnitTestContext testContext, boolean isSetup, Collection<AnnotationAttributes> annotations) throws Exception {
		DatabaseConnections connections = testContext.getConnections();
		for (AnnotationAttributes annotation : annotations) {
			List<IDataSet> datasets = loadDataSets(testContext, annotation);
			DatabaseOperation operation = annotation.getType();
			org.dbunit.operation.DatabaseOperation dbUnitOperation = getDbUnitDatabaseOperation(testContext, operation);
			if (!datasets.isEmpty()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing " + (isSetup ? "Setup" : "Teardown") + " of @DatabaseTest using "+ operation + " on " + datasets);
				}
				IDatabaseConnection connection = connections.get(annotation.getConnection());
				IDataSet dataSet = new CompositeDataSet(datasets.toArray(new IDataSet[datasets.size()]));
				dbUnitOperation.execute(connection, dataSet);
			}
		}
	}

	private List<IDataSet> loadDataSets(DbUnitTestContext testContext, AnnotationAttributes annotation)
			throws Exception {
		List<IDataSet> datasets = new ArrayList<>();
		for (String dataSetLocation : annotation.getValue()) {
			datasets.add(loadDataset(testContext, dataSetLocation, DataSetModifier.NONE));
		}
		if (datasets.isEmpty()) {
			datasets.add(getFullDatabaseDataSet(testContext, annotation.getConnection()));
		}
		return datasets;
	}

	private IDataSet getFullDatabaseDataSet(DbUnitTestContext testContext, String name) throws Exception {
		IDatabaseConnection connection = testContext.getConnections().get(name);
		return connection.createDataSet();
	}

	private IDataSet loadDataset(DbUnitTestContext testContext, String dataSetLocation, DataSetModifier modifier)
			throws Exception {
		DataSetLoader dataSetLoader = testContext.getDataSetLoader();
		if (StringUtils.hasLength(dataSetLocation)) {
			IDataSet dataSet = dataSetLoader.loadDataSet(testContext.getTestClass(), dataSetLocation);
			dataSet = modifier.modify(dataSet);
			Assert.notNull(dataSet,"Unable to load dataset from \"" + dataSetLocation + "\" using " + dataSetLoader.getClass());
			return dataSet;
		}
		return null;
	}

	private List<IColumnFilter> getColumnFilters(DbUnitTestContext testContext, ExpectedDatabase annotation) throws Exception {
		Class<? extends IColumnFilter>[] fromDbUnitConfiguration = getColumnFiltersFromDbUnitConfiguration(testContext);
		Class<? extends IColumnFilter>[] fromExpectedDatabase = getColumnFiltersFromExpectedDatabase(annotation);
		Class<? extends IColumnFilter>[] columnFilterClasses = mergeDistinct(fromDbUnitConfiguration, fromExpectedDatabase);
		List<IColumnFilter> columnFilters = new LinkedList<>();
		for (Class<? extends IColumnFilter> columnFilterClass : columnFilterClasses) {
			columnFilters.add(columnFilterClass.getDeclaredConstructor().newInstance());
		}
		return columnFilters;
	}

	private Class<? extends IColumnFilter>[] mergeDistinct(Class<? extends IColumnFilter>[] first, Class<? extends IColumnFilter>[] second) {
		Set<Class<? extends IColumnFilter>> result = new HashSet<>();
		result.addAll(Arrays.asList(first));
		result.addAll(Arrays.asList(second));
		return result.toArray(new Class[0]);
	}

	private Class<? extends IColumnFilter>[] getColumnFiltersFromExpectedDatabase(ExpectedDatabase annotation) {
		Class<? extends IColumnFilter>[] columnFilterClasses = annotation.columnFilters();
		if (logger.isDebugEnabled()) {
			logger.debug("Found columnFilters on @ExpectedDatabase configuration");
		}
		return columnFilterClasses;
	}

	private Class<? extends IColumnFilter>[] getColumnFiltersFromDbUnitConfiguration(DbUnitTestContext testContext) {
		Class<? extends IColumnFilter>[] columnFilterClasses = new Class[0];
		DbUnitConfiguration configuration = testContext.getTestClass().getAnnotation(DbUnitConfiguration.class);
		if (configuration != null) {
			columnFilterClasses = configuration.columnFilters();
			if (logger.isDebugEnabled()) {
				logger.debug("Found columnFilters on @DbUnitConfiguration configuration");
			}
		}
		return columnFilterClasses;
	}

	private List<String> getIgnoredColumns(ExpectedDatabase annotation) {
		return Arrays.asList(annotation.ignoreCols());
	}

	private org.dbunit.operation.DatabaseOperation getDbUnitDatabaseOperation(DbUnitTestContext testContext, DatabaseOperation operation) {
		org.dbunit.operation.DatabaseOperation databaseOperation = testContext.getDatabaseOperationLookup().get(operation);
		Assert.state(databaseOperation != null, "The database operation " + operation + " is not supported");
		return databaseOperation;
	}

	private static class AnnotationAttributes {

		private final DatabaseOperation type;

		private final String[] value;

		private final String connection;

		public AnnotationAttributes(Annotation annotation) {
			Assert.state((annotation instanceof DatabaseSetup) || (annotation instanceof DatabaseTearDown),
					"Only DatabaseSetup and DatabaseTearDown annotations are supported");
			Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
			this.type = (DatabaseOperation) attributes.get("type");
			this.value = (String[]) attributes.get("value");
			this.connection = (String) attributes.get("connection");
		}

		public DatabaseOperation getType() {
			return this.type;
		}

		public String[] getValue() {
			return this.value;
		}

		public String getConnection() {
			return this.connection;
		}

		public static <T extends Annotation> Collection<AnnotationAttributes> get(Annotations<T> annotations) {
			List<AnnotationAttributes> annotationAttributes = new ArrayList<>();
			for (T annotation : annotations) {
				annotationAttributes.add(new AnnotationAttributes(annotation));
			}
			return annotationAttributes;
		}

	}

	private static class Annotations<T extends Annotation> implements Iterable<T> {

		private final List<T> classAnnotations;

		private final List<T> methodAnnotations;

		private final List<T> allAnnotations;

		public Annotations(DbUnitTestContext context, Class<? extends Annotation> container, Class<T> annotation) {
			this.classAnnotations = getClassAnnotations(context.getTestClass(), container, annotation);
			this.methodAnnotations = getMethodAnnotations(context.getTestMethod(), container, annotation);
			List<T> allAnnotations = new ArrayList<>(this.classAnnotations.size() + this.methodAnnotations.size());
			allAnnotations.addAll(this.classAnnotations);
			allAnnotations.addAll(this.methodAnnotations);
			this.allAnnotations = Collections.unmodifiableList(allAnnotations);
		}

		private List<T> getClassAnnotations(Class<?> element, Class<? extends Annotation> container,
											Class<T> annotation) {
			List<T> annotations = new ArrayList<>();
			addAnnotationToList(annotations, AnnotationUtils.findAnnotation(element, annotation));
			addRepeatableAnnotationsToList(annotations, AnnotationUtils.findAnnotation(element, container));
			return Collections.unmodifiableList(annotations);
		}

		private List<T> getMethodAnnotations(Method element, Class<? extends Annotation> container,
											 Class<T> annotation) {
			List<T> annotations = new ArrayList<>();
			addAnnotationToList(annotations, AnnotationUtils.findAnnotation(element, annotation));
			addRepeatableAnnotationsToList(annotations, AnnotationUtils.findAnnotation(element, container));
			return Collections.unmodifiableList(annotations);
		}

		private void addAnnotationToList(List<T> annotations, T annotation) {
			if (annotation != null) {
				annotations.add(annotation);
			}
		}

		@SuppressWarnings("unchecked")
		private void addRepeatableAnnotationsToList(List<T> annotations, Annotation container) {
			if (container != null) {
				T[] value = (T[]) AnnotationUtils.getValue(container);
				annotations.addAll(Arrays.asList(value));
			}
		}

		public List<T> getClassAnnotations() {
			return this.classAnnotations;
		}

		public List<T> getMethodAnnotations() {
			return this.methodAnnotations;
		}

		public Iterator<T> iterator() {
			return this.allAnnotations.iterator();
		}

		private static <T extends Annotation> Annotations<T> get(DbUnitTestContext testContext,
				Class<? extends Annotation> container, Class<T> annotation) {
			return new Annotations<>(testContext, container, annotation);
		}

	}

}
