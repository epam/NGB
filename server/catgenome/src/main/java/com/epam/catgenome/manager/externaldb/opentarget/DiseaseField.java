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

package com.epam.catgenome.manager.externaldb.opentarget;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DiseaseField {
    GENE_ID("geneId"),
    DISEASE_NAME("diseaseName"),
    OVERALL_SCORE("overall_score"),
    GENETIC_ASSOCIATIONS_SCORE("genetic_association_score"),
    SOMATIC_MUTATIONS_SCORE("somatic_mutation_score"),
    DRUGS_SCORE("known_drug_score"),
    PATHWAYS_SCORE("affected_pathway_score"),
    TEXT_MINING_SCORE("literature_score"),
    RNA_EXPRESSION_SCORE("rna_expression_score"),
    ANIMAL_MODELS_SCORE("animal_model_score");
    private final String name;

    public static String getDefault() {
        return DiseaseField.OVERALL_SCORE.getName();
    }
}
