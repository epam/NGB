/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

package com.epam.catgenome.security.acl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.acls.domain.AbstractPermission;
import org.springframework.security.acls.model.Permission;

import lombok.Getter;
import lombok.Setter;

/**
 * {@link AclPermission} represents a set of supported permissions for ACL security layer.
 * {@link AclPermission} is used to check whether user has a required permission to access an
 * entity and is also used to calculate and return
 * a permission mask to the client.
 * Basically we support three basic permissions: READ, WRITE, EXECUTE. For each basic permission
 * there is also a corresponding denying permission: NO_READ, NO_WRITE, NO_EXECUTE. Since permissions
 * maybe inherited from entity's parent, denying
 * permissions are used to prohibit permission inheriting for a basic permission, e.g. if NO_READ
 * permission is set for a entity, user won't have READ access to it, even if he has READ access to
 * it's parent.
 * Supplementary OWNER permission gives full access to object with all basic permissions granted.
 * We support two permissions mask notations:
 * -    full 7-bit mask containing basic and denying permissions bits is stored in DB
 *      and used for granting/denying access to objects
 * -    simple 4-bit mask containing only READ/WRITE/EXECUTE/OWNER bits is returned to client.
 */
@Getter
@Setter
public class AclPermission extends AbstractPermission {

    public static final String WRITE_PERMISSION = "WRITE";

    public static final Permission READ = new AclPermission(1, 'R', true); //1
    public static final Permission NO_READ = new AclPermission(1 << 1, 'Q', false); //2

    public static final Permission WRITE = new AclPermission(1 << 2, 'W', true); //4
    public static final Permission NO_WRITE = new AclPermission(1 << 3, 'S', false); //8

    public static final Permission OWNER = new AclPermission(1 << 6, 'O', true); //16

    public static final Permission ALL_DENYING_PERMISSIONS = new AclPermission(NO_READ.getMask() | NO_WRITE.getMask());

    public static final Map<Permission, Permission> DENYING_PERMISSIONS = new HashMap<>();
    static {
        DENYING_PERMISSIONS.put(READ, NO_READ);
        DENYING_PERMISSIONS.put(WRITE, NO_WRITE);
    }

    public static final Map<AclPermission, Integer> SIMPLE_MASKS = new HashMap<>();
    static {
        SIMPLE_MASKS.put((AclPermission)READ, 1);
        SIMPLE_MASKS.put((AclPermission)WRITE, 1 << 1);
        SIMPLE_MASKS.put((AclPermission)OWNER, 1 << 3);
    }

    public static final Map<String, AclPermission> NAME_PERMISSION_MAP = new HashMap<>();
    static {
        NAME_PERMISSION_MAP.put("READ", (AclPermission)READ);
        NAME_PERMISSION_MAP.put("WRITE", (AclPermission)WRITE);
        NAME_PERMISSION_MAP.put("OWNER", (AclPermission)OWNER);
    }

    private boolean granting = true;

    public AclPermission(int mask) {
        super(mask);
    }

    public AclPermission(int mask, char code) {
        super(mask, code);
    }

    public AclPermission(int mask, char code, boolean granting) {
        super(mask, code);
        this.granting = granting;
    }

    public AclPermission getDenyPermission() {
        Permission permission = DENYING_PERMISSIONS.get(this);
        return permission == null ? null : (AclPermission)permission;
    }

    public int getSimpleMask() {
        return SIMPLE_MASKS.get(this);
    }

    public static List<AclPermission> getBasicPermissions() {
        return Arrays.asList((AclPermission)READ, (AclPermission)WRITE);
    }

    public static AclPermission getAclPermissionByName(String permissionName) {
        return NAME_PERMISSION_MAP.get(permissionName);
    }
}
