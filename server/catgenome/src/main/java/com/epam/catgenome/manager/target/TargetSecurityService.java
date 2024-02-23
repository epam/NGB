/*
 * MIT License
 *
 * Copyright (c) 2023-2024 EPAM Systems
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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetQueryParams;
import com.epam.catgenome.exception.TargetUpdateException;
import com.epam.catgenome.security.acl.aspect.AclMask;
import com.epam.catgenome.security.acl.aspect.AclMaskList;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
@RequiredArgsConstructor
public class TargetSecurityService {

    private final TargetManager targetManager;

    @AclMask
    @PreAuthorize(ROLE_USER)
    public Target loadTarget(final long targetId) {
        return targetManager.load(targetId);
    }

    @AclMaskList
    @PreAuthorize(ROLE_USER)
    @PostFilter(ROLE_TARGET_MANAGER + OR + "isAllowed('READ', filterObject)")
    public List<Target> loadTargets(final TargetQueryParams queryParameters) {
        return targetManager.load(queryParameters);
    }

    @AclMaskList
    @PreAuthorize(ROLE_USER)
    @PostFilter(ROLE_TARGET_MANAGER + OR + "isAllowed('READ', filterObject)")
    public List<Target> loadTargets(final String geneName, final Long taxId) throws ParseException, IOException {
        return targetManager.load(geneName, taxId);
    }

    @PreAuthorize(ROLE_USER)
    public List<Target> loadTargets() {
        return targetManager.load();
    }

    @PreAuthorize(ROLE_USER)
    public Target createTarget(final Target target) throws IOException {
        return targetManager.create(target);
    }

    @PreAuthorize(ROLE_TARGET_MANAGER + OR + "hasPermissionOnTarget(#targetId, 'WRITE')")
    public void deleteTarget(final long targetId) throws ParseException, IOException {
        targetManager.delete(targetId);
    }

    @PreAuthorize(ROLE_USER)
    public List<String> loadFieldValues(final TargetField field) {
        return targetManager.loadFieldValues(field);
    }

    @AclMask
    @PreAuthorize(ROLE_TARGET_MANAGER + OR + "hasPermissionOnTarget(#target.id, 'WRITE')")
    public Target updateTarget(final Target target) throws TargetUpdateException, IOException {
        return targetManager.update(target);
    }
}
