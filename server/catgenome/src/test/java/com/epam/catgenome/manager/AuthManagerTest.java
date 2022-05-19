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

package com.epam.catgenome.manager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.common.AbstractSecurityTest;
import com.epam.catgenome.common.security.WithMockUserContext;
import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.entity.security.JwtTokenClaims;
import com.epam.catgenome.security.UserContext;
import com.epam.catgenome.security.jwt.JwtTokenVerifier;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuthManagerTest extends AbstractSecurityTest {
    private static final String TEST_USER_NAME = "test";
    private static final long TEST_USER_ID = 1L;
    private static final String TEST_GROUP = "TEST";
    private static final String TEST_ORG_UNIT_ID = "EPAM";

    @Autowired
    private AuthManager authManager;

    @Autowired
    private JwtTokenVerifier jwtTokenVerifier;

    @MockBean
    protected SAMLEntryPoint samlEntryPoint;

    @MockBean
    protected SAMLAuthenticationProvider samlAuthenticationProvider;

    @Test
    @WithMockUserContext(userName = TEST_USER_NAME)
    public void testGetUserContext() {
        UserContext context = authManager.getUserContext();
        Assert.assertNotNull(context);
        Assert.assertEquals(TEST_USER_NAME, context.getUsername());
    }

    @Test
    public void testGetNoUserContext() {
        UserContext context = authManager.getUserContext();
        Assert.assertNull(context);
    }

    @Test
    @WithMockUserContext(userName = TEST_USER_NAME, userId = TEST_USER_ID, groups = TEST_GROUP,
        orgUnitId = TEST_ORG_UNIT_ID)
    public void testIssueToken() {
        JwtRawToken token = authManager.issueTokenForCurrentUser(null);
        JwtTokenClaims claims = jwtTokenVerifier.readClaims(token.getToken());
        Assert.assertEquals(TEST_USER_NAME, claims.getUserName());
        Assert.assertEquals(TEST_USER_ID, claims.getUserId().longValue());
    }
}
