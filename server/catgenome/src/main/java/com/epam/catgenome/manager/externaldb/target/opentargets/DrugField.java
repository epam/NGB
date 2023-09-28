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

import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.target.AssociationExportFieldDiseaseView;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum DrugField implements AssociationExportFieldDiseaseView<DrugAssociation> {
    GENE_ID("Target ID", DrugAssociation::getGeneId, FilterType.TERMS, true, false),
    GENE_SYMBOL("Target", DrugAssociation::getGeneSymbol,  FilterType.TERMS, false, true),
    GENE_NAME("Target Description", DrugAssociation::getGeneName,  FilterType.NONE,
            false, true),
    DRUG_ID(false, false),
    DRUG_NAME("Drug", o -> String.valueOf(o.getDrug().getName()), FilterType.PHRASE,
            true, true),
    DRUG_TYPE("Type", DrugAssociation::getDrugType, FilterType.TERMS, true, true),
    MECHANISM_OF_ACTION("Mechanism Of Action", DrugAssociation::getMechanismOfAction, FilterType.TERMS,
            true, true),
    ACTION_TYPE("Action Type", DrugAssociation::getActionType, FilterType.TERMS, true, true),
    DISEASE_ID(false, false),
    DISEASE_NAME("Disease", o -> String.valueOf(o.getDisease().getName()), FilterType.PHRASE,
            true, false),
    PHASE("Phase", DrugAssociation::getPhase, FilterType.TERMS, true, true),
    STATUS("Status", DrugAssociation::getStatus, FilterType.TERMS, true, true),
    SOURCE("Source", o -> String.valueOf(o.getSource().getName()), FilterType.TERMS,
            true, true),
    SOURCE_URL(false, false);
    private String label;
    private Function<DrugAssociation, String> getter;
    private FilterType type;
    private final boolean export;
    private final boolean exportDiseaseView;

    DrugField(boolean export, boolean exportDiseaseView) {
        this.export = export;
        this.exportDiseaseView = exportDiseaseView;
    }
}
