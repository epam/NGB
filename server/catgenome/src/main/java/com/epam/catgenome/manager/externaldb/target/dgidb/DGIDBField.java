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
package com.epam.catgenome.manager.externaldb.target.dgidb;

import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.target.AssociationExportField;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@AllArgsConstructor
@Getter
public enum DGIDBField implements AssociationExportField<DGIDBDrugAssociation> {
    GENE_ID("Target ID", DGIDBDrugAssociation::getGeneId, FilterType.TERMS, true),
    DRUG_NAME("Drug", DGIDBDrugAssociation::getName, FilterType.PHRASE, true),
    DRUG_CLAIM_NAME("Drug Claim Name", DGIDBDrugAssociation::getClaimName, FilterType.PHRASE, true),
    INTERACTION_CLAIM_SOURCE("Interaction Claim Source",
            DGIDBDrugAssociation::getInteractionClaimSource, FilterType.TERMS, true),
    INTERACTION_TYPES("Interaction Types", DGIDBDrugAssociation::getInteractionTypes,
            FilterType.TERMS, true);
    private String label;
    private Function<DGIDBDrugAssociation, String> getter;
    private FilterType type;
    private final boolean export;
    DGIDBField(boolean export) {
        this.export = export;
    }
}
