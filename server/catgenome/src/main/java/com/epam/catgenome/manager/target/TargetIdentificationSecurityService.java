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

import com.epam.catgenome.controller.vo.externaldb.NCBISummaryVO;
import com.epam.catgenome.controller.vo.target.PublicationSearchRequest;
import com.epam.catgenome.controller.vo.target.StructuresSearchRequest;
import com.epam.catgenome.entity.externaldb.target.opentargets.BareDisease;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.target.GeneRefSection;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.entity.target.GeneSequences;
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBDrugFieldValues;
import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociationAggregated;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.target.IdentificationRequest;
import com.epam.catgenome.entity.target.IdentificationResult;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugFieldValues;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

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
            throws ParseException, IOException {
        return manager.getDGIDbDrugs(request);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<PharmGKBDrug> getPharmGKBDrugs(final AssociationSearchRequest request)
            throws ParseException, IOException {
        return manager.getPharmGKBDrugs(request);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<PharmGKBDisease> getPharmGKBDiseases(final AssociationSearchRequest request)
            throws ParseException, IOException {
        return manager.getPharmGKBDiseases(request);
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

    @PreAuthorize(ROLE_USER)
    public List<DiseaseAssociationAggregated> getAllOpenTargetsDiseases(final AssociationSearchRequest request)
            throws IOException, ParseException {
        return manager.getAllOpenTargetsDiseases(request);
    }

    @PreAuthorize(ROLE_ADMIN)
    public void importOpenTargetsData(final String path) throws IOException, ParseException {
        manager.importOpenTargetsData(path);
    }

    @PreAuthorize(ROLE_ADMIN)
    public void importPharmGKBData(final String genePath, final String drugPath,
                                   final String drugAssociationPath, final String diseaseAssociationPath)
            throws IOException, ParseException {
        manager.importPharmGKBData(genePath, drugPath, drugAssociationPath, diseaseAssociationPath);
    }

    @PreAuthorize(ROLE_ADMIN)
    public void importDGIdbData(final String path) throws IOException, ParseException {
        manager.importDGIdbData(path);
    }

    @PreAuthorize(ROLE_USER)
    public PharmGKBDrugFieldValues getPharmGKBDrugFieldValues(final List<String> geneIds)
            throws IOException, ParseException {
        return manager.getPharmGKBDrugFieldValues(geneIds);
    }

    @PreAuthorize(ROLE_USER)
    public DGIDBDrugFieldValues getDGIDBDrugFieldValues(final List<String> geneIds)
            throws IOException, ParseException {
        return manager.getDGIDBDrugFieldValues(geneIds);
    }

    @PreAuthorize(ROLE_USER)
    public DrugFieldValues getDrugFieldValues(final List<String> geneIds) throws IOException, ParseException {
        return manager.getDrugFieldValues(geneIds);
    }

    @PreAuthorize(ROLE_USER)
    public List<BareDisease> getDiseasesTree() throws IOException {
        return manager.getDiseasesTree();
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<NCBISummaryVO> getPublications(final PublicationSearchRequest request) {
        return manager.getPublications(request);
    }

    @PreAuthorize(ROLE_USER)
    public String getArticleAbstracts(final PublicationSearchRequest request) {
        return manager.getArticlesAbstracts(request);
    }

    @PreAuthorize(ROLE_USER)
    public List<GeneSequences> getGeneSequences(final List<String> geneIds) throws ParseException, IOException {
        return manager.getGeneSequences(geneIds);
    }

    @PreAuthorize(ROLE_USER)
    public List<GeneRefSection> getGeneSequencesTable(final List<String> geneIds, final Boolean getComments)
            throws ParseException, IOException, ExternalDbUnavailableException {
        return manager.getGeneSequencesTable(geneIds, getComments);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<Structure> getStructures(final StructuresSearchRequest request) {
        return manager.getStructures(request);
    }
}
