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

package com.epam.catgenome.common.security;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.epam.catgenome.security.UserContext;
import com.epam.catgenome.security.jwt.JwtAuthenticationToken;

public class WithMockUserContextSecurityContextFactory implements WithSecurityContextFactory<WithMockUserContext> {

    @Override
    public SecurityContext createSecurityContext(WithMockUserContext annotation) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        UserContext userContext = new UserContext(annotation.userName());

        if (annotation.userId() > 0) {
            userContext.setUserId(annotation.userId());
        }

        userContext.setGroups(Arrays.asList(annotation.groups()));

        if (StringUtils.isNotBlank(annotation.orgUnitId())) {
            userContext.setOrgUnitId(annotation.orgUnitId());
        }

        Authentication auth = new JwtAuthenticationToken(userContext, Collections.emptyList());
        securityContext.setAuthentication(auth);
        return securityContext;
    }
}
