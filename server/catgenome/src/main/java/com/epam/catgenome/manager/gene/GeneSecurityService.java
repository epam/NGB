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

package com.epam.catgenome.manager.gene;

import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.externaldb.DimStructure;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneHighLevel;
import com.epam.catgenome.entity.gene.GeneTranscript;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.exception.HistogramReadingException;
import com.epam.catgenome.security.acl.aspect.AclMaskList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
public class GeneSecurityService {

    private static final String READ_ON_FILE_OR_PROJECT_BY_TRACK =
            "hasPermissionOnFileOrParentProject(#track.id, 'com.epam.catgenome.entity.gene.GeneFile', " +
                    "#track.projectId, 'READ')";
    private static final String READ_ON_FILE_BY_ID = "hasPermission(#geneFileId, " +
            "'com.epam.catgenome.entity.gene.GeneFile', 'READ')";

    @Autowired
    private GeneFileManager geneFileManager;

    @Autowired
    private GffManager gffManager;


    @PreAuthorize(ROLE_ADMIN + OR + ROLE_GENE_MANAGER)
    public GeneFile registerGeneFile(FeatureIndexedFileRegistrationRequest request) {
        return gffManager.registerGeneFile(request);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_GENE_MANAGER)
    public GeneFile unregisterGeneFile(long geneFileId) throws IOException {
        return gffManager.unregisterGeneFile(geneFileId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_GENE_MANAGER)
    public GeneFile reindexGeneFile(long geneFileId, boolean full, boolean createTabixIndex) throws IOException {
        return gffManager.reindexGeneFile(geneFileId, full, createTabixIndex);
    }

    @AclMaskList
    @PostFilter(ROLE_ADMIN + OR + READ_ON_FILTER_OBJECT)
    public List<GeneFile> loadGeneFilesByReferenceId(Long referenceId) {
        return geneFileManager.loadGeneFilesByReferenceId(referenceId);
    }

    @PreAuthorize(ROLE_USER)
    public Track<Gene> loadGenes(Track<Gene> geneTrack, boolean collapsed, String fileUrl, String indexUrl)
            throws GeneReadingException {
        return gffManager.loadGenes(geneTrack, collapsed, fileUrl, indexUrl);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_ON_FILE_OR_PROJECT_BY_TRACK)
    public Track<Gene> loadGenes(Track<Gene> track, boolean collapsed) throws GeneReadingException {
        return gffManager.loadGenes(track, collapsed);
    }

    @PreAuthorize(ROLE_ADMIN + OR +
            "hasPermission(#blocks.get(0).gffId, 'com.epam.catgenome.entity.gene.GeneFile', 'READ')")
    public List<GeneHighLevel> convertGeneTrackForClient(List<Gene> blocks,
                                                         Map<Gene, List<ProteinSequenceEntry>> aminoAcids) {
        return gffManager.convertGeneTrackForClient(blocks, aminoAcids);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_ON_FILE_OR_PROJECT_BY_TRACK)
    public Track<GeneTranscript> loadGenesTranscript(Track<Gene> track, String fileUrl, String indexUrl)
            throws GeneReadingException {
        return gffManager.loadGenesTranscript(track, fileUrl, indexUrl);
    }

    @PreAuthorize(ROLE_USER)
    public DimStructure getPBDItemsFromBD(String pbdID) throws ExternalDbUnavailableException {
        return gffManager.getPBDItemsFromBD(pbdID);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_ON_FILE_OR_PROJECT_BY_TRACK)
    public Track<Wig> loadHistogram(Track<Wig> track) throws HistogramReadingException {
        return gffManager.loadHistogram(track);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_ON_FILE_BY_ID + OR + READ_PROJECT_BY_ID)
    public Gene getNextOrPreviousFeature(int fromPosition, long geneFileId, long chromosomeId, boolean forward,
                                         Long projectId) throws IOException {
        return gffManager.getNextOrPreviousFeature(fromPosition, geneFileId, chromosomeId, forward);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_ON_FILE_BY_ID + OR + READ_PROJECT_BY_ID)
    public List<Block> loadExonsInViewPort(Long geneFileId, Long chromosomeId, Integer centerPosition,
                                           Integer viewPortSize, Integer intronLength,
                                           Long projectId) throws IOException {
        return gffManager.loadExonsInViewPort(geneFileId, chromosomeId, centerPosition, viewPortSize, intronLength);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_ON_FILE_BY_ID + OR + READ_PROJECT_BY_ID)
    public List<Block> loadExonsInTrack(Long geneFileId, Long chromosomeId, Integer startIndex,
                                        Integer endIndex, Integer intronLength,
                                        Long projectId) throws IOException {
        return gffManager.loadExonsInTrack(geneFileId, chromosomeId, startIndex, endIndex, intronLength);
    }
}
