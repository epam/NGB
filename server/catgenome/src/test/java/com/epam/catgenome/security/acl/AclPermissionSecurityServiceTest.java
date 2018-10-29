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

package com.epam.catgenome.security.acl;

import com.epam.catgenome.common.AbstractACLSecurityTest;
import com.epam.catgenome.controller.vo.security.PermissionGrantVO;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.manager.user.UserManager;
import com.epam.catgenome.util.AclTestDao;
import com.epam.catgenome.util.NGBRegistrationUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class AclPermissionSecurityServiceTest extends AbstractACLSecurityTest {

    private static final String TEST_USER = "TEST_ADMIN";
    private static final String TEST_USER_2 = "TEST_USER";
    private static final String TEST_REF_NAME = "//dm606.X.fa";

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private NGBRegistrationUtils registrationUtils;

    @Autowired
    private AclTestDao aclTestDao;

    @Autowired
    private AclPermissionSecurityService securityService;

    @Autowired
    private PermissionHelper permissionHelper;

    @Autowired
    private UserManager userManager;

    private Reference reference;
    private AclTestDao.AclObjectIdentity refIdentity;
    private AclTestDao.AclSid userSid;

    @Before
    public void setup() throws IOException {
        userManager.createUser(TEST_USER, new ArrayList<>(Arrays.asList(1L, 3L)),
                new ArrayList<>(), Collections.emptyMap());
        userManager.createUser(TEST_USER_2, new ArrayList<>(Arrays.asList(1L, 3L)),
                new ArrayList<>(), Collections.emptyMap());
        reference = registrationUtils.registerReference(TEST_REF_NAME,
                TEST_REF_NAME + biologicalDataItemDao.createBioItemId(), TEST_USER);

        AclTestDao.AclSid ownerSid = new AclTestDao.AclSid(true, TEST_USER);
        ownerSid.setId(1L);
        aclTestDao.createAclSid(ownerSid);

        userSid = new AclTestDao.AclSid(true, TEST_USER_2);
        userSid.setId(2L);
        aclTestDao.createAclSid(userSid);

        AclTestDao.AclClass registryAclClass = new AclTestDao.AclClass(Reference.class.getCanonicalName());
        registryAclClass.setId(1L);
        aclTestDao.createAclClassIfNotPresent(registryAclClass);

        refIdentity = new AclTestDao.AclObjectIdentity(ownerSid, reference.getId(),
                registryAclClass.getId(), null, true);
        refIdentity.setId(1L);
        aclTestDao.createObjectIdentity(refIdentity);
    }

    @Test
    @WithMockUser(value = TEST_USER)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void setPermissionsTest() {
        PermissionGrantVO grantVO = new PermissionGrantVO();
        grantVO.setAclClass(AclClass.REFERENCE);
        grantVO.setId(reference.getId());
        grantVO.setUserName(TEST_USER_2);
        grantVO.setPrincipal(true);
        grantVO.setMask(1);
        securityService.setPermissions(grantVO);

        Integer mask = permissionHelper.retrieveMaskForSid(reference, true, true,
                permissionHelper.convertUserToSids(TEST_USER_2));

        Assert.assertEquals(1, mask.intValue());
    }

    @Test(expected = AccessDeniedException.class)
    @WithMockUser(value = TEST_USER_2)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void setPermissionsWhenNotPermittedTest() {
        PermissionGrantVO grantVO = new PermissionGrantVO();
        grantVO.setAclClass(AclClass.REFERENCE);
        grantVO.setId(reference.getId());
        grantVO.setUserName(TEST_USER_2);
        grantVO.setPrincipal(true);
        grantVO.setMask(1);
        securityService.setPermissions(grantVO);
    }

    @Test
    @WithMockUser(value = TEST_USER)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void deletePermissionsTest() {
        PermissionGrantVO grantVO = new PermissionGrantVO();
        grantVO.setAclClass(AclClass.REFERENCE);
        grantVO.setId(reference.getId());
        grantVO.setUserName(TEST_USER_2);
        grantVO.setPrincipal(true);
        grantVO.setMask(1);
        securityService.setPermissions(grantVO);

        Integer mask = permissionHelper.retrieveMaskForSid(reference, true, true,
                permissionHelper.convertUserToSids(TEST_USER_2));

        Assert.assertEquals(1, mask.intValue());

        securityService.deletePermissions(reference.getId(), AclClass.REFERENCE, TEST_USER_2, true);

        mask = permissionHelper.retrieveMaskForSid(reference, true, true,
                permissionHelper.convertUserToSids(TEST_USER_2));

        Assert.assertEquals(0, mask.intValue());
    }

}