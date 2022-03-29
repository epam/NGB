/*
 * MIT License
 *
 * Copyright (c) 2018-2022 EPAM Systems
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
package com.epam.catgenome.manager.vcf;

import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationQuery;
import com.epam.catgenome.entity.vcf.VcfFieldValues;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.security.acl.aspect.AclMapFilter;
import com.epam.catgenome.security.acl.aspect.AclMask;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
@RequiredArgsConstructor
public class VcfSecurityService {

    private static final String READ_VCF_BY_TRACK_ID =
            "hasPermissionOnFileOrParentProject(#track.id, 'com.epam.catgenome.entity.vcf.VcfFile', " +
                    "#track.projectId, 'READ')";
    private static final String READ_VCF_BY_QUERY_ID =
            "hasPermissionOnFileOrParentProject(#query.id, 'com.epam.catgenome.entity.vcf.VcfFile', " +
                    "#query.projectId, 'READ')";

    private final VcfManager vcfManager;
    private final VcfFileManager vcfFileManager;

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_VCF_MANAGER)
    public VcfFile registerVcfFile(final FeatureIndexedFileRegistrationRequest request) {
        return vcfManager.registerVcfFile(request);
    }

    @AclMask
    @PreAuthorize(ROLE_ADMIN + OR + ROLE_VCF_MANAGER)
    public VcfFile reindexVcfFile(final long vcfFileId, final boolean createTabixIndex) throws FeatureIndexException {
        return vcfManager.reindexVcfFile(vcfFileId, createTabixIndex);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_VCF_MANAGER)
    public VcfFile unregisterVcfFile(final long vcfFileId) throws IOException {
        return vcfManager.unregisterVcfFile(vcfFileId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_VCF_BY_TRACK_ID)
    public Track<Variation> loadVariations(final Track<Variation> track, final Long sampleId, final boolean loadInfo,
                                 final boolean collapsed) throws VcfReadingException {
        return vcfManager.loadVariations(track, sampleId, loadInfo, collapsed);
    }

    @PreAuthorize(ROLE_USER)
    public Track<Variation> loadVariations(final Track<Variation> track, final String fileUrl, final String indexUrl,
                                           final Integer sampleIndex, final boolean loadInfo,
                                           final boolean collapse) throws VcfReadingException {
        return vcfManager.loadVariations(track, fileUrl, indexUrl, sampleIndex, loadInfo, collapse);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_VCF_BY_QUERY_ID)
    public Variation loadVariation(final VariationQuery query) throws FeatureFileReadingException {
        return vcfManager.loadVariation(query);
    }

    @PreAuthorize(ROLE_USER)
    public Variation loadVariation(final VariationQuery query, final String fileUrl, final String indexUrl)
            throws FeatureFileReadingException {
        return vcfManager.loadVariation(query, fileUrl, indexUrl);
    }

    @PreAuthorize(ROLE_USER)
    public Variation getNextOrPreviousVariation(final int fromPosition, final Long trackId, final Long sampleId,
                                                final long chromosomeId, final boolean loadInfo, final String fileUrl,
                                                final String indexUrl, final Long projectId)
                                                throws VcfReadingException {
        return vcfManager.getNextOrPreviousVariation(fromPosition, trackId, sampleId, chromosomeId,
                                                     loadInfo, fileUrl, indexUrl);
    }

    @AclMapFilter
    @PreAuthorize(ROLE_USER)
    public VcfFilterInfo getFiltersInfo(final Map<Long, List<Long>> fileIdsByProject) throws IOException {
        return vcfManager.getFiltersInfo(
                fileIdsByProject.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    }

    @PreAuthorize(ROLE_USER)
    public void setVcfAliases(final Map<String, String> aliases, final long vcfFileId) {
        vcfFileManager.setVcfAliases(aliases, vcfFileId);
    }

    @PreAuthorize(ROLE_USER)
    public VcfFieldValues loadFieldValues(final Long vcfFileId,
                                          final String fieldName,
                                          final Integer maxSize) throws IOException {
        return vcfManager.loadFieldValues(vcfFileId, fieldName, maxSize);
    }
}
