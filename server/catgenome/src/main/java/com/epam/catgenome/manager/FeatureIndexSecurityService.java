/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018-2021 EPAM Systems
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

package com.epam.catgenome.manager;

import com.epam.catgenome.entity.gene.GeneFilterForm;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.security.acl.aspect.AclFilter;
import com.epam.catgenome.security.acl.aspect.AclMapFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.security.acl.SecurityExpressions.OR;
import static com.epam.catgenome.security.acl.SecurityExpressions.READ_PROJECT_BY_ID;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
public class FeatureIndexSecurityService {

    @Autowired
    private FeatureIndexManager featureIndexManager;

    @PreAuthorize(ROLE_USER)
    public IndexSearchResult<FeatureIndexEntry> searchFeaturesByReference(final String featureId,
                                                                          final Long referenceId) throws IOException {
        return featureIndexManager.searchFeaturesByReference(featureId, referenceId);
    }

    @PreAuthorize(ROLE_USER)
    public IndexSearchResult<GeneIndexEntry> searchFeaturesByReference(final GeneFilterForm filterForm,
                                                                       final Long referenceId) throws IOException {
        return featureIndexManager.searchGenesByReference(filterForm, referenceId);
    }

    @AclMapFilter
    @PreAuthorize(ROLE_USER)
    public Set<String> searchGenesInVcfFiles(String search, Map<Long, List<Long>> fileIdsByProject) throws IOException {
        return featureIndexManager.searchGenesInVcfFiles(search,
                fileIdsByProject.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    }

    @AclFilter
    @PreAuthorize(ROLE_USER)
    public IndexSearchResult<VcfIndexEntry> filterVariations(VcfFilterForm filterForm) throws IOException {
        return featureIndexManager.filterVariations(filterForm);
    }

    @AclFilter
    @PreAuthorize(ROLE_USER)
    public List<Group> groupVariations(VcfFilterForm filterForm, String groupBy) throws IOException {
        return featureIndexManager.groupVariations(filterForm, groupBy);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_PROJECT_BY_ID)
    public IndexSearchResult<FeatureIndexEntry> searchFeaturesInProject(final String featureId,
                                                                        final Long projectId) throws IOException {
        return featureIndexManager.searchFeaturesInProject(featureId, projectId);
    }

    @AclFilter
    @PreAuthorize(ROLE_ADMIN + OR + READ_PROJECT_BY_ID)
    public IndexSearchResult<VcfIndexEntry> filterVariations(VcfFilterForm filterForm, long projectId)
            throws IOException {
        return featureIndexManager.filterVariations(filterForm, projectId);
    }

    @AclFilter
    @PreAuthorize(ROLE_ADMIN + OR + READ_PROJECT_BY_ID)
    public List<Group> groupVariations(VcfFilterForm filterForm, long projectId, String groupBy) throws IOException {
        return featureIndexManager.groupVariations(filterForm, projectId, groupBy);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_PROJECT_BY_ID)
    public Set<String> searchGenesInVcfFilesInProject(long projectId, String search, List<Long> vcfIds)
            throws IOException {
        return featureIndexManager.searchGenesInVcfFilesInProject(projectId, search, vcfIds);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_PROJECT_BY_ID)
    public VcfFilterInfo loadVcfFilterInfoForProject(Long projectId) throws IOException {
        return featureIndexManager.loadVcfFilterInfoForProject(projectId);
    }

    public Object getAttributeFields(Long referenceId) {
        return null;
    }
}
