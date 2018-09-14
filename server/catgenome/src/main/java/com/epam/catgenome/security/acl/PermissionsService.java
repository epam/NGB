package com.epam.catgenome.security.acl;

import java.util.List;

import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Permission;
import org.springframework.util.Assert;

import com.epam.catgenome.entity.security.AbstractSecuredEntity;

public class PermissionsService {

    private static final List<AclPermission> BASIC_PERMISSIONS = AclPermission.getBasicPermissions();

    public List<AclPermission> getBasicPermissions() {
        return BASIC_PERMISSIONS;
    }

    public void validateMask(Integer mask) {
        for (AclPermission permission : AclPermission.getBasicPermissions()) {
            Assert.isTrue(!bothBitsSet(mask, permission),
                    "Granting and denying permissions cannot be set together.");
        }
    }

    public int unsetBits(int mask, int... bits) {
        int unsetMask = mask;
        for (int bit : bits) {
            unsetMask = unsetMask & ~bit;
        }
        return unsetMask;
    }

    public boolean isPermissionSet(int mask, AclPermission permission) {
        int grantingMask = permission.getMask();
        int denyingMask = permission.getDenyPermission().getMask();
        if (isMaskBitSet(mask, grantingMask) || isMaskBitSet(mask, denyingMask)) {
            return true;
        }
        return false;
    }

    public boolean allPermissionsSet(int mask, List<AclPermission> permissionToCollect) {
        for (AclPermission permission : permissionToCollect) {
            if (!isPermissionSet(mask, permission)) {
                return false;
            }
        }
        return true;
    }

    public Integer mergeMask(int extendedMask) {
        return mergeMask(extendedMask, getBasicPermissions());
    }

    public Integer mergeMask(int extendedMask, List<AclPermission> basicPermissions) {
        int result = 0;
        for (AclPermission p : basicPermissions) {
            int grantingMask = p.getMask();
            int denyingMask = p.getDenyPermission().getMask();
            if (isMaskBitSet(extendedMask, grantingMask) && !isMaskBitSet(extendedMask,
                    denyingMask)) {
                result = result | p.getSimpleMask();
            }
        }
        if (isMaskBitSet(extendedMask, AclPermission.OWNER.getMask())) {
            AclPermission ownerPermission = (AclPermission)AclPermission.OWNER;
            result = result | ownerPermission.getSimpleMask();
        }
        return result;
    }

    public boolean permissionIsNotDenied(AccessControlEntry ace, Permission cumulativePermission,
                                         Permission p) {
        return !containsOppositeMask(cumulativePermission, p) && ace.isGranting();
    }

    public boolean permissionIsNotDenied(int cumulativeMask,
                                         Permission p) {
        return !containsOppositeMask(cumulativeMask, p);
    }

    public boolean containsPermission(Permission cumulativePermission, Permission singlePermission) {
        return maskIncludes(cumulativePermission, singlePermission) ||
                containsOppositeMask(cumulativePermission, singlePermission);
    }

    public boolean containsPermission(int mask, Permission singlePermission) {
        return maskIncludes(mask, singlePermission) ||
                containsOppositeMask(mask, singlePermission);
    }

    private boolean containsOppositeMask(Permission cumulativePermission,
                                         Permission singlePermission) {
        return containsOppositeMask(cumulativePermission.getMask(), singlePermission);
    }

    private boolean containsOppositeMask(int mask,
                                         Permission singlePermission) {
        if (singlePermission instanceof AclPermission) {
            AclPermission aclPermission = (AclPermission) singlePermission;
            if (aclPermission.isGranting()) {
                Permission opposite = aclPermission.getDenyPermission();
                return maskIncludes(mask, opposite);
            }
        }
        return false;
    }

    private boolean maskIncludes(int mask, Permission singlePermission) {
        return isMaskBitSet(mask, singlePermission.getMask());
    }

    private boolean maskIncludes(Permission cumulativePermission, Permission singlePermission) {
        return maskIncludes(cumulativePermission.getMask(), singlePermission);
    }

    public boolean isMaskBitSet(int extendedMask, int permissionMask) {
        return (extendedMask & permissionMask) == permissionMask;
    }

    private boolean isMaskBitSet(int extendedMask, AclPermission permission) {
        return isMaskBitSet(extendedMask, permission.getMask());
    }

    private boolean bothBitsSet(Integer mask, AclPermission permission) {
        return isMaskBitSet(mask, permission) && isMaskBitSet(mask, permission.getDenyPermission());
    }

    public int mergeParentMask(int childMask, int parentMask) {
        if (childMask == AbstractSecuredEntity.ALL_PERMISSIONS_MASK ||
                childMask == AbstractSecuredEntity.ALL_PERMISSIONS_MASK_FULL) {
            return childMask;
        }
        int resultMask = 0;
        for (AclPermission permission : AclPermission.getBasicPermissions()) {
            // at first try child permission, then if none of the bits is set - parent mask
            if (maskIncludes(childMask, permission)) {
                resultMask = resultMask | permission.getMask();
            } else if (maskIncludes(childMask, permission.getDenyPermission())) {
                resultMask = resultMask | permission.getDenyPermission().getMask();
            } else if (maskIncludes(parentMask, permission)) {
                resultMask = resultMask | permission.getMask();
            } else if (maskIncludes(parentMask, permission.getDenyPermission())) {
                resultMask = resultMask | permission.getDenyPermission().getMask();
            }
        }
        return resultMask;
    }

    public boolean isPermissionGranted(int mask, Permission permission) {
        return isMaskBitSet(mask, permission.getMask());
    }
}
