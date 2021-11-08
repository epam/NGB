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
package com.epam.catgenome.manager.lineage;

import com.epam.catgenome.controller.vo.registration.LineageTreeRegistrationRequest;
import com.epam.catgenome.entity.lineage.LineageTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.OR;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_LINEAGE_TREE_MANAGER;

@Service
public class LineageTreeSecurityService {

    private static final String READ_TREE_BY_PROJECT_ID =
            "readOnALineageTreeIsAllowed(#lineageTreeId, #projectId)";

    @Autowired
    private LineageTreeManager lineageTreeManager;

    @PreAuthorize(ROLE_ADMIN + OR + READ_TREE_BY_PROJECT_ID)
    public LineageTree loadLineageTree(final long lineageTreeId, final Long projectId) {
        return lineageTreeManager.loadLineageTree(lineageTreeId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_TREE_BY_PROJECT_ID)
    public List<LineageTree> loadLineageTrees(final long referenceId, final Long lineageTreeId, final Long projectId) {
        return lineageTreeManager.loadLineageTrees(referenceId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_LINEAGE_TREE_MANAGER)
    public List<LineageTree> loadAllLineageTrees() {
        return lineageTreeManager.loadAllLineageTrees();
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_LINEAGE_TREE_MANAGER)
    public LineageTree createLineageTree(final LineageTreeRegistrationRequest request) throws IOException {
        return lineageTreeManager.createLineageTree(request);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_LINEAGE_TREE_MANAGER)
    public void deleteLineageTree(final long lineageTreeId) throws IOException {
        lineageTreeManager.deleteLineageTree(lineageTreeId);
    }
}
