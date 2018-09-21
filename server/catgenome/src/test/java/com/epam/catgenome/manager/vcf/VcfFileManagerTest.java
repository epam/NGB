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

package com.epam.catgenome.manager.vcf;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.app.AclSecurityConfiguration;
import com.epam.catgenome.app.XmlConfigAdapter;
import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.dao.vcf.VcfFileDao;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.security.acl.JdbcMutableAclServiceImpl;
import com.epam.catgenome.security.acl.aspect.AclAspect;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AclSecurityConfiguration.class, XmlConfigAdapter.class})
@TestPropertySource(locations = "classpath:test-catgenome-acl.properties")
@Ignore //TODO: fxi or remove
public class VcfFileManagerTest extends AbstractManagerTest {
    @Autowired
    private VcfFileManager vcfFileManager;

    @MockBean
    private VcfFileDao vcfFileDao;

    @Autowired
    private JdbcMutableAclServiceImpl aclService;

    @Autowired
    private AclAspect aclAspect;

    @Autowired
    private AuthManager authManager;

    @Test
    public void testCreate() {
        VcfFile vcfFile = new VcfFile();
        vcfFileManager.create(vcfFile);
        authManager.getAuthorizedUser();
        //Mockito.verify(aclService).createAcl(Mockito.any(AbstractSecuredEntity.class));
    }
}