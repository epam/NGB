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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.person.Person;
import com.epam.catgenome.manager.person.PersonManager;
import com.epam.catgenome.security.AuthManager;
import com.epam.catgenome.security.UserContext;
import com.wordnik.swagger.annotations.ApiOperation;

/** *
 * {@code PersonController} represents an implementation of MVC controller which handles
 * requests to manage browser users.
 * <p>
 * It's designed to communicate with corresponding managers that provide all required
 * calls and manage all operations concerned with users.
 */
@RestController
public class PersonController {
    @Autowired
    private PersonManager personManager;

    @Autowired
    private AuthManager authManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonController.class);

    @PostMapping(value = "/user/register")
    @ApiOperation(
            value = "Registers a user in the system",
            notes = "Registers a user in the system")
    public Result<Person> register(@RequestBody final Person person) {
        personManager.savePerson(person);
        return Result.success(person);
    }

    @GetMapping("/user/current")
    public Result<UserContext> currentUser() {
        return Result.success(authManager.getUserContext());
    }
}
