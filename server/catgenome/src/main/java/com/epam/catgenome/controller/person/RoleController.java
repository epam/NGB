/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.controller.person;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.RoleVO;
import com.epam.catgenome.entity.user.ExtendedRole;
import com.epam.catgenome.entity.user.Role;
import com.epam.catgenome.manager.user.RoleSecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;


/**
 * {@code UserController} represents an implementation of MVC controller which handles
 * requests to manage browser users.
 * <p>
 * It's designed to communicate with corresponding managers that provide all required
 * calls and manage all operations concerned with users.
 */
@RestController
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
@Api(value = "Role", description = "Role Management")
public class RoleController extends AbstractRESTController {

    @Autowired
    private RoleSecurityService roleSecurityService;

    @RequestMapping(value = "/role/loadAll", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Loads all available roles.",
            notes = "Loads all available roles. Parameter <b>loadUsers</b> specifies whether"
                    + "list of associated users should be returned with roles.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Collection<Role>> loadRoles(
            @RequestParam(required = false, defaultValue = "false") boolean loadUsers) {
        return Result.success(loadUsers ? roleSecurityService.loadRolesWithUsers() : roleSecurityService.loadRoles());
    }

    @RequestMapping(value = "/role/{id}/assign", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Assigns a list of users to role.",
            notes = "Assigns a list of users to role",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ExtendedRole> assignRole(@PathVariable Long id,
                                           @RequestParam List<Long> userIds) {
        return Result.success(roleSecurityService.assignRole(id, userIds));
    }

    @RequestMapping(value = "/role/{id}/remove", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(
            value = "Removes a role from a list of users",
            notes = "Removes a role from a list of users",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ExtendedRole> removeRole(@PathVariable Long id,
                                           @RequestParam List<Long> userIds) {
        return Result.success(roleSecurityService.removeRole(id, userIds));
    }

    @RequestMapping(value = "/role/create", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Creates a new role.",
            notes = "Creates a new role with specified name. Name should not be empty. All roles"
                    + "are supposed to start with 'ROLE_' prefix, if it is not provided, prefix will"
                    + "be added automatically.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Role> createRole(@RequestParam String roleName,
                                   @RequestParam(required = false, defaultValue = "false") boolean userDefault) {
        return Result.success(roleSecurityService.createRole(roleName, userDefault));
    }

    @RequestMapping(value = "/role/{id}", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(
            value = "Updates a role specified by ID.",
            notes = "Updates a role specified by ID.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Role> updateRole(@PathVariable Long id, @RequestBody RoleVO roleVO) {
        return Result.success(roleSecurityService.updateRole(id, roleVO));
    }

    @RequestMapping(value = "/role/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Gets a role specified by ID.",
            notes = "Gets a role specified by ID.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Role> getRole(@PathVariable Long id) {
        return Result.success(roleSecurityService.loadRole(id));
    }

    @RequestMapping(value = "/role/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(
            value = "Deletes a role specified by ID.",
            notes = "Deletes a role specified by ID along with all permissions set",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Role> deleteRole(@PathVariable Long id) {
        return Result.success(roleSecurityService.deleteRole(id));
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Finds a role specified by name.",
            notes = "Finds a role specified by name.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Role> loadRoleByName(@RequestParam String name) {
        return Result.success(roleSecurityService.loadRoleByName(name));
    }

}
