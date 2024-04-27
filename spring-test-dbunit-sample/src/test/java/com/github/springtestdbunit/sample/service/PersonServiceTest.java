package com.github.springtestdbunit.sample.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class })
public class PersonServiceTest {

	@Autowired
	private PersonService personService;

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
	@ExpectedDatabase("expectedData.xml")
	public void testRemove() {
		this.personService.remove(1);
	}

}
