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

package com.epam.catgenome.manager.person;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.dao.person.PersonDao;
import com.epam.catgenome.entity.person.Person;
import com.epam.catgenome.entity.person.PersonRole;
import com.epam.catgenome.security.BrowserUser;

/**
 * Provides service for handling {@code Person}: supports creating, updating and loading users data
 */
@Service
public class PersonManager {
    @Autowired
    private PersonDao personDao;

    /**
     * Saves or updates a {@code Person} on the database. User updating operation is allowed only
     * for a {@code ROLE_ADMIN} user.
     * @param person to save or update
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void savePerson(final Person person) {
        if (person.getRole() == null || person.getId() == null) {
            person.setRole(PersonRole.ROLE_APP);
        }

        if (person.getId() != null) {
            checkUpdatePermission(person);
        }
        personDao.savePerson(person);
        person.setPasswordHash(null);
    }

    private void checkUpdatePermission(Person person) {
        if (SecurityContextHolder.getContext() == null) {
            throw new UnauthorizedClientException("Unauthorized");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        BrowserUser user = (BrowserUser) auth.getPrincipal();

        if ((!person.getId().equals(user.getPerson().getId()) || person.getRole().equals(PersonRole.ROLE_ADMIN))
                && !user.getAuthorities().contains(new SimpleGrantedAuthority(PersonRole.ROLE_ADMIN.name()))) {
            throw new UnauthorizedClientException("Only admin can do this");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Person loadPersonByNameAndPassword(String name, String password) {
        Person person = personDao.loadPersonByNameAndPassword(name, password);
        Assert.notNull(person, "Unauthorized: Incorrect user name or password");
        return person;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Person loadPersonById(long personId) {
        return personDao.loadPersonById(personId);
    }
}
