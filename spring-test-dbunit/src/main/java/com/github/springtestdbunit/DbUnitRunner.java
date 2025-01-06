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

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.github.springtestdbunit.annotation.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.assertion.FailureHandler;
import org.dbunit.database.*;
import org.dbunit.dataset.*;
import org.dbunit.dataset.csv.CsvDataSetWriter;
import org.dbunit.dataset.excel.XlsDataSet;
import org.dbunit.dataset.filter.IColumnFilter;
import org.dbunit.dataset.json.JsonDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.yaml.YamlDataSet;
import org.dbunit.operation.ExecuteSqlOperation;
import org.dbunit.operation.TruncateTableOperation;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.github.springtestdbunit.assertion.DatabaseAssertion;
import com.github.springtestdbunit.dataset.DataSetLoader;
import com.github.springtestdbunit.dataset.DataSetModifier;

import javax.sql.DataSource;

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
	 * @throws Exception exception
	 */
	public void beforeTestMethod(DbUnitTestContext testContext) throws Exception {
		Annotations<DatabaseSetup> annotations = Annotations.get(testContext, DatabaseSetups.class, DatabaseSetup.class);
		setupOrTeardown(testContext, true, DatabaseSetupTearDownAnnotationAttributes.get(annotations));
	}

	/**
	 * Called after a test method is executed to perform any database teardown and to check expected results.
	 * @param testContext The test context
	 * @throws Exception exception
	 */
	public void afterTestMethod(DbUnitTestContext testContext) throws Exception {
		try {
			try {
				verifyExpected(testContext, Annotations.get(testContext, ExpectedDatabases.class, ExpectedDatabase.class));
				doExport(testContext, Annotations.get(testContext, Exports.class, Export.class));
			} finally {
				Annotations<DatabaseTearDown> annotations = Annotations.get(testContext, DatabaseTearDowns.class, DatabaseTearDown.class);
				try {
					setupOrTeardown(testContext, false, DatabaseSetupTearDownAnnotationAttributes.get(annotations));
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

	private void doExport(DbUnitTestContext testContext, Annotations<Export> annotations) throws Exception {
		if (testContext.getTestException() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping @Export expectation due to test exception " + testContext.getTestException().getClass());
			}
			return;
		}
		String testClassPackage = "src/test/resources/" + testContext.getTestClass().getPackage().getName().replaceAll("\\.", "/");
		DatabaseConnections connections = testContext.getConnections();
		IDatabaseConnection connection = null;
		String fileName = "", format = "";
		List<Pair<String, String>> tableNameAndSql = new ArrayList<>();
		List<Export> list =  annotations.getMethodAnnotations();
		if (CollectionUtils.isEmpty(list)) return;
		boolean xmlElement = false;
		boolean sortColumn = false;
		String[] replacements = new String[]{};
		for (Export annotation : list) {
			connection = connections.get(annotation.connection());
			fileName = StringUtils.isEmpty(annotation.fileName()) ? testContext.getTestMethod().getName() : annotation.fileName();
			format = annotation.format();
			xmlElement = annotation.xmlElement();
			sortColumn = annotation.sortColumn();
			replacements = annotation.replacements();
			tableNameAndSql.add(Pair.of(annotation.tableName(), annotation.query()));
		}
		export(connection, testClassPackage + "/" + fileName, tableNameAndSql, format, xmlElement, sortColumn, replacements);
	}

	private void verifyExpected(DbUnitTestContext testContext, Annotations<ExpectedDatabase> annotations) throws Exception {
		if (testContext.getTestException() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping @DatabaseTest expectation due to test exception " + testContext.getTestException().getClass());
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
		//IDataSet expectedDataSet = loadDataset(testContext, annotation.value(), modifier);
		IDataSet expectedDataSet = loadDataset(testContext, new ExpectedDatabaseAnnotationAttributes(annotation),
				annotation.value(), modifier);
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
				String[] tableArray = table.split(",", -1);
				for (String each : tableArray) {
					ITable actualTable = connection.createTable(each);
					ITable expectedTable = expectedDataSet.getTable(each);
					assertion.assertEquals(expectedTable, actualTable, columnFilters, ignoredColumns, failureHandler);
				}
			} else {
				IDataSet actualDataSet = connection.createDataSet();
				assertion.assertEquals(expectedDataSet, actualDataSet, columnFilters, ignoredColumns, failureHandler);
			}
		}
	}

	public static void export(IDatabaseConnection connection, String fileName, List<Pair<String, String>> tableNameAndSql, String format, boolean xmlElement, boolean sortColumn, String[] replacements) throws DataSetException, IOException {
		connection.getConfig().setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, true);
		DatabaseConfig config = connection.getConfig();
		config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, new CachedResultSetTableFactory());
		if (CollectionUtils.isEmpty(tableNameAndSql)) return;
		QueryDataSet queryDataSet = new QueryDataSet(connection);
		Collections.sort(tableNameAndSql, Comparator.comparing(Pair::getLeft));
		for(Pair<String, String> each : tableNameAndSql) {
			queryDataSet.addTable(each.getLeft(), each.getRight());
		}
		if ("csv".equalsIgnoreCase(format)) {
			CsvDataSetWriter.write(queryDataSet, new File(fileName + "." + format));
		} else if ("xml".equalsIgnoreCase(format)){
			FlatXmlDataSet.write(queryDataSet, FileUtils.openOutputStream(new File(fileName + "." + format)), xmlElement, sortColumn, replacements);
		} else if ("json".equalsIgnoreCase(format)) {
			JsonDataSet.write(queryDataSet, FileUtils.openOutputStream(new File(fileName + "." + format)), sortColumn, replacements);
		} else if ("xls".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
			XlsDataSet.write(queryDataSet, FileUtils.openOutputStream(new File(fileName+ "." + format)), sortColumn, replacements);
		} else if ("yml".equalsIgnoreCase(format) || "yaml".equalsIgnoreCase(format)) {
			YamlDataSet.write(queryDataSet, FileUtils.openOutputStream(new File(fileName+ "." + format)), sortColumn, replacements);
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

	private void setupOrTeardown(DbUnitTestContext testContext, boolean isSetup, Collection<DatabaseSetupTearDownAnnotationAttributes> annotations)
			throws Exception {
		DatabaseConnections connections = testContext.getConnections();
		for (DatabaseSetupTearDownAnnotationAttributes annotation : annotations) {
			DatabaseOperation operation = annotation.getType();
			org.dbunit.operation.DatabaseOperation dbUnitOperation = getDbUnitDatabaseOperation(testContext, operation);
			IDatabaseConnection connection = connections.get(annotation.getConnection());
			if (dbUnitOperation instanceof ExecuteSqlOperation) {
				for (String each : annotation.getValue()) {
					Resource resource = getClassRelativeResource(testContext.getTestClass(), each);
					if (resource.exists()) {
						dbUnitOperation.execute(connection, resource.getFile());
						continue;
					}
					resource = getClasspathResource(each);
					if (resource.exists()) {
						dbUnitOperation.execute(connection, resource.getFile());
						continue;
					}
					dbUnitOperation.execute(connection, each);
				}
				continue;
			}
			if (dbUnitOperation instanceof TruncateTableOperation) {
				for (String each : annotation.getValue()) {
					Resource resource1 = getClassRelativeResource(testContext.getTestClass(), each);
					Resource resource2 = getClasspathResource(each);
					if (!resource1.exists() && !resource2.exists()) {
						dbUnitOperation.execute(connection, each);
					} else {
						List<IDataSet> datasets = loadDataSets(testContext, annotation);
						if (!datasets.isEmpty()) {
							if (logger.isDebugEnabled()) {
								logger.debug("Executing " + (isSetup ? "Setup" : "Teardown") + " of @DatabaseTest using "+ operation + " on " + datasets);
							}
							IDataSet dataSet = new CompositeDataSet(datasets.toArray(new IDataSet[datasets.size()]));
							dbUnitOperation.execute(connection, dataSet);
						}
					}
				}
				continue;
			}
			List<IDataSet> datasets = loadDataSets(testContext, annotation);
			if (!datasets.isEmpty()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing " + (isSetup ? "Setup" : "Teardown") + " of @DatabaseTest using "+ operation + " on " + datasets);
				}
				IDataSet dataSet = new CompositeDataSet(datasets.toArray(new IDataSet[datasets.size()]));
				dbUnitOperation.execute(connection, dataSet);
			}
		}
	}

	private Resource getClassRelativeResource(Class<?> testClass, String location) {
		ResourceLoader resourceLoader = getResourceLoader(testClass);
		return resourceLoader.getResource(location);
	}

	private ResourceLoader getResourceLoader(Class<?> testClass) {
		return new ClassRelativeResourceLoader(testClass);
	}

	private Resource getClasspathResource(String location) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		String classpathLocation = location.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX) ? location :
				ResourceLoader.CLASSPATH_URL_PREFIX + location;
		return resourceLoader.getResource(classpathLocation);
	}

	//private List<IDataSet> loadDataSets(DbUnitTestContext testContext, AnnotationAttributes annotation)
	//		throws Exception {
	private List<IDataSet> loadDataSets(DbUnitTestContext testContext,
				DatabaseSetupTearDownAnnotationAttributes annotation) throws Exception {
		List<IDataSet> datasets = new ArrayList<>();
		for (String dataSetLocation : annotation.getValue()) {
			//datasets.add(loadDataset(testContext, dataSetLocation, DataSetModifier.NONE));
			datasets.add(loadDataset(testContext, annotation, dataSetLocation, DataSetModifier.NONE));
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

	//private IDataSet loadDataset(DbUnitTestContext testContext, String dataSetLocation, DataSetModifier modifier)
	//		throws Exception {
		/**
		 * Loads a dataset using the configuration defined in the test annotation and the global test context.
		 *
		 * @param testContext The test context.
		 * @param annotation The annotation which is currently being processed for the test.
		 * @param dataSetLocation The location of the dataset.
		 * @param modifier The dataset modifier.
		 * @return The loaded dataset.
		 * @throws DataSetException An exception thrown if the dataset itself has a problem.
		 * @throws IOException An exception thrown if the dataset could not be loaded.
		 */
	private IDataSet loadDataset(final DbUnitTestContext testContext, final AbstractDatabaseAnnotationAttributes annotation,
								 final String dataSetLocation, final DataSetModifier modifier) throws Exception {
			DataSetLoader dataSetLoader = DataSetAnnotationUtils.getDataSetLoader(testContext, annotation);
		//DataSetLoader dataSetLoader = testContext.getDataSetLoader();
		if (StringUtils.hasLength(dataSetLocation)) {
			IDataSet dataSet = dataSetLoader.loadDataSet(testContext.getTestClass(), dataSetLocation, annotation.getDatasetId());
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
}
