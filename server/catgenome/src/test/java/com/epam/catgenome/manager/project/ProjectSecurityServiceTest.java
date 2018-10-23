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

package com.epam.catgenome.manager.project;

import com.epam.catgenome.common.AbstractACLSecurityTest;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.security.acl.AclPermission;
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
import java.util.Arrays;
import java.util.Collections;

public class ProjectSecurityServiceTest extends AbstractACLSecurityTest {

    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private static final String TEST_BAM_NAME = "//agnX1.09-28.trim.dm606.realign.bam";
    private static final String TEST_USER = "TEST_USER";
    private static final String TEST_USER2 = "TEST_USER2";
    private static final String TEST_USER_NO_READ = "TEST_USER3";

    private Project project;

    @Autowired
    private NGBRegistrationUtils registrationUtils;

    @Autowired
    private AclTestDao aclTestDao;

    @Autowired
    private ProjectSecurityService projectSecurityService;

    @Before
    public void setup() throws IOException {

        Reference reference = registrationUtils.registerReference(TEST_REF_NAME, TEST_REF_NAME, TEST_USER);
        BamFile bam1 = registrationUtils.registerBam(reference, TEST_BAM_NAME, "bam1", TEST_USER);
        BamFile bam2 = registrationUtils.registerBam(reference, TEST_BAM_NAME, "bam2", TEST_USER);
        BamFile bam3 = registrationUtils.registerBam(reference, TEST_BAM_NAME, "bam3", TEST_USER);

        project = registrationUtils
                .registerProject("data_set1", TEST_USER, null, Arrays.asList(bam1, bam2));
        Project nested = registrationUtils
                .registerProject("data_set2", TEST_USER, project.getId(), Collections.singletonList(bam3));


        AclTestDao.AclSid ownerSid = new AclTestDao.AclSid(true, TEST_USER);
        ownerSid.setId(1L);
        aclTestDao.createAclSid(ownerSid);

        AclTestDao.AclSid testUserSid = new AclTestDao.AclSid(true, TEST_USER2);
        testUserSid.setId(2L);
        aclTestDao.createAclSid(testUserSid);

        AclTestDao.AclSid testUserDeniedSid = new AclTestDao.AclSid(true, TEST_USER_NO_READ);
        testUserDeniedSid.setId(3L);
        aclTestDao.createAclSid(testUserDeniedSid);

        AclTestDao.AclClass bamAclClass = new AclTestDao.AclClass(BamFile.class.getCanonicalName());
        bamAclClass.setId(1L);
        aclTestDao.createAclClassIfNotPresent(bamAclClass);

        AclTestDao.AclClass projectAclClass = new AclTestDao.AclClass(Project.class.getCanonicalName());
        projectAclClass.setId(2L);
        aclTestDao.createAclClassIfNotPresent(projectAclClass);

        AclTestDao.AclObjectIdentity bam1refIdentity = new AclTestDao.AclObjectIdentity(ownerSid, bam1.getId(),
                bamAclClass.getId(), null, true);
        bam1refIdentity.setId(1L);
        aclTestDao.createObjectIdentity(bam1refIdentity);

        AclTestDao.AclObjectIdentity bam2refIdentity = new AclTestDao.AclObjectIdentity(ownerSid, bam2.getId(),
                bamAclClass.getId(), null, true);
        bam2refIdentity.setId(2L);
        aclTestDao.createObjectIdentity(bam2refIdentity);

        AclTestDao.AclObjectIdentity bam3refIdentity = new AclTestDao.AclObjectIdentity(ownerSid, bam3.getId(),
                bamAclClass.getId(), null, true);
        bam3refIdentity.setId(3L);
        aclTestDao.createObjectIdentity(bam3refIdentity);

        AclTestDao.AclEntry bam1AclEntry = new AclTestDao.AclEntry(bam1refIdentity, 1, testUserSid,
                AclPermission.NO_READ.getMask(), false);
        bam1AclEntry.setId(1L);
        aclTestDao.createAclEntry(bam1AclEntry);

        AclTestDao.AclEntry bam2AclEntry = new AclTestDao.AclEntry(bam2refIdentity, 1, testUserSid,
                AclPermission.WRITE.getMask(), true);
        bam2AclEntry.setId(2L);
        aclTestDao.createAclEntry(bam2AclEntry);

        AclTestDao.AclObjectIdentity projectrefIdentity = new AclTestDao.AclObjectIdentity(ownerSid, project.getId(),
                projectAclClass.getId(), null, true);
        projectrefIdentity.setId(4L);
        aclTestDao.createObjectIdentity(projectrefIdentity);

        AclTestDao.AclEntry projectAclEntry = new AclTestDao.AclEntry(projectrefIdentity, 1, testUserSid,
                AclPermission.READ.getMask(), true);
        projectAclEntry.setId(4L);
        aclTestDao.createAclEntry(projectAclEntry);

        AclTestDao.AclObjectIdentity  nestedProRefIdentity = new AclTestDao.AclObjectIdentity(ownerSid, nested.getId(),
                projectAclClass.getId(), null, true);
        nestedProRefIdentity.setId(5L);
        aclTestDao.createObjectIdentity(nestedProRefIdentity);

        projectAclEntry = new AclTestDao.AclEntry(nestedProRefIdentity, 1, testUserSid,
                AclPermission.NO_READ.getMask(), true);
        projectAclEntry.setId(5L);
        aclTestDao.createAclEntry(projectAclEntry);

    }

    @Test
    @WithMockUser(TEST_USER2)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadProjectTest() {
        Project loaded = projectSecurityService.load(project.getId());
        Assert.assertNotNull(loaded);
        Assert.assertNotNull(loaded.getLeaves());
        Assert.assertEquals(1, loaded.getLeaves().size());
        Assert.assertNotNull(loaded.getChildren());
        Assert.assertEquals(0, loaded.getChildren().size());

    }

    @Test(expected = AccessDeniedException.class)
    @WithMockUser(TEST_USER_NO_READ)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadProjectDeniedTest() {
        projectSecurityService.load(project.getId());
    }

}