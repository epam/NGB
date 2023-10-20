/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
import com.epam.catgenome.util.db.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
@RequiredArgsConstructor
public class TargetSecurityService {

    private final TargetManager targetManager;

    @PreAuthorize(ROLE_USER)
    public Target loadTarget(final long targetId) {
        return targetManager.load(targetId);
    }

    @PreAuthorize(ROLE_USER)
    public Page<Target> loadTargets(final TargetQueryParams queryParameters) {
        return targetManager.load(queryParameters);
    }

    @PreAuthorize(ROLE_USER)
    public List<Target> loadTargets(final String geneName, final Long taxId) {
        return targetManager.load(geneName, taxId);
    }

    @PreAuthorize(ROLE_USER)
    public Target createTarget(final Target target) {
        return targetManager.create(target);
    }

    @PreAuthorize(ROLE_USER)
    public void deleteTarget(final long targetId) {
        targetManager.delete(targetId);
    }

    @PreAuthorize(ROLE_USER)
    public List<String> loadFieldValues(final TargetField field) {
        return targetManager.loadFieldValues(field);
    }

    @PreAuthorize(ROLE_USER)
    public Target updateTarget(final Target target) {
        return targetManager.update(target);
    }
}
