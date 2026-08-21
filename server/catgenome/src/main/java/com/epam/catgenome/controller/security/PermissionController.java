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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Permissions")
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class PermissionController extends AbstractRESTController {

    @Autowired
    private AclPermissionSecurityService permissionApiService;

    @RequestMapping(value = "/grant", method = RequestMethod.POST)
    @Operation(
            summary = "Sets user's  permissions for an object.",
            description = "Sets user's permissions for an object.")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> grantPermissions(@RequestBody PermissionGrantVO grantVO) {
        return Result.success(permissionApiService.setPermissions(grantVO));
    }

    @RequestMapping(value = "/grant", method = RequestMethod.DELETE)
    @Operation(
            summary = "Deletes user's permissions for an object.",
            description = "Deletes user's permissions for an object.")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> deletePermissionsForUser(
            @RequestParam Long id,
            @RequestParam AclClass aclClass, @RequestParam String user,
            @RequestParam(required = false, defaultValue = "true") Boolean isPrincipal) {
        return Result.success(permissionApiService.deletePermissions(id, aclClass, user, isPrincipal));
    }

    @RequestMapping(value = "/grant/all", method = RequestMethod.DELETE)
    @Operation(
            summary = "Deletes all permissions for an object.",
            description = "Deletes all permissions for an object.")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> deleteAllPermissions(@RequestParam Long id,
            @RequestParam AclClass aclClass) {
        return Result.success(permissionApiService.deleteAllPermissions(id, aclClass));
    }

    @RequestMapping(value = "/grant", method = RequestMethod.GET)
    @Operation(
            summary = "Loads all permissions for an object.",
            description = "Loads all permissions for an object.")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> getPermissions(@RequestParam Long id,
                                                  @RequestParam AclClass aclClass) {
        return Result.success(permissionApiService.getPermissions(id, aclClass));
    }

    @RequestMapping(value = "grant/owner", method = RequestMethod.POST)
    @Operation(
            summary = "Change the owner of the particular acl object.",
            description = "Change the owner of the particular acl object.")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<AclSecuredEntry> changeOwner(@RequestParam Long id,
            @RequestParam AclClass aclClass, @RequestParam String userName) {
        return Result.success(permissionApiService.changeOwner(id, aclClass, userName));
    }

    @PostMapping(value = "grant/sync")
    @Operation(
            summary = "Synchronises all existing entities to ACL tables.",
            description = "Might be useful when security is enabled for previously registered data.")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public void syncAclEntities() {
        permissionApiService.syncEntities();
    }
}
