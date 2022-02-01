/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
package com.epam.catgenome.manager.pathway;

import com.epam.catgenome.controller.vo.registration.PathwayRegistrationRequest;
import com.epam.catgenome.entity.pathway.Pathway;
import com.epam.catgenome.entity.pathway.PathwayQueryParams;
import com.epam.catgenome.util.db.Page;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
@RequiredArgsConstructor
public class PathwaySecurityService {

    private final PathwayManager pathwayManager;

    @PreAuthorize(ROLE_USER)
    public Pathway loadPathway(final long pathwayId, final Long projectId) {
        return pathwayManager.loadPathway(pathwayId);
    }

    @PreAuthorize(ROLE_USER)
    public Page<Pathway> loadPathways(final PathwayQueryParams params) throws IOException, ParseException {
        return pathwayManager.loadPathways(params);
    }

    @PreAuthorize(ROLE_USER)
    public List<Pathway> loadPathways(final Long projectId) {
        return pathwayManager.loadPathways();
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_PATHWAY_MANAGER)
    public Pathway createPathway(final PathwayRegistrationRequest request) throws IOException {
        return pathwayManager.createPathway(request);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_PATHWAY_MANAGER)
    public void deletePathway(final long pathwayId) throws IOException {
        pathwayManager.deletePathway(pathwayId);
    }

    @PreAuthorize(ROLE_USER)
    public byte[] loadPathwayContent(final Long pathwayId, final Long projectId) throws IOException {
        return pathwayManager.loadPathwayContent(pathwayId);
    }
}
