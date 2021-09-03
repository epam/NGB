/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.manager.session;

import com.epam.catgenome.entity.session.NGBSession;
import com.epam.catgenome.entity.session.NGBSessionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NGBSessionSecurityService {

    public static final String ROLE_USER = "hasRole('USER')";
    public static final String ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String OR = " OR ";
    public static final String WRITE_SESSION_BY_ID = "hasPermissionOnSession(#id, 'WRITE')";
    public static final String SESSION_IS_READABLE = "sessionIsReadable(returnObject)";
    public static final String FILTERED_SESSION_IS_READABLE = "sessionIsReadable(filterObject)";


    @Autowired
    private final NGBSessionManager sessionManager;

    @PreAuthorize(ROLE_USER)
    public NGBSession create(final NGBSession session) {
        return sessionManager.create(session);
    }


    @PostFilter(FILTERED_SESSION_IS_READABLE)
    public List<NGBSession> filter(final NGBSessionFilter filter) {
        return sessionManager.filter(filter);
    }


    @PostAuthorize(SESSION_IS_READABLE)
    public NGBSession load(final Long id) {
        return sessionManager.load(id);
    }

    @PreAuthorize(ROLE_ADMIN + OR + WRITE_SESSION_BY_ID)
    public NGBSession update(final NGBSession session) {
        return sessionManager.update(session);
    }

    @PreAuthorize(ROLE_ADMIN + OR + WRITE_SESSION_BY_ID)
    public NGBSession delete(final Long id) {
        return sessionManager.delete(id);
    }

}
