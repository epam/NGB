/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
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

package com.epam.catgenome.manager.vcf;

import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationQuery;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.security.acl.aspect.AclMapFilter;
import com.epam.catgenome.security.acl.aspect.AclMask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
public class VcfSecurityService {

    private static final String READ_VCF_BY_TRACK_ID =
            "hasPermissionOnFileOrParentProject(#track.id, 'com.epam.catgenome.entity.vcf.VcfFile', " +
                    "#track.projectId, 'READ')";
    private static final String READ_VCF_BY_QUERY_ID =
            "hasPermissionOnFileOrParentProject(#query.id, 'com.epam.catgenome.entity.vcf.VcfFile', " +
                    "#query.projectId, 'READ')";

    @Autowired
    private VcfManager vcfManager;

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_VCF_MANAGER)
    public VcfFile registerVcfFile(FeatureIndexedFileRegistrationRequest request) {
        return vcfManager.registerVcfFile(request);
    }

    @AclMask
    @PreAuthorize(ROLE_ADMIN + OR + ROLE_VCF_MANAGER)
    public VcfFile reindexVcfFile(long vcfFileId, boolean createTabixIndex) throws FeatureIndexException {
        return vcfManager.reindexVcfFile(vcfFileId, createTabixIndex);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_VCF_MANAGER)
    public VcfFile unregisterVcfFile(long vcfFileId) throws IOException {
        return vcfManager.unregisterVcfFile(vcfFileId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_VCF_BY_TRACK_ID)
    public Track<Variation> loadVariations(Track<Variation> track, Long sampleId, boolean loadInfo,
                                 boolean collapsed) throws VcfReadingException {
        return vcfManager.loadVariations(track, sampleId, loadInfo, collapsed);
    }

    @PreAuthorize(ROLE_USER)
    public Track<Variation> loadVariations(final Track<Variation> track, String fileUrl, String indexUrl,
                                           final Integer sampleIndex, final boolean loadInfo,
                                           final boolean collapse) throws VcfReadingException {
        return vcfManager.loadVariations(track, fileUrl, indexUrl, sampleIndex, loadInfo, collapse);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_VCF_BY_QUERY_ID)
    public Variation loadVariation(VariationQuery query) throws FeatureFileReadingException {
        return vcfManager.loadVariation(query);
    }

    @PreAuthorize(ROLE_USER)
    public Variation loadVariation(VariationQuery query, String fileUrl, String indexUrl)
            throws FeatureFileReadingException {
        return vcfManager.loadVariation(query, fileUrl, indexUrl);
    }

    @PreAuthorize(ROLE_USER)
    public Variation getNextOrPreviousVariation(int fromPosition, Long trackId, Long sampleId, long chromosomeId,
                                                boolean loadInfo, String fileUrl, String indexUrl, Long projectId)
                                                throws VcfReadingException {
        return vcfManager.getNextOrPreviousVariation(fromPosition, trackId, sampleId, chromosomeId,
                                                     loadInfo, fileUrl, indexUrl);
    }

    @AclMapFilter
    @PreAuthorize(ROLE_USER)
    public VcfFilterInfo getFiltersInfo(Map<Long, List<Long>> fileIdsByProject) throws IOException {
        return vcfManager.getFiltersInfo(
                fileIdsByProject.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    }
}
