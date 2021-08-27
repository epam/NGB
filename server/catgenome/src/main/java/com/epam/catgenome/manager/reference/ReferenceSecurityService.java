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

package com.epam.catgenome.manager.reference;

import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.entity.reference.StrandedSequence;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.ReferenceReadingException;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.security.acl.aspect.AclMask;
import com.epam.catgenome.security.acl.aspect.AclMaskList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
public class ReferenceSecurityService {

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private MotifSearchManager motifSearchManager;

    @AclMaskList
    @PreAuthorize(ROLE_USER)
    public List<Reference> loadAllReferenceGenomes(String referenceName) {
        return referenceGenomeManager.loadAllReferenceGenomes(referenceName);
    }

    @AclMask
    @PreAuthorize(ROLE_USER)
    public Reference load(Long referenceId) {
        return referenceGenomeManager.load(referenceId);
    }

    @AclMask
    @PreAuthorize(ROLE_USER)
    public Reference loadReferenceGenomeByBioItemId(Long bioItemID) {
        return referenceGenomeManager.loadReferenceGenomeByBioItemId(bioItemID);
    }

    @PreAuthorize(ROLE_USER)
    public List<Chromosome> loadChromosomes(Long referenceId) {
        return referenceGenomeManager.loadChromosomes(referenceId);
    }

    @PreAuthorize(ROLE_USER)
    public Chromosome loadChromosome(Long chromosomeId) {
        return referenceGenomeManager.loadChromosome(chromosomeId);
    }

    @PreAuthorize(ROLE_USER)
    public Track<Sequence> getNucleotidesResultFromNib(Track<Sequence> track) throws ReferenceReadingException {
        return referenceManager.getNucleotidesResultFromNib(track);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_REFERENCE_MANAGER)
    public Reference registerGenome(ReferenceRegistrationRequest request) throws IOException {
        return referenceManager.registerGenome(request);
    }

    @AclMask
    @PreAuthorize(ROLE_ADMIN + OR + ROLE_REFERENCE_MANAGER)
    public Reference updateReferenceGeneFileId(Long referenceId, Long geneFileId) {
        return referenceGenomeManager.updateReferenceGeneFileId(referenceId, geneFileId);
    }

    @AclMask
    @PreAuthorize(ROLE_ADMIN + OR + ROLE_REFERENCE_MANAGER)
    public Reference updateReferenceAnnotationFile(Long referenceId, Long geneFileId, Boolean remove)
            throws IOException, FeatureIndexException {
        return referenceGenomeManager.updateReferenceAnnotationFile(referenceId, geneFileId, remove);
    }

    @AclMask
    @PreAuthorize(ROLE_ADMIN + OR + ROLE_REFERENCE_MANAGER)
    public Reference unregisterGenome(long referenceId) throws IOException {
        return referenceManager.unregisterGenome(referenceId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_REFERENCE_MANAGER)
    public Species registerSpecies(Species species) {
        return referenceGenomeManager.registerSpecies(species);

    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_REFERENCE_MANAGER)
    public Species updateSpecies(Species species) {
        return referenceGenomeManager.updateSpecies(species);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_REFERENCE_MANAGER)
    public Species unregisterSpecies(String speciesVersion) {
        return referenceGenomeManager.unregisterSpecies(speciesVersion);
    }

    @PreAuthorize(ROLE_USER)
    public List<Species> loadAllSpecies() {
        return referenceGenomeManager.loadAllSpecies();
    }

    @AclMask
    @PreAuthorize(ROLE_ADMIN + OR + ROLE_REFERENCE_MANAGER)
    public Reference updateSpecies(Long referenceId, String speciesVersion) {
        return referenceGenomeManager.updateSpecies(referenceId, speciesVersion);
    }

    @PreAuthorize(ROLE_USER)
    @PostFilter(READ_ON_FILTER_OBJECT)
    public List<BiologicalDataItem> getReferenceAnnotationFiles(Long referenceId) {
        return referenceGenomeManager.getReferenceAnnotationFiles(referenceId);
    }

    @PreAuthorize(ROLE_USER)
    public Track<StrandedSequence> fillTrackWithMotifSearch(Track<StrandedSequence> track, String motif, StrandSerializable strand) {
        return motifSearchManager.fillTrackWithMotifSearch(track, motif, strand);
    }

    @PreAuthorize(ROLE_USER)
    public MotifSearchResult getTableByMotif(MotifSearchRequest motifSearchRequest) {
        return motifSearchManager.search(motifSearchRequest);
    }
}
