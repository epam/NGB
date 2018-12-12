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

package com.epam.catgenome.manager.protein;

import com.epam.catgenome.controller.gene.ProteinSequenceVariationQuery;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.protein.MrnaProteinSequenceVariants;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.protein.ProteinSequenceInfo;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.GeneReadingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.epam.catgenome.security.acl.SecurityExpressions.OR;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;

@Service
public class ProteinSequenceSecurityService {

    private static final String READ_PERMISSION_FOR_GENE_FILE_BY_TRACK = ROLE_ADMIN + OR +
            "readOnGeneFileIsAllowed(#genes.id, #genes.projectId)";

    private static final String READ_PERMISSION_FOR_GENE_FILE_BY_QUERY = ROLE_ADMIN + OR +
            "readOnGeneFileIsAllowed(#psVariationQuery.trackQuery.id, #psVariationQuery.trackQuery.projectId)";

    @Autowired
    private ProteinSequenceManager proteinSequenceManager;

    @PreAuthorize(READ_PERMISSION_FOR_GENE_FILE_BY_TRACK)
    public Map<Gene, List<ProteinSequenceEntry>> loadProteinSequenceWithoutGrouping(
            Track<Gene> genes, Long referenceId, boolean collapsed) throws GeneReadingException {
        return proteinSequenceManager.loadProteinSequenceWithoutGrouping(genes, referenceId, collapsed);
    }

    @PreAuthorize(READ_PERMISSION_FOR_GENE_FILE_BY_TRACK)
    public Track<ProteinSequenceInfo> loadProteinSequence(Track<Gene> genes, Long referenceId)
            throws GeneReadingException {
        return proteinSequenceManager.loadProteinSequence(genes, referenceId);
    }

    @PreAuthorize(READ_PERMISSION_FOR_GENE_FILE_BY_QUERY)
    public Track<MrnaProteinSequenceVariants> loadProteinSequenceWithVariations(
            ProteinSequenceVariationQuery psVariationQuery, Long referenceId) throws GeneReadingException {
        return proteinSequenceManager.loadProteinSequenceWithVariations(psVariationQuery, referenceId);
    }
}
