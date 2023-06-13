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

import com.epam.catgenome.entity.externaldb.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.opentarget.DiseaseAssociationAggregated;
import com.epam.catgenome.entity.externaldb.opentarget.DrugAssociation;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.target.IdentificationRequest;
import com.epam.catgenome.entity.target.IdentificationResult;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
@RequiredArgsConstructor
public class TargetIdentificationSecurityService {

    private final TargetIdentificationManager manager;

    @PreAuthorize(ROLE_USER)
    public IdentificationResult launchIdentification(final IdentificationRequest request)
            throws ExternalDbUnavailableException, ParseException, IOException {
        return manager.launchIdentification(request);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<DGIDBDrugAssociation> getDGIDbDrugs(final AssociationSearchRequest request)
            throws ExternalDbUnavailableException, ParseException, IOException {
        return manager.getDGIDbDrugs(request);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<PharmGKBDrug> getPharmGKBDrugs(final AssociationSearchRequest request)
            throws ExternalDbUnavailableException, ParseException, IOException {
        return manager.getPharmGKBDrugs(request);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<DrugAssociation> getOpenTargetsDrugs(final AssociationSearchRequest request)
            throws IOException, ParseException {
        return manager.getOpenTargetsDrugs(request);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<DiseaseAssociationAggregated> getOpenTargetsDiseases(final AssociationSearchRequest request)
            throws IOException, ParseException {
        return manager.getOpenTargetsDiseases(request);
    }

    @PreAuthorize(ROLE_ADMIN)
    public void importOpenTargetsData(final String targetsPath, final String diseasesPath, final String drugsPath,
                                      final String overallScoresPath, final String scoresPath) throws IOException {
        manager.importOpenTargetsData(targetsPath, diseasesPath, drugsPath, overallScoresPath, scoresPath);
    }

    @PreAuthorize(ROLE_ADMIN)
    public void importPharmGKBData(final String genePath, final String drugPath, final String drugAssociationPath)
            throws IOException {
        manager.importPharmGKBData(genePath, drugPath, drugAssociationPath);
    }

    @PreAuthorize(ROLE_ADMIN)
    public void importDGIdbData(final String path) throws IOException {
        manager.importDGIdbData(path);
    }
}
