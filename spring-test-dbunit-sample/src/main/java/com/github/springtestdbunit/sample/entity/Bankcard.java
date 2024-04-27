/*
 * Copyright 2002-2016 the original author or authors
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

package com.github.springtestdbunit.sample.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
@Entity
@NamedQueries({ @NamedQuery(name = "Bankcard.find", query = "SELECT p from Bankcard p where p.id = :id") })
public class Bankcard {

	@Id
	private int id;

	private String bankcardName;

	private Integer bankcardNo;

	private int personId;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getBankcardName() {
		return bankcardName;
	}

	public void setBankcardName(String bankcardName) {
		this.bankcardName = bankcardName;
	}

	public Integer getBankcardNo() {
		return bankcardNo;
	}

	public void setBankcardNo(Integer bankcardNo) {
		this.bankcardNo = bankcardNo;
	}

	public int getPersonId() {
		return personId;
	}

	public void setPersonId(int personId) {
		this.personId = personId;
	}
}
