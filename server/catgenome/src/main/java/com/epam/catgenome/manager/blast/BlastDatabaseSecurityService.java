/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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
package com.epam.catgenome.manager.blast;

import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.security.acl.aspect.AclTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
public class BlastDatabaseSecurityService {

    @Autowired
    private BlastDatabaseManager blastDatabaseManager;

    @PreAuthorize(ROLE_ADMIN)
    public void save(final BlastDatabase database) {
        blastDatabaseManager.save(database);
    }

    @AclTree
    @PreAuthorize(ROLE_ADMIN)
    public void delete(final long id) {
        blastDatabaseManager.delete(id);
    }

    @PreAuthorize(ROLE_USER)
    public BlastDatabase loadById(final long id) {
        return blastDatabaseManager.loadById(id);
    }

    @PreAuthorize(ROLE_USER)
    public List<BlastDatabase> load(final BlastDatabaseType type, final String path) {
        return blastDatabaseManager.load(type, path);
    }
}
