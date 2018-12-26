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

import com.epam.catgenome.controller.vo.IDList;
import com.epam.catgenome.controller.vo.NgbUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.manager.user.UserSecurityService;
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
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
@Api(value = "user", description = "User Management")
public class UserController extends AbstractRESTController {

    @Autowired
    private UserSecurityService userSecurityService;

    @GetMapping("/user/current")
    @ApiOperation(
        value = "Returns currently logged in user",
        notes = "Returns currently logged in user",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<UserContext> currentUser() {
        return Result.success(userSecurityService.getUserContext());
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
        return Result.success(userSecurityService.issueTokenForCurrentUser(expiration));
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Creates a new user.",
            notes = "Creates a new user with specified username and roles.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<NgbUser> createUser(@RequestBody NgbUserVO userVO) {
        return Result.success(userSecurityService.createUser(userVO));
    }

    @RequestMapping(value = "/user/loadList", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Loads users by names.",
            notes = "Loads users by names.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Collection<NgbUser>> loadUsersByNames(@RequestBody IDList userList) {
        return Result.success(userSecurityService.loadUsersByNames(userList));
    }


    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Loads a user by a ID.",
            notes = "Loads a user by a ID.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<NgbUser> loadUser(@PathVariable Long id) {
        return Result.success(userSecurityService.loadUser(id));
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(
            value = "Updates a user by a ID.",
            notes = "Updates a user by a ID.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<NgbUser> updateUser(@PathVariable Long id, @RequestBody NgbUserVO userVO) {
        return Result.success(userSecurityService.updateUser(id, userVO));
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(
            value = "Deletes a user by a ID.",
            notes = "Deletes a user by a ID.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result deleteUser(@PathVariable Long id) {
        userSecurityService.deleteUser(id);
        return Result.success(null);
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
        return Result.success(userSecurityService.loadAllUsers());
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Loads a user by a name.",
            notes = "Loads a user by a name.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result loadUserByName(@RequestParam String name) {
        return Result.success(userSecurityService.loadUserByName(name));
    }
}
