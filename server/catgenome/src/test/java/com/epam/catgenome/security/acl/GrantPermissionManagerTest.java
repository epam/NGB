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
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.util.AclTestDao;
import com.epam.catgenome.util.NGBRegistrationUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
public class GrantPermissionManagerTest extends AbstractACLSecurityTest {

    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private static final String TEST_BAM_NAME = "//agnX1.09-28.trim.dm606.realign.bam";
    private static final String TEST_USER = "TEST_USER";
    private static final String TEST_USER2 = "TEST_USER2";

    @Autowired
    private NGBRegistrationUtils registrationUtils;

    @Autowired
    private AclTestDao aclTestDao;

    @Autowired
    private GrantPermissionManager permissionManager;

    private Reference reference;
    BamFile bam2;
    BamFile bam1;


    @Before
    @WithMockUser(TEST_USER)
    public void setUp() throws Exception {
        reference = registrationUtils.registerReference(TEST_REF_NAME, TEST_REF_NAME);
        bam1 = registrationUtils.registerBam(reference, TEST_BAM_NAME, "bam1", TEST_USER);

        AclTestDao.AclSid ownerSid = new AclTestDao.AclSid(true, TEST_USER);
        ownerSid.setId(1L);
        aclTestDao.createAclSid(ownerSid);

        AclTestDao.AclSid testUserSid = new AclTestDao.AclSid(true, TEST_USER2);
        testUserSid.setId(2L);
        aclTestDao.createAclSid(testUserSid);

        AclTestDao.AclClass registryAclClass = new AclTestDao.AclClass(BamFile.class.getCanonicalName());
        registryAclClass.setId(1L);
        aclTestDao.createAclClassIfNotPresent(registryAclClass);

        AclTestDao.AclObjectIdentity refIdentity = new AclTestDao.AclObjectIdentity(ownerSid, bam1.getId(),
                registryAclClass.getId(), null, true);
        refIdentity.setId(1L);
        aclTestDao.createObjectIdentity(refIdentity);

        AclTestDao.AclEntry refAclEntry = new AclTestDao.AclEntry(refIdentity, 1, testUserSid,
                AclPermission.NO_READ.getMask(), false);
        refAclEntry.setId(1L);
        aclTestDao.createAclEntry(refAclEntry);

        bam2 = registrationUtils.registerBam(reference, TEST_BAM_NAME, "bam2", TEST_USER);

        refIdentity = new AclTestDao.AclObjectIdentity(ownerSid, bam2.getId(),
                registryAclClass.getId(), null, true);
        refIdentity.setId(2L);
        aclTestDao.createObjectIdentity(refIdentity);

        refAclEntry = new AclTestDao.AclEntry(refIdentity, 1, testUserSid,
                AclPermission.READ.getMask(), true);
        refAclEntry.setId(2L);
        aclTestDao.createAclEntry(refAclEntry);

    }

    @Test
    @WithMockUser(TEST_USER2)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void filterListTest() {

        List<AbstractSecuredEntity> registered = new ArrayList<>();
        registered.add(bam1);
        registered.add(bam2);

        List<? extends AbstractSecuredEntity> filtered = permissionManager.filterList(registered, AclPermission.READ);
        Assert.assertNotNull(filtered);
        Assert.assertEquals(1, filtered.size());

    }

}