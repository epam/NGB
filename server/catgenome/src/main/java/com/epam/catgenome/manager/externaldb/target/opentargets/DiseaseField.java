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
package com.epam.catgenome.manager.externaldb.target.opentargets;

import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.manager.externaldb.target.AssociationExportField;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum DiseaseField implements AssociationExportField<DiseaseAssociation> {
    GENE_ID("Target", DiseaseAssociation::getGeneId, true),
    DISEASE_ID(false),
    DISEASE_NAME_FLTR(false),
    DISEASE_NAME("Disease", DiseaseAssociation::getDiseaseName, true),
    OVERALL_SCORE("Overall score", o -> String.valueOf(o.getOverallScore()), true),
    GENETIC_ASSOCIATIONS_SCORE("Genetic associations", o -> String.valueOf(o.getGeneticAssociationScore()), true),
    SOMATIC_MUTATIONS_SCORE("Somatic mutations", o -> String.valueOf(o.getSomaticMutationScore()), true),
    DRUGS_SCORE("Drugs", o -> String.valueOf(o.getKnownDrugScore()), true),
    PATHWAYS_SCORE("Pathways systems", o -> String.valueOf(o.getAffectedPathwayScore()), true),
    TEXT_MINING_SCORE("Text mining", o -> String.valueOf(o.getLiteratureScore()), true),
    ANIMAL_MODELS_SCORE("Animal models", o -> String.valueOf(o.getAnimalModelScore()), true),
    RNA_EXPRESSION_SCORE("RNA expression", o -> String.valueOf(o.getRnaExpressionScore()), true);
    private String label;
    private Function<DiseaseAssociation, String> getter;
    private final boolean export;

    DiseaseField(boolean export) {
        this.export = export;
    }
}
