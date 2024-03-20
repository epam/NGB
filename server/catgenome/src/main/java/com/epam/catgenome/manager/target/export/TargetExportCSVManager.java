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
package com.epam.catgenome.manager.target.export;

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.export.ExportUtils;
import com.epam.catgenome.manager.externaldb.pdb.PdbStructureField;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBField;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseField;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugField;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDiseaseField;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugField;
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDiseaseField;
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDrugField;
import com.epam.catgenome.manager.pdb.PdbFileField;
import com.epam.catgenome.entity.target.export.GeneSequenceField;
import com.epam.catgenome.entity.target.export.TargetHomologyField;
import com.epam.catgenome.manager.target.LaunchIdentificationManager;
import com.epam.catgenome.util.FileFormat;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.manager.target.LaunchIdentificationManager.getGeneIds;
import static com.epam.catgenome.manager.target.export.TargetExportManager.*;


@Service
@RequiredArgsConstructor
public class TargetExportCSVManager {

    private final DrugAssociationManager drugAssociationManager;
    private final DiseaseAssociationManager diseaseAssociationManager;
    private final TargetExportManager targetExportManager;
    private final LaunchIdentificationManager identificationManager;

    public byte[] exportDisease(final String diseaseId, final TargetExportTable source,
                                final FileFormat format, final boolean includeHeader)
            throws ParseException, IOException {
        byte[] result = null;
        switch (source) {
            case OPEN_TARGETS_DISEASES:
                result = ExportUtils.export(diseaseAssociationManager.search(diseaseId),
                        getAssociationFieldsDiseaseView(DiseaseField.values()), format, includeHeader);
                break;
            case OPEN_TARGETS_DRUGS:
                result = ExportUtils.export(drugAssociationManager.search(diseaseId),
                        getAssociationFieldsDiseaseView(DrugField.values()), format, includeHeader);
                break;
            default:
                break;
        }
        return result;
    }

    public byte[] export(final Long targetId,
                         final List<String> genesOfInterest,
                         final List<String> translationalGenes,
                         final FileFormat format,
                         final TargetExportTable source,
                         final boolean includeHeader)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final List<String> geneIds = getGeneIds(genesOfInterest, translationalGenes);
        final List<String> expandedGeneIds = identificationManager.getExpandedGeneIds(targetId, geneIds, true);
        final Map<String, String> genesMap = targetExportManager.getTargetGeneNames(targetId, geneIds);
        return export(targetId, geneIds, expandedGeneIds, genesOfInterest, translationalGenes,
                format, source, includeHeader, genesMap);
    }

    public byte[] exportGene(final String geneId, final TargetExportTable source,
                             final FileFormat format, final boolean includeHeader)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final Map<String, String> genesMap = targetExportManager.getTargetGeneNames(null, geneId);
        return export(null,
                Collections.singletonList(geneId),
                Collections.singletonList(geneId),
                Collections.singletonList(geneId),
                Collections.emptyList(),
                format, source, includeHeader, genesMap);
    }

    private byte[] export(final Long targetId,
                          final List<String> geneIds,
                          final List<String> expandedGeneIds,
                          final List<String> genesOfInterest,
                          final List<String> translationalGenes,
                          final FileFormat format,
                          final TargetExportTable source,
                          final boolean includeHeader,
                          final Map<String, String> genesMap)
            throws IOException, ParseException, ExternalDbUnavailableException {
        byte[] result = null;
        switch (source) {
            case OPEN_TARGETS_DISEASES:
                result = ExportUtils.export(targetExportManager.getDiseaseAssociations(expandedGeneIds, genesMap),
                        getAssociationFields(DiseaseField.values()), format, includeHeader);
                break;
            case OPEN_TARGETS_DRUGS:
                result = ExportUtils.export(targetExportManager.getDrugAssociations(expandedGeneIds, genesMap),
                        getAssociationFields(DrugField.values()), format, includeHeader);
                break;
            case PHARM_GKB_DISEASES:
                result = ExportUtils.export(targetExportManager.getPharmGKBDiseases(expandedGeneIds, genesMap),
                        getAssociationFields(PharmGKBDiseaseField.values()), format, includeHeader);
                break;
            case PHARM_GKB_DRUGS:
                result = ExportUtils.export(targetExportManager.getPharmGKBDrugs(expandedGeneIds, genesMap),
                        getAssociationFields(PharmGKBDrugField.values()), format, includeHeader);
                break;
            case DGIDB_DRUGS:
                result = ExportUtils.export(targetExportManager.getDGIDBDrugs(expandedGeneIds, genesMap),
                        getAssociationFields(DGIDBField.values()), format, includeHeader);
                break;
            case TTD_DISEASES:
                result = ExportUtils.export(targetExportManager.getTTDDiseases(targetId, expandedGeneIds, genesMap),
                        getAssociationFields(TTDDiseaseField.values()), format, includeHeader);
                break;
            case TTD_DRUGS:
                result = ExportUtils.export(targetExportManager.getTTDDrugs(targetId, expandedGeneIds, genesMap),
                        getAssociationFields(TTDDrugField.values()), format, includeHeader);
                break;
            case STRUCTURES:
                result = ExportUtils.export(targetExportManager.getStructures(targetId, expandedGeneIds),
                        Arrays.asList(PdbStructureField.values()), format, includeHeader);
                break;
            case LOCAL_PDBS:
                result = ExportUtils.export(targetExportManager.getPdbFiles(expandedGeneIds),
                        Arrays.asList(PdbFileField.values()), format, includeHeader);
                break;
            case SEQUENCES:
                result = ExportUtils.export(targetExportManager.getSequenceTable(targetId, geneIds, genesMap),
                        Arrays.asList(GeneSequenceField.values()), format, includeHeader);
                break;
            case HOMOLOGY:
                result = ExportUtils.export(targetExportManager.getHomologyData(targetId, genesOfInterest,
                                translationalGenes, genesMap),
                        Arrays.asList(TargetHomologyField.values()), format, includeHeader);
                break;
            default:
                break;
        }
        return result;
    }
}
