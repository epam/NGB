/*
 * MIT License
 *
 * Copyright (c) 2024 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.target.ttd;

import com.epam.catgenome.entity.externaldb.target.ttd.TTDDrugAssociation;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.target.AssociationExportField;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum TTDDrugField implements AssociationExportField<TTDDrugAssociation> {
    GENE_ID("Target ID", TTDDrugAssociation::getGeneId, FilterType.TERM, false),
    TARGET("Target", TTDDrugAssociation::getTarget, FilterType.TERM, true),
    TTD_GENE_ID("TTD Target ID", TTDDrugAssociation::getTtdGeneId, FilterType.TERM, false),
    TTD_TARGET("TTD Target", TTDDrugAssociation::getTtdTarget, FilterType.OPTIONS, true),
    TTD_TARGET_CI("TTD Target Case Insensitive", t -> t.getTtdTarget().toLowerCase(), FilterType.NONE, false),
    DRUG_NAME("Drug", TTDDrugAssociation::getName, FilterType.PHRASE, true),
    DRUG_ID(false),
    COMPANY("Company", TTDDrugAssociation::getCompany, FilterType.OPTIONS, true),
    TYPE("Type", TTDDrugAssociation::getType, FilterType.OPTIONS, true),
    THERAPEUTIC_CLASS("Therapeutic Class", TTDDrugAssociation::getTherapeuticClass, FilterType.OPTIONS, true),
    INCHI("InChI", TTDDrugAssociation::getInChI, FilterType.PHRASE, true),
    INCHI_KEY("InChIKey", TTDDrugAssociation::getInChIKey, FilterType.PHRASE, true),
    CANONICAL_SMILES("Canonical SMILES", TTDDrugAssociation::getCanonicalSmiles, FilterType.PHRASE, true),
    STATUS("Status", TTDDrugAssociation::getStatus, FilterType.OPTIONS, true),
    COMPOUND_CLASS("Compound Class", TTDDrugAssociation::getCompoundClass, FilterType.OPTIONS, true);
    private String label;
    private Function<TTDDrugAssociation, String> getter;
    private FilterType type;
    private final boolean export;

    TTDDrugField(boolean export) {
        this.export = export;
    }
}
