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
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.ReferenceReadingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ReferenceSecurityService {

    private static final String HAS_ROLE_USER = "hasRole('USER')";
    private static final String HAS_ROLE_ADMIN_OR_REFERENCE_MANAGER =
            "hasRole('ADMIN') OR hasRole('REFERENCE_MANAGER')";

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @PreAuthorize(HAS_ROLE_USER)
    public List<Reference> loadAllReferenceGenomes(String referenceName) {
        return referenceGenomeManager.loadAllReferenceGenomes(referenceName);
    }

    @PreAuthorize(HAS_ROLE_USER)
    public Reference load(Long referenceId) {
        return referenceGenomeManager.load(referenceId);
    }

    @PreAuthorize(HAS_ROLE_USER)
    public Reference loadReferenceGenomeByBioItemId(Long bioItemID) {
        return referenceGenomeManager.loadReferenceGenomeByBioItemId(bioItemID);
    }

    @PreAuthorize(HAS_ROLE_USER)
    public List<Chromosome> loadChromosomes(Long referenceId) {
        return referenceGenomeManager.loadChromosomes(referenceId);
    }

    @PreAuthorize(HAS_ROLE_USER)
    public Chromosome loadChromosome(Long chromosomeId) {
        return referenceGenomeManager.loadChromosome(chromosomeId);
    }


    @PreAuthorize(HAS_ROLE_USER)
    public Track<Sequence> getNucleotidesResultFromNib(Track<Sequence> track) throws ReferenceReadingException {
        return referenceManager.getNucleotidesResultFromNib(track);
    }

    @PreAuthorize(HAS_ROLE_ADMIN_OR_REFERENCE_MANAGER)
    public Reference registerGenome(ReferenceRegistrationRequest request) throws IOException {
        return referenceManager.registerGenome(request);
    }

    @PreAuthorize("hasPermission(#referenceId, com.epam.catgenome.entity.reference.Reference, 'WRITE') " +
            "AND hasPermission(#geneFileId, com.epam.catgenome.entity.gene.GeneFile, 'READ')")
    public Reference updateReferenceGeneFileId(Long referenceId, Long geneFileId) {
        return referenceGenomeManager.updateReferenceGeneFileId(referenceId, geneFileId);
    }

    @PreAuthorize("hasPermission(#referenceId, com.epam.catgenome.entity.reference.Reference, 'WRITE') " +
            "AND hasPermission(#geneFileId, com.epam.catgenome.entity.gene.GeneFile, 'READ')")
    public Reference updateReferenceAnnotationFile(Long referenceId, Long annotationFileId, Boolean remove)
            throws IOException, FeatureIndexException {
        return referenceGenomeManager.updateReferenceAnnotationFile(referenceId, annotationFileId, remove);
    }

    @PreAuthorize(HAS_ROLE_ADMIN_OR_REFERENCE_MANAGER)
    public Reference unregisterGenome(long referenceId) throws IOException {
        return referenceManager.unregisterGenome(referenceId);
    }

    @PreAuthorize(HAS_ROLE_ADMIN_OR_REFERENCE_MANAGER)
    public Species registerSpecies(Species species) {
        return referenceGenomeManager.registerSpecies(species);

    }

    @PreAuthorize(HAS_ROLE_ADMIN_OR_REFERENCE_MANAGER)
    public Species unregisterSpecies(String speciesVersion) {
        return referenceGenomeManager.unregisterSpecies(speciesVersion);
    }

    //TODO
    @PreAuthorize(HAS_ROLE_USER)
    public List<Species> loadAllSpecies() {
        return referenceGenomeManager.loadAllSpecies();
    }

    @PreAuthorize(HAS_ROLE_ADMIN_OR_REFERENCE_MANAGER)
    public Reference updateSpecies(Long referenceId, String speciesVersion) {
        return referenceGenomeManager.updateSpecies(referenceId, speciesVersion);
    }
}
