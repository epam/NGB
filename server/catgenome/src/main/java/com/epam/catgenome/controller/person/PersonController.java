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

package com.epam.catgenome.controller.person;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.person.Person;
import com.epam.catgenome.manager.person.PersonManager;
import com.epam.catgenome.security.BrowserUser;
import com.wordnik.swagger.annotations.ApiOperation;

/** *
 * {@code PersonController} represents an implementation of MVC controller which handles
 * requests to manage browser users.
 * <p>
 * It's designed to communicate with corresponding managers that provide all required
 * calls and manage all operations concerned with users.
 */
@Controller
public class PersonController {
    @Autowired
    private PersonManager personManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonController.class);

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Registers a user in the system",
            notes = "Registers a user in the system")
    public Result<Person> register(@RequestBody final Person person) {
        personManager.savePerson(person);
        return Result.success(person);
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Checks the correctness of provided user and password",
            notes = "Checks the correctness of provided user and password")
    public Result<Person> login(@RequestParam String name, @RequestParam String password) {
        return Result.success(personManager.loadPersonByNameAndPassword(name, password));
    }

    @PreAuthorize("hasAnyRole('ROLE_APP','ROLE_ADMIN')")
    @RequestMapping(value = "/secure/user/current", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns information on a user by a specified ID",
            notes = "Returns information on a user by a specified ID")
    public Result<Person> getUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        BrowserUser user = (BrowserUser) auth.getPrincipal();
        LOGGER.info(String.format("User %d : %s", user.getPerson().getId(), user.getPerson().getName()));
        return Result.success(personManager.loadPersonById(user.getPerson().getId()));
    }

    @PreAuthorize("hasAnyRole('ROLE_APP','ROLE_ADMIN')")
    @RequestMapping(value = "/secure/user/update", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Updates information on an already registered user",
            notes = "Updates information on an already registered user")
    public Result<Person> updateUser(@RequestBody final Person person) {
        personManager.savePerson(person);
        return Result.success(person);
    }
}
