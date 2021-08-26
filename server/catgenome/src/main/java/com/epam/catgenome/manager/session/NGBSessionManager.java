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

import com.epam.catgenome.dao.session.NGBSessionDao;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.entity.session.NGBSession;
import com.epam.catgenome.entity.session.NGBSessionFilter;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.manager.SecuredEntityManager;
import com.epam.catgenome.security.acl.aspect.AclSync;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@AclSync
@Service
@AllArgsConstructor
public class NGBSessionManager implements SecuredEntityManager {

    @Autowired
    private final NGBSessionDao sessionDao;

    @Autowired
    private final AuthManager authManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public NGBSession create(final NGBSession session) {
        Assert.hasText(session.getName(), "Session name couldn't be empty");
        Assert.hasText(session.getSessionValue(), "Session value couldn't be empty");
        session.setOwner(authManager.getAuthorizedUser());
        return sessionDao.create(session);
    }


    public List<NGBSession> filter(final NGBSessionFilter filter) {
        return sessionDao.filter(filter);
    }


    public NGBSession load(final Long id) {
        return sessionDao.load(id).orElseThrow(() ->
                new IllegalArgumentException("Can't find a session with id: " + id));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public NGBSession delete(final Long id) {
        return sessionDao.delete(id).orElseThrow(() ->
                new IllegalArgumentException("Can't find a session with id: " + id));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public NGBSession update(final NGBSession session) {
        Assert.notNull(load(session.getId()), "Can't find a session with id: " + session.getId());
        return sessionDao.update(session);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AbstractSecuredEntity changeOwner(final Long id, final String owner) {
        final NGBSession session = load(id);
        session.setOwner(owner);
        return update(session);
    }

    @Override
    public AclClass getSupportedClass() {
        return AclClass.SESSION;
    }
}
