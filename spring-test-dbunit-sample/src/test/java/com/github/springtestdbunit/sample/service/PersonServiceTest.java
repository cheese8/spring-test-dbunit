package com.github.springtestdbunit.sample.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.github.springtestdbunit.annotation.*;
import com.github.springtestdbunit.dataset.XlsDataSetLoader;
import org.dbunit.assertion.DiffCollectingFailureHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.sample.entity.Person;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@RunWith(SpringJUnit4ClassRunner.class)
@DbUnitConfiguration(failureHandler = DiffCollectingFailureHandler.class)
@ContextConfiguration
@EnableTransactionManagement
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class })
public class PersonServiceTest {

	@Autowired
	private PersonService personService;

	@Autowired
	private BankcardService bankcardService;

	@Test
	@DatabaseSetup("sampleData.xml")
	public void testFind() {
		List<Person> personList = this.personService.find("hil");
		assertEquals(1, personList.size());
		assertEquals("Phillip", personList.get(0).getFirstName());
	}

	@Test
	@DatabaseSetup("sampleData4.xml")
	public void testFind4() {
		List<Person> personList = this.personService.find("hil");
		assertEquals(1, personList.size());
		assertEquals("Phillip", personList.get(0).getFirstName());
	}

	@Test
	@DatabaseSetup(type = DatabaseOperation.SQL, value = {"select * from person", "select id from person"})
	@DatabaseSetup("sampleData.xml")
	public void testFind_0() {
		List<Person> personList = this.personService.find("hil");
		assertEquals(1, personList.size());
		assertEquals("Phillip", personList.get(0).getFirstName());
	}

	@Test
	//@DatabaseSetup(type = DatabaseOperation.SQL, value = {"select * from person", "select id from person"})
	@DatabaseSetup(type = DatabaseOperation.TRUNCATE_TABLE, value = {"person"})
	@DatabaseSetup(type = DatabaseOperation.SQL, value = {"testFind_2.sql"})
	@DatabaseSetup("sampleData.xml")
	public void testFind_2() {
		List<Person> personList = this.personService.find("hil");
		assertEquals(1, personList.size());
		assertEquals("Phillip", personList.get(0).getFirstName());
	}

	@Test
	//@DatabaseSetup(type = DatabaseOperation.SQL, value = {"select * from person", "select id from person"})
	@DatabaseSetup(type = DatabaseOperation.SQL, value = {"testFind_1.sql"})
	@DatabaseSetup("sampleData.xml")
	public void testFind_1() {
		List<Person> personList = this.personService.find("hil");
		assertEquals(1, personList.size());
		assertEquals("Phillip", personList.get(0).getFirstName());
	}


	@Test
	@DatabaseSetup("sampleData.xml")
	@ExpectedDatabase(value = "expectedData.xml", table = "person")
	public void testRemove() {
		this.personService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person")
	@ExpectedDatabase(value = "expectedData.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "bankcard")
	public void testRemoveWithClass() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person")
	@ExpectedDatabase(value = "expectedData.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "bankcard")
	public void testRemove_2() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData1.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData1.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person,bankcard")
	public void testRemove_1() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData3.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData3.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person,bankcard")
	public void testRemove_3() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData3.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData3.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person,bankcard")
	@Exports(value = { @Export(fileName = "testRemoveAndExport.xml", tableName = "person", query = "select * from person", replacements = {"ID", "[ID()]"}),
			@Export(fileName = "testRemoveAndExport.xml", tableName = "bankcard", query = "select * from bankcard", replacements = {"ID", "[ID()]"})})
	public void testRemoveAndExportXMl() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData3.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData3.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person,bankcard")
	@Exports(value = { @Export(fileName = "testRemoveAndExport.xml", tableName = "person", query = "select * from person", format = "yml", replacements = {"ID", "[ID()]"}),
			@Export(fileName = "testRemoveAndExport.xml", tableName = "bankcard", query = "select * from bankcard", format = "yml", replacements = {"ID", "[ID()]"})})
	public void testRemoveAndExportYml() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData3.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData3.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person,bankcard")
	@Exports(value = { @Export(fileName = "testRemoveAndExport.xml", tableName = "person", query = "select * from person", format = "xls", replacements = {"ID", "[ID()]"}),
			@Export(fileName = "testRemoveAndExport.xml", tableName = "bankcard", query = "select * from bankcard", format = "xls", replacements = {"ID", "[ID()]"})})
	public void testRemoveAndExportXls() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData3.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData3.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person,bankcard")
	@Exports(value = { @Export(fileName = "testRemoveAndExport.xml", tableName = "person", query = "select * from person", format = "json", replacements = {"ID", "[ID()]"}),
			@Export(fileName = "testRemoveAndExport.xml", tableName = "bankcard", query = "select * from bankcard", format = "json", replacements = {"ID", "[ID()]"})})
	public void testRemoveAndExportJson() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

	@Test
	@DatabaseSetup(value = "sampleData3.xlsx", dataSetLoader = XlsDataSetLoader.class)
	@ExpectedDatabase(value = "expectedData3.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person,bankcard")
	@Exports(value = { @Export(tableName = "person", query = "select * from person", format = "json"),
			@Export(tableName = "bankcard", query = "select * from bankcard", format = "json")})
	public void testRemoveAndExportJson2() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

}
