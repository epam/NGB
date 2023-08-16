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
import com.epam.catgenome.manager.externaldb.target.AssociationExportField;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum DrugField implements AssociationExportField<DrugAssociation> {
    GENE_ID("Target", DrugAssociation::getGeneId, true),
    DRUG_ID(false),
    DRUG_NAME("Drug", o -> String.valueOf(o.getDrug().getName()), true),
    DRUG_NAME_FLTR(false),
    DRUG_TYPE("Type", DrugAssociation::getDrugType, true),
    MECHANISM_OF_ACTION("Mechanism Of Action", DrugAssociation::getMechanismOfAction, true),
    ACTION_TYPE("Action Type", DrugAssociation::getActionType, true),
    DISEASE_ID(false),
    DISEASE_NAME("Disease", o -> String.valueOf(o.getDisease().getName()), true),
    DISEASE_NAME_FLTR(false),
    PHASE("Phase", DrugAssociation::getPhase, true),
    STATUS("Status", DrugAssociation::getStatus, true),
    SOURCE("Source", o -> String.valueOf(o.getSource().getName()), true),
    SOURCE_URL(false);
    private String label;
    private Function<DrugAssociation, String> getter;
    private final boolean export;

    DrugField(boolean export) {
        this.export = export;
    }
}