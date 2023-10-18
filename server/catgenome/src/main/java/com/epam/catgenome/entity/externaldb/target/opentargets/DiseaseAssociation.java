/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.entity.externaldb.target.opentargets;

import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.epam.catgenome.entity.externaldb.target.Association;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseAssociation extends Association {
    private String geneSymbol;
    private String geneName;
    private AssociationType type;
    private Float score;
    private Float overallScore;
    private Float geneticAssociationScore;
    private Float somaticMutationScore;
    private Float knownDrugScore;
    private Float affectedPathwayScore;
    private Float literatureScore;
    private Float rnaExpressionScore;
    private Float animalModelScore;
    private List<HomologGroup> homologues;
    @Builder
    public DiseaseAssociation(String id, String name, String url, String geneId, String target,
                              String geneSymbol, String geneName, AssociationType type,
                              Float score, Float overallScore, Float geneticAssociationScore,
                              Float somaticMutationScore, Float knownDrugScore, Float affectedPathwayScore,
                              Float literatureScore, Float rnaExpressionScore, Float animalModelScore,
                              List<HomologGroup> homologues) {
        super(id, name, url, geneId, target);
        this.geneSymbol = geneSymbol;
        this.geneName = geneName;
        this.type = type;
        this.score = score;
        this.overallScore = overallScore;
        this.geneticAssociationScore = geneticAssociationScore;
        this.somaticMutationScore = somaticMutationScore;
        this.knownDrugScore = knownDrugScore;
        this.affectedPathwayScore = affectedPathwayScore;
        this.literatureScore = literatureScore;
        this.rnaExpressionScore = rnaExpressionScore;
        this.homologues = homologues;
    }
}
