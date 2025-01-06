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

package com.github.springtestdbunit.dataset;

import static org.junit.Assert.*;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestContext;

import com.github.springtestdbunit.testutils.ExtendedTestContextManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link FlatXmlDataSetLoader}.
 *
 * @author Phillip Webb
 */
public class FlatXmlDataSetLoaderTest {

	private TestContext testContext;

	private FlatXmlDataSetLoader loader;

	@Before
	public void setup() throws Exception {
		this.loader = new FlatXmlDataSetLoader();
		ExtendedTestContextManager manager = new ExtendedTestContextManager(getClass());
		this.testContext = manager.accessTestContext();
	}

	@Test
	public void shouldSenseColumnsWithClassRelative() throws Exception {
		IDataSet dataset = this.loader.loadDataSet(this.testContext.getTestClass(), "test-column-sensing.xml", null);
		assertDataset(dataset);
	}

	@Test
	public void shouldSenseColumnsWithClassPath() throws Exception {
		IDataSet dataset = this.loader.loadDataSet(this.testContext.getTestClass(), "test-column-sensing-classpath.xml", null);
		assertDataset(dataset);
	}

	private void assertDataset(IDataSet dataset) throws DataSetException {
		assertNull(dataset.getTable("Sample").getValue(0, "name"));
		assertEquals("test", dataset.getTable("Sample").getValue(1, "name"));
	}

	@Test
	public void shouldLoadFromRelativeFile() throws Exception {
		IDataSet dataset = this.loader.loadDataSet(this.testContext.getTestClass(), "test.xml", null);
		assertEquals("Sample", dataset.getTableNames()[0]);
	}

	@Test
	public void shouldReturnNullOnMissingFile() throws Exception {
		IDataSet dataset = this.loader.loadDataSet(this.testContext.getTestClass(), "doesnotexist.xml", null);
		assertNull(dataset);
	}

	@Test
	public void testBuildDataSetFromStream() throws Exception {
		buildDataSetFromStream("test-column-sensing-classpath.xml");
	}

	@Test
	public void testBuildDataSetFromStreamWithClasspath() throws Exception {
		buildDataSetFromStream("classpath:/test-column-sensing-classpath.xml");
	}

	private void buildDataSetFromStream(String location) throws DataSetException {
		FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
		builder.setColumnSensing(true);
		IDataSet dataset = ReflectionTestUtils.invokeMethod(this.loader, "buildDataSetFromStream", new Object[]{builder, getClasspathResource(location), null});
		assertDataset(dataset);
	}

	private Resource getClasspathResource(String location) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		String classpathLocation = location.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX) ? location :
				ResourceLoader.CLASSPATH_URL_PREFIX + location;
		return resourceLoader.getResource(classpathLocation);
	}
}
