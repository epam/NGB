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
package com.epam.catgenome.manager.externaldb.target.opentargets;

import com.epam.catgenome.entity.externaldb.target.opentargets.Disease;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.index.SearchRequest;
import com.epam.catgenome.util.FileFormat;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
@RequiredArgsConstructor
public class DiseaseSecurityService {

    private final DiseaseManager diseaseManager;
    private final DrugAssociationManager drugAssociationManager;
    private final DiseaseAssociationManager diseaseAssociationManager;

    @PreAuthorize(ROLE_USER)
    public Map<String, String> search(final String name) throws IOException, ParseException {
        return diseaseManager.search(name);
    }

    @PreAuthorize(ROLE_USER)
    public Disease searchById(final String diseaseId) throws IOException, ParseException {
        return diseaseManager.searchById(diseaseId);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<DrugAssociation> searchDrugs(final SearchRequest request, final String diseaseId)
            throws IOException, ParseException {
        return drugAssociationManager.search(request, diseaseId);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<DiseaseAssociation> searchTargets(final SearchRequest request, final String diseaseId)
            throws IOException, ParseException {
        return diseaseAssociationManager.search(request, diseaseId);
    }

    @PreAuthorize(ROLE_USER)
    public byte[] exportDrugs(final String diseaseId, final FileFormat format, final boolean includeHeader)
            throws IOException, ParseException {
        return drugAssociationManager.export(diseaseId, format, includeHeader);
    }


    @PreAuthorize(ROLE_USER)
    public byte[] exportTargets(final String diseaseId, final FileFormat format, final boolean includeHeader)
            throws IOException, ParseException {
        return diseaseAssociationManager.export(diseaseId, format, includeHeader);
    }
}
