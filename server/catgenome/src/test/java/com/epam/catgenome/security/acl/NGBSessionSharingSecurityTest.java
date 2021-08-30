/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2021 EPAM Systems
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
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.session.NGBSessionDao;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.session.NGBSession;
import com.epam.catgenome.manager.session.NGBSessionSecurityService;
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
import java.util.List;

public class NGBSessionSharingSecurityTest extends AbstractACLSecurityTest {

    private static final String TEST_USER = "TEST_ADMIN";
    private static final String TEST_USER_2 = "TEST_USER";
    private static final String TEST_REF_NAME = "//dm606.X.fa";
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
    public static final String READ = "READ";
    public static final long END = 10000L;
    public static final long START = 0L;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private NGBRegistrationUtils registrationUtils;

    @Autowired
    private AclTestDao aclTestDao;

    @Autowired
    private UserManager userManager;

    @Autowired
    private NGBSessionSecurityService sessionSecurityService;

    @Autowired
    private NGBSessionDao sessionDao;

    private Reference reference;
    private AclTestDao.AclObjectIdentity refIdentity;
    private BamFile bamFile;
    private AclTestDao.AclObjectIdentity bamIdentity;
    private Project project;
    private AclTestDao.AclObjectIdentity projectIdentity;
    private NGBSession ngbSession;
    private AclTestDao.AclObjectIdentity sessionIdentity;
    private AclTestDao.AclSid userSid;


    @Before
    public void setup() throws IOException {
        userManager.createUser(TEST_USER, new ArrayList<>(Arrays.asList(1L, 3L)),
                new ArrayList<>(), Collections.emptyMap());
        userManager.createUser(TEST_USER_2, new ArrayList<>(Arrays.asList(1L, 3L)),
                new ArrayList<>(), Collections.emptyMap());
        reference = registrationUtils.registerReference(TEST_REF_NAME,
                TEST_REF_NAME + biologicalDataItemDao.createBioItemId(), TEST_USER);

        bamFile = registrationUtils.registerBam(reference,
                "//agnX1.09-28.trim.dm606.realign.bam", "bamfile", TEST_USER);

        project = registrationUtils.registerProject("data", TEST_USER, null,
                reference, Collections.singletonList(bamFile));

        AclTestDao.AclSid ownerSid = new AclTestDao.AclSid(true, TEST_USER);
        ownerSid.setId(1L);
        aclTestDao.createAclSid(ownerSid);

        userSid = new AclTestDao.AclSid(true, TEST_USER_2);
        userSid.setId(2L);
        aclTestDao.createAclSid(userSid);

        AclTestDao.AclClass refAclClass = new AclTestDao.AclClass(Reference.class.getCanonicalName());
        refAclClass.setId(1L);
        aclTestDao.createAclClassIfNotPresent(refAclClass);

        refIdentity = new AclTestDao.AclObjectIdentity(ownerSid, reference.getId(),
                refAclClass.getId(), null, true);
        refIdentity.setId(1L);
        aclTestDao.createObjectIdentity(refIdentity);

        AclTestDao.AclClass bamAclClass = new AclTestDao.AclClass(BamFile.class.getCanonicalName());
        bamAclClass.setId(2L);
        aclTestDao.createAclClassIfNotPresent(bamAclClass);

        bamIdentity = new AclTestDao.AclObjectIdentity(ownerSid, bamFile.getId(),
                bamAclClass.getId(), null, true);
        bamIdentity.setId(2L);
        aclTestDao.createObjectIdentity(bamIdentity);

        AclTestDao.AclClass projectAclClass = new AclTestDao.AclClass(Project.class.getCanonicalName());
        projectAclClass.setId(3L);
        aclTestDao.createAclClassIfNotPresent(projectAclClass);

        projectIdentity = new AclTestDao.AclObjectIdentity(ownerSid, project.getId(),
                projectAclClass.getId(), null, true);
        projectIdentity.setId(3L);
        aclTestDao.createObjectIdentity(projectIdentity);

        ngbSession = new NGBSession();
        ngbSession.setName("testSession");
        ngbSession.setChromosome("X");
        ngbSession.setStart(START);
        ngbSession.setEnd(END);
        ngbSession.setReferenceId(reference.getId());
        ngbSession.setSessionValue("{" +
                "\"name\":\"testSession\"," +
                "\"tracks\":[" +
                "   {\"bioDataItemId\":\"" + reference.getName() + "\",\"height\":100,\"projectId\":\""
                + project.getName() + "\",\"format\":\"REFERENCE\"}, " +
                "   {\"bioDataItemId\":\"" + bamFile.getName() + "\",\"height\":100,\"projectId\":\""
                + project.getName() + "\",\"format\":\"BAM\"}" +
                "]}"
        );
        sessionDao.create(ngbSession);

        AclTestDao.AclClass sessionAclClass = new AclTestDao.AclClass(NGBSession.class.getCanonicalName());
        sessionAclClass.setId(4L);
        aclTestDao.createAclClassIfNotPresent(sessionAclClass);

        sessionIdentity = new AclTestDao.AclObjectIdentity(ownerSid, ngbSession.getId(),
                sessionAclClass.getId(), null, true);
        sessionIdentity.setId(4L);
        aclTestDao.createObjectIdentity(sessionIdentity);
    }

