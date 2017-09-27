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

package com.epam.catgenome.dao.person;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.person.Person;
import com.epam.catgenome.entity.person.PersonRole;

/**
 * {@code PersonDao} is a DAO component, that handles database interaction with MAF file metadata.
 */
public class PersonDao extends NamedParameterJdbcDaoSupport {
    private String personSequenceName;
    private String insertPersonQuery;
    private String updatePersonQuery;
    private String loadPersonByIdQuery;
    private String loadPersonByNameAndPasswordQuery;
    private String loadPersonByNameQuery;

    @Autowired
    private DaoHelper daoHelper;

    /**
     * Saves or updates a {@code Person} instance in the database
     * @param person to save or update, if person's ID is specified it assumed to be
     *               already stored in the database; if ID isn't specified a new record will
     *               be created in the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void savePerson(final Person person) {
        if (person.getId() == null) {
            person.setId(daoHelper.createId(personSequenceName));
            getNamedParameterJdbcTemplate().update(insertPersonQuery, PersonParameters.getParameters(person));
        } else {
            getNamedParameterJdbcTemplate().update(updatePersonQuery, PersonParameters.getParameters(person));
        }
    }

    /**
     * Loads a {@code Person} instance from the database specified by it's ID
     * @param personId of a user
     * @return a loaded {@code Person} instance or
     *          {@code null} if user with a given ID doesn't exist
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Person loadPersonById(long personId) {
        List<Person> persons = getJdbcTemplate().query(loadPersonByIdQuery, PersonParameters.getRowMapper(), personId);
        return persons.isEmpty() ? null : persons.get(0);
    }

    /**
     * Loads a {@code Person} instance from the database specified by it's name and password
     * @param name of a user
     * @param password of a user
     * @return  a loaded {@code Person} instance or
     *          {@code null} if user with a given name and password doesn't exist
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Person loadPersonByNameAndPassword(String name, String password) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PersonParameters.NAME.name(), name);
        params.addValue(PersonParameters.PASSWORD.name(), password);

        List<Person> persons = getNamedParameterJdbcTemplate().query(loadPersonByNameAndPasswordQuery, params,
                PersonParameters.getRowMapper());
        return persons.isEmpty() ? null : persons.get(0);
    }

    /**
     * Loads a {@code Person} instance from the database specified by it's name
     * @param name of a user
     * @return  a loaded {@code Person} instance or
     *          {@code null} if user with a given name doesn't exist
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Person loadPersonByName(String name) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PersonParameters.NAME.name(), name);

        List<Person> persons = getNamedParameterJdbcTemplate().query(loadPersonByNameQuery, params,
                PersonParameters.getPasswordRowMapper());
        return persons.isEmpty() ? null : persons.get(0);
    }

    enum PersonParameters {
        PERSON_ID,
        NAME,
        PASSWORD,
        EMAIL,
        ROLE_ID;

        static MapSqlParameterSource getParameters(Person person) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(PERSON_ID.name(), person.getId());
            params.addValue(NAME.name(), person.getName());
            params.addValue(PASSWORD.name(), person.getPasswordHash());
            params.addValue(EMAIL.name(), person.getEmail());
            params.addValue(ROLE_ID.name(), person.getRole().getRoleId());

            return params;
        }

        static RowMapper<Person> getRowMapper() {
            return (rs, rowNum) -> {
                Person person = new Person();

                person.setId(rs.getLong(PERSON_ID.name()));
                person.setName(rs.getString(NAME.name()));
                person.setEmail(rs.getString(EMAIL.name()));

                long longVal = rs.getLong(ROLE_ID.name());
                if (!rs.wasNull()) {
                    person.setRole(PersonRole.getRoleById(longVal));
                }

                return person;
            };
        }

        static RowMapper<Person> getPasswordRowMapper() {
            return (rs, rowNum) -> {
                Person person = new Person();

                person.setId(rs.getLong(PERSON_ID.name()));
                person.setName(rs.getString(NAME.name()));
                person.setEmail(rs.getString(EMAIL.name()));
                person.setPasswordHash(rs.getString(PASSWORD.name()));

                long longVal = rs.getLong(ROLE_ID.name());
                if (!rs.wasNull()) {
                    person.setRole(PersonRole.getRoleById(longVal));
                }

                return person;
            };
        }
    }

    @Required
    public void setPersonSequenceName(String personSequenceName) {
        this.personSequenceName = personSequenceName;
    }

    @Required
    public void setInsertPersonQuery(String insertPersonQuery) {
        this.insertPersonQuery = insertPersonQuery;
    }

    @Required
    public void setLoadPersonByIdQuery(String loadPersonByIdQuery) {
        this.loadPersonByIdQuery = loadPersonByIdQuery;
    }

    @Required
    public void setLoadPersonByNameAndPasswordQuery(String loadPersonByNameAndPasswordQuery) {
        this.loadPersonByNameAndPasswordQuery = loadPersonByNameAndPasswordQuery;
    }

    @Required
    public void setLoadPersonByNameQuery(String loadPersonByNameQuery) {
        this.loadPersonByNameQuery = loadPersonByNameQuery;
    }

    @Required
    public void setUpdatePersonQuery(String updatePersonQuery) {
        this.updatePersonQuery = updatePersonQuery;
    }
}
