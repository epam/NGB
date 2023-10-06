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
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.target.AssociationExportFieldDiseaseView;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum DiseaseField implements AssociationExportFieldDiseaseView<DiseaseAssociation> {
    GENE_ID("Target ID", DiseaseAssociation::getGeneId, FilterType.TERM, true, false),
    GENE_SYMBOL("Target", DiseaseAssociation::getGeneSymbol,  FilterType.PHRASE, false, true),

    GENE_NAME("Target Name", DiseaseAssociation::getGeneName,  FilterType.PHRASE,
            false, true),
    DISEASE_ID(false, false),
    DISEASE_NAME("Disease", DiseaseAssociation::getDiseaseName, FilterType.PHRASE,
            true, false),
    OVERALL_SCORE("Overall score", o -> String.valueOf(o.getOverallScore()), FilterType.NONE,
            true, true),
    GENETIC_ASSOCIATIONS_SCORE("Genetic associations", o -> String.valueOf(o.getGeneticAssociationScore()),
            FilterType.NONE, true, true),
    SOMATIC_MUTATIONS_SCORE("Somatic mutations", o -> String.valueOf(o.getSomaticMutationScore()),
            FilterType.NONE, true, true),
    DRUGS_SCORE("Drugs", o -> String.valueOf(o.getKnownDrugScore()), FilterType.NONE,
            true, true),
    PATHWAYS_SCORE("Pathways systems", o -> String.valueOf(o.getAffectedPathwayScore()),
            FilterType.NONE, true, true),
    TEXT_MINING_SCORE("Text mining", o -> String.valueOf(o.getLiteratureScore()), FilterType.NONE,
            true, true),
    ANIMAL_MODELS_SCORE("Animal models", o -> String.valueOf(o.getAnimalModelScore()),
            FilterType.NONE, true, true),
    RNA_EXPRESSION_SCORE("RNA expression", o -> String.valueOf(o.getRnaExpressionScore()),
            FilterType.NONE, true, true);
    private String label;
    private Function<DiseaseAssociation, String> getter;
    private FilterType type;
    private final boolean export;
    private final boolean exportDiseaseView;

    DiseaseField(boolean export, boolean exportDiseaseView) {
        this.export = export;
        this.exportDiseaseView = exportDiseaseView;
    }
}
