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
package com.epam.catgenome.entity.target.export.xls;

import com.epam.catgenome.manager.export.ExportField;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@AllArgsConstructor
@Getter
public enum TargetExportSummaryField implements ExportField<TargetExportSummary> {
    GENE("Gene", TargetExportSummary::getGene),
    GENE_ID("Gene ID", TargetExportSummary::getGeneId),
    SPECIES("Species", TargetExportSummary::getSpecies),
    TYPE("Type", TargetExportSummary::getType),
    DESCRIPTION("Description", TargetExportSummary::getDescription),
    KNOWN_DRUGS("Known Drugs", o -> String.valueOf(o.getKnownDrugs())),
    KNOWN_DRUG_RECORDS("Known Drug Records", o -> String.valueOf(o.getKnownDrugRecords())),
    DISEASES("Diseases", o -> String.valueOf(o.getDiseases())),
    PUBLICATIONS("Publications", o -> String.valueOf(o.getPublications())),
    SEQUENCES("Sequences", TargetExportSummary::getSequences),
    STRUCTURES("Structures", o -> String.valueOf(o.getStructures())),
    HOMOLOGS("Homologs", o -> String.valueOf(o.getHomologs()));
    private String label;
    private Function<TargetExportSummary, String> getter;
}