    @Test
    @WithMockUser(username = TEST_USER, roles = {ADMIN, USER})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sessionOwnerLoadTest() {
        NGBSession loaded = sessionSecurityService.load(ngbSession.getId());
        Assert.assertNotNull(loaded);
    }

    @Test(expected = AccessDeniedException.class)
    @WithMockUser(username = TEST_USER_2)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sessionUserLoadWithoutPermissionsTest() {
        sessionSecurityService.load(ngbSession.getId());
    }

    @Test
    @WithMockUser(username = TEST_USER, roles = {ADMIN, USER})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sessionOwnerFilterTest() {
        List<NGBSession> loaded = sessionSecurityService.filter(null);
        Assert.assertEquals(1, loaded.size());
    }

    @Test
    @WithMockUser(username = TEST_USER_2)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sessionUserFilterWithoutPermissionsTest() {
        List<NGBSession> loaded = sessionSecurityService.filter(null);
        Assert.assertEquals(0, loaded.size());
    }

    @Test
    @WithMockUser(username = TEST_USER_2)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sessionUserLoadWithPermissionsOnTrackTest() {
        aclTestDao.grantPermissions(bamFile, TEST_USER_2,
                Collections.singletonList(AclPermission.NAME_PERMISSION_MAP.get(READ)));
        NGBSession loaded = sessionSecurityService.load(ngbSession.getId());
        Assert.assertNotNull(loaded);
    }

    @Test
    @WithMockUser(username = TEST_USER_2)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sessionUserLoadWithPermissionsOnProjectTest() {
        aclTestDao.grantPermissions(project, TEST_USER_2,
                Collections.singletonList(AclPermission.NAME_PERMISSION_MAP.get(READ)));
        NGBSession loaded = sessionSecurityService.load(ngbSession.getId());
        Assert.assertNotNull(loaded);
    }


    @Test
    @WithMockUser(username = TEST_USER_2)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sessionUserFilterWithPermissionsOnTrackTest() {
        aclTestDao.grantPermissions(bamFile, TEST_USER_2,
                Collections.singletonList(AclPermission.NAME_PERMISSION_MAP.get(READ)));
        List<NGBSession> loaded = sessionSecurityService.filter(null);
        Assert.assertEquals(1, loaded.size());
    }

    @Test
    @WithMockUser(username = TEST_USER_2)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sessionUserFilterWithPermissionsOnProjectTest() {
        aclTestDao.grantPermissions(project, TEST_USER_2,
                Collections.singletonList(AclPermission.NAME_PERMISSION_MAP.get(READ)));
        List<NGBSession> loaded = sessionSecurityService.filter(null);
        Assert.assertEquals(1, loaded.size());
    }
}