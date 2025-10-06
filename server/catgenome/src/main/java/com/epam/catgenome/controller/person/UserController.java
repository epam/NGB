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

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.manager.user.UserSecurityService;
import com.epam.catgenome.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** *
 * {@code UserController} represents an implementation of MVC controller which handles
 * requests to manage browser users.
 * <p>
 * It's designed to communicate with corresponding managers that provide all required
 * calls and manage all operations concerned with users.
 */
@RestController
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
@Tag(name = "user", description = "User Management")
public class UserController extends AbstractRESTController {

    @Autowired
    private UserSecurityService userSecurityService;

    @GetMapping("/user/current")
    @Operation(
        summary = "Returns currently logged in user",
        description = "Returns currently logged in user")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<UserContext> currentUser() {
        return Result.success(userSecurityService.getUserContext());
    }

    @GetMapping("/user/token")
    @Operation(
        summary = "Creates a JWT token for current user",
        description = "Creates a JWT token for current user")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<JwtRawToken> getToken(@RequestParam(required = false) Long expiration) {
        return Result.success(userSecurityService.issueTokenForCurrentUser(expiration));
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    @Operation(
            summary = "Creates a new user.",
        description = "Creates a new user with specified username and roles.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<NgbUser> createUser(@RequestBody NgbUserVO userVO) {
        return Result.success(userSecurityService.createUser(userVO));
    }

    @RequestMapping(value = "/user/loadList", method = RequestMethod.POST)
    @ResponseBody
    @Operation(
            summary = "Loads users by names.",
        description = "Loads users by names.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Collection<NgbUser>> loadUsersByNames(@RequestBody IDList userList) {
        return Result.success(userSecurityService.loadUsersByNames(userList));
    }


    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    @ResponseBody
    @Operation(
            summary = "Loads a user by a ID.",
        description = "Loads a user by a ID.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<NgbUser> loadUser(@PathVariable Long id) {
        return Result.success(userSecurityService.loadUser(id));
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.PUT)
    @ResponseBody
    @Operation(
            summary = "Updates a user by a ID.",
        description = "Updates a user by a ID.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<NgbUser> updateUser(@PathVariable Long id, @RequestBody NgbUserVO userVO) {
        return Result.success(userSecurityService.updateUser(id, userVO));
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @Operation(
            summary = "Deletes a user by a ID.",
        description = "Deletes a user by a ID.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result deleteUser(@PathVariable Long id) {
        userSecurityService.deleteUser(id);
        return Result.success(null);
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    @ResponseBody
    @Operation(
        summary = "Loads all registered users.",
        description = "Loads all registered users.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Collection<NgbUser>> loadUsers() {
        return Result.success(userSecurityService.loadAllUsers());
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    @ResponseBody
    @Operation(
            summary = "Loads a user by a name.",
        description = "Loads a user by a name.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result loadUserByName(@RequestParam String name) {
        return Result.success(userSecurityService.loadUserByName(name));
    }

    @GetMapping(value = "/group/find")
    @ResponseBody
    @Operation(
            summary = "Finds user group by a prefix (case insensitive).",
        description = "Finds user group by a prefix (case insensitive).")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Collection<String>> findGroups(@RequestParam(required = false) String prefix) {
        return Result.success(userSecurityService.findGroups(prefix));
    }
}
