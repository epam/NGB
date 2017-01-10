/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.dao;

import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.person.PersonDao;
import com.epam.catgenome.entity.person.Person;
import com.epam.catgenome.entity.person.PersonRole;

/**
 * Source:
 * Created:     10/12/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 *
 * @author Semen_Dmitriev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class PersonDaoTest extends AbstractDaoTest {

    private static final String TEST_NAME = "your mom";
    private static final String TEST_EMAIL = "blah@epam.com";
    private static final String TEST_PASSWORD = "6f1ed002ab5595859014ebf0951522d9";
    private static final String OTHER_TEST_PASSWORD = "6f1ed002ab5595859014235";
    @Autowired
    private PersonDao personDao;

    @Override
    public void setup() throws Exception {
        assertNotNull("GeneFileDao isn't provided.", personDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void addPerson() {
        Person person = new Person();
        person.setName(TEST_NAME);
        person.setEmail(TEST_EMAIL);
        person.setPasswordHash(TEST_PASSWORD);
        person.setRole(PersonRole.ROLE_APP);
        personDao.savePerson(person);

        Person loadPerson = personDao.loadPersonByNameAndPassword(TEST_NAME, TEST_PASSWORD);
        validPerson(loadPerson);
        Assert.assertNull(loadPerson.getPasswordHash());

        loadPerson = personDao.loadPersonByName(TEST_NAME);
        validPerson(loadPerson);
        Assert.assertTrue(loadPerson.getPasswordHash().equals(TEST_PASSWORD));

        loadPerson = personDao.loadPersonById(loadPerson.getId());
        validPerson(loadPerson);
        Assert.assertNull(loadPerson.getPasswordHash());

        loadPerson.setPasswordHash(OTHER_TEST_PASSWORD);
        personDao.savePerson(loadPerson);

        loadPerson = personDao.loadPersonByName(TEST_NAME);
        validPerson(loadPerson);
        Assert.assertTrue(loadPerson.getPasswordHash().equals(OTHER_TEST_PASSWORD));

    }

    private void validPerson(Person loadPerson){
        Assert.assertTrue(loadPerson != null);
        Assert.assertTrue(loadPerson.getEmail().equals(TEST_EMAIL));
        Assert.assertTrue(loadPerson.getName().equals(TEST_NAME));
        Assert.assertTrue(loadPerson.getRole().equals(PersonRole.ROLE_APP));
    }
}
