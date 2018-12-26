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

package com.epam.catgenome.manager.user;

import com.epam.catgenome.common.AbstractACLSecurityTest;
import com.epam.catgenome.controller.vo.NgbUserVO;
import com.epam.catgenome.entity.security.NgbUser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;

public class UserSecurityServiceTest extends AbstractACLSecurityTest {

    private static final String TEST_USER = "USER";
    private static final String TEST_ADMIN = "ADMIN";
    private static final String USER_2 = "USER2";

    @Autowired
    private UserSecurityService userSecurityService;

    @Test
    @WithMockUser(value = TEST_ADMIN, roles = {"ADMIN", "USER"})
    public void createUserPassTest() {
        NgbUserVO userVO = new NgbUserVO();
        userVO.setRoleIds(Collections.singletonList(2L));
        userVO.setUserName(USER_2);
        NgbUser created = userSecurityService.createUser(userVO);

        NgbUser loaded = userSecurityService.loadUser(created.getId());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(loaded.getUserName(), USER_2);
    }

    @Test(expected = AccessDeniedException.class)
    @WithMockUser(value = TEST_USER)
    public void createUserFailTest() {
        NgbUserVO userVO = new NgbUserVO();
        userVO.setRoleIds(Collections.singletonList(2L));
        userVO.setUserName(USER_2);
        userSecurityService.createUser(userVO);
    }

    @Test
    @WithMockUser(value = TEST_USER)
    public void getUserContextShouldWorkForAnyUserTest() {
        userSecurityService.getUserContext();
    }
}