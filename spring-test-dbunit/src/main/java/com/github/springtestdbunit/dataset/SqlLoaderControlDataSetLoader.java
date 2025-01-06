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
 *
 */
package com.github.springtestdbunit.dataset;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.sqlloader.SqlLoaderControlDataSet;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

/**
 * A {@link DataSetLoader data set loader} that can be used to load {@link SqlLoaderControlDataSet}s.
 *
 * @author Paul Podgorsek
 */
public class SqlLoaderControlDataSetLoader extends AbstractDataSetLoader {

	private static final String ORDERED_TABLE_FILE = "tables.lst";

	@Override
	protected IDataSet createDataSet(final Resource resource, String datasetId) throws IOException, DataSetException {

		File ctlDir = resource.getFile();
		String ctlDirPath = ctlDir.getAbsolutePath();

		if (!ctlDirPath.endsWith("/")) {
			ctlDirPath = ctlDirPath + "/";
		}

		File orderedTablesFile = new File(ctlDirPath + ORDERED_TABLE_FILE);

		return new SqlLoaderControlDataSet(ctlDir, orderedTablesFile);
	}

}
