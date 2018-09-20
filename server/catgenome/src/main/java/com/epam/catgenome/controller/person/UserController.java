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

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.person.Person;
import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.manager.person.PersonManager;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.manager.user.UserApiService;
import com.epam.catgenome.security.UserContext;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/** *
 * {@code UserController} represents an implementation of MVC controller which handles
 * requests to manage browser users.
 * <p>
 * It's designed to communicate with corresponding managers that provide all required
 * calls and manage all operations concerned with users.
 */
@RestController
@Api(value = "user", description = "User Management")
public class UserController extends AbstractRESTController {
    @Autowired
    private PersonManager personManager;

    @Autowired
    private UserApiService userApiService;

    @Autowired
    private AuthManager authManager;

    @PostMapping(value = "/user/register")
    @ApiOperation(
            value = "Registers a user in the system",
            notes = "Registers a user in the system")
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Person> register(@RequestBody final Person person) {
        personManager.savePerson(person);
        return Result.success(person);
    }

    @GetMapping("/user/current")
    @ApiOperation(
        value = "Returns currently logged in user",
        notes = "Returns currently logged in user",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<UserContext> currentUser() {
        return Result.success(authManager.getUserContext());
    }

    @GetMapping("/user/token")
    @ApiOperation(
        value = "Creates a JWT token for current user",
        notes = "Creates a JWT token for current user",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<JwtRawToken> getToken(@RequestParam(required = false) Long expiration) {
        return Result.success(authManager.issueTokenForCurrentUser(expiration));
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
        value = "Loads all registered users.",
        notes = "Loads all registered users.",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Collection<NgbUser>> loadUsers() {
        return Result.success(userApiService.loadAllUsers());
    }
}
