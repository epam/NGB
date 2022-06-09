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

package com.epam.catgenome.controller.security;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.security.PermissionGrantVO;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.entity.security.AclSecuredEntry;
import com.epam.catgenome.security.acl.AclPermissionSecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(value = "Permissions")
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class PermissionController extends AbstractRESTController {

    @Autowired
    private AclPermissionSecurityService permissionApiService;

    @RequestMapping(value = "/grant", method = RequestMethod.POST)
    @ApiOperation(
            value = "Sets user's  permissions for an object.",
            notes = "Sets user's permissions for an object.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> grantPermissions(@RequestBody PermissionGrantVO grantVO) {
        return Result.success(permissionApiService.setPermissions(grantVO));
    }

    @RequestMapping(value = "/grant", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Deletes user's permissions for an object.",
            notes = "Deletes user's permissions for an object.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> deletePermissionsForUser(
            @RequestParam Long id,
            @RequestParam AclClass aclClass, @RequestParam String user,
            @RequestParam(required = false, defaultValue = "true") Boolean isPrincipal) {
        return Result.success(permissionApiService.deletePermissions(id, aclClass, user, isPrincipal));
    }

    @RequestMapping(value = "/grant/all", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Deletes all permissions for an object.",
            notes = "Deletes all permissions for an object.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> deleteAllPermissions(@RequestParam Long id,
            @RequestParam AclClass aclClass) {
        return Result.success(permissionApiService.deleteAllPermissions(id, aclClass));
    }

    @RequestMapping(value = "/grant", method = RequestMethod.GET)
    @ApiOperation(
            value = "Loads all permissions for an object.",
            notes = "Loads all permissions for an object.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> getPermissions(@RequestParam Long id,
                                                  @RequestParam AclClass aclClass) {
        return Result.success(permissionApiService.getPermissions(id, aclClass));
    }

    @RequestMapping(value = "grant/owner", method = RequestMethod.POST)
    @ApiOperation(
            value = "Change the owner of the particular acl object.",
            notes = "Change the owner of the particular acl object.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> changeOwner(@RequestParam Long id,
            @RequestParam AclClass aclClass, @RequestParam String userName) {
        return Result.success(permissionApiService.changeOwner(id, aclClass, userName));
    }

    @PostMapping(value = "grant/sync")
    @ApiOperation(
            value = "Synchronises all existing entities to ACL tables.",
            notes = "Might be useful when security is enabled for previously registered data.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void syncAclEntities() {
        permissionApiService.syncEntities();
    }

    @PostMapping(value = "grant/cache")
    @ApiOperation(
            value = "Performs cache warm up",
            notes = "Performs cache warm up.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void fillAclCache() {
        permissionApiService.fillCache();
    }
}
