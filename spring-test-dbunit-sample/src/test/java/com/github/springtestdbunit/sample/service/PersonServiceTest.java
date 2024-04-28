package com.github.springtestdbunit.sample.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.dataset.XlsDataSetLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.sample.entity.Person;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@RunWith(SpringJUnit4ClassRunner.class)
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
	@DatabaseSetup(type = DatabaseOperation.SQL, value = {"select * from person", "select id from person"})
	@DatabaseSetup("sampleData.xml")
	public void testFind_0() {
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
	//@DatabaseSetup(type = DatabaseOperation.SQL, value = {"select * from person", "select id from person"})
	@DatabaseSetup(type = DatabaseOperation.SQL, value = {"testFind_2.sql"})
	@DatabaseSetup("sampleData.xml")
	public void testFind_2() {
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
	@ExpectedDatabase(value = "expectedData1.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "person")
	@ExpectedDatabase(value = "expectedData1.xlsx", dataSetLoader = XlsDataSetLoader.class, table = "bankcard")
	public void testRemove_1() throws Exception {
		personService.remove(1);
		bankcardService.remove(1);
	}

}
