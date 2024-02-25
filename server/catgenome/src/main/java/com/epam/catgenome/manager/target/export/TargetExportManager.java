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

import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.epam.catgenome.entity.externaldb.homolog.HomologType;
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.entity.externaldb.homologene.HomologeneEntry;
import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.pdb.PdbFile;
import com.epam.catgenome.entity.target.GeneRefSection;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.export.GeneSequence;
import com.epam.catgenome.entity.target.export.TargetHomologue;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.manager.externaldb.homolog.HomologManager;
import com.epam.catgenome.manager.externaldb.homologene.HomologeneManager;
import com.epam.catgenome.manager.externaldb.pdb.PdbEntriesManager;
import com.epam.catgenome.manager.externaldb.target.AssociationExportField;
import com.epam.catgenome.manager.externaldb.target.AssociationExportFieldDiseaseView;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugAssociationManager;
import com.epam.catgenome.manager.pdb.PdbFileManager;
import com.epam.catgenome.manager.target.LaunchIdentificationManager;
import com.epam.catgenome.manager.target.TargetManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.util.TextUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class TargetExportManager {

    private static final String TARGET_NAME = "%s (%s)";
    private static final String GENE_SYMBOL = "id: %s";
    private final PharmGKBDrugAssociationManager pharmGKBDrugAssociationManager;
    private final PharmGKBDiseaseAssociationManager pharmGKBDiseaseAssociationManager;
    private final DGIDBDrugAssociationManager dgidbDrugAssociationManager;
    private final DrugAssociationManager drugAssociationManager;
    private final DiseaseAssociationManager diseaseAssociationManager;
    private final PdbEntriesManager pdbEntriesManager;
    private final PdbFileManager pdbFileManager;
    private final TargetManager targetManager;
    private final LaunchIdentificationManager identificationManager;
    private final HomologManager homologManager;
    private final HomologeneManager homologeneManager;

    public static <T> List<AssociationExportField<T>> getAssociationFields(final AssociationExportField<T>[] array) {
        return Arrays.stream(array).filter(AssociationExportField::isExport).collect(Collectors.toList());
    }

    public static <T> List<AssociationExportFieldDiseaseView<T>> getAssociationFieldsDiseaseView(
            final AssociationExportFieldDiseaseView<T>[] array) {
        return Arrays.stream(array)
                .filter(AssociationExportFieldDiseaseView::isExportDiseaseView)
                .collect(Collectors.toList());
    }

    public List<TargetHomologue> getHomologyData(final List<String> genesOfInterest,
                                                 final List<String> translationalGenes,
                                                 final Map<String, String> geneNames)
            throws IOException, ParseException {
        final List<Long> species = CollectionUtils.isEmpty(translationalGenes) ?
                Collections.emptyList() :
                targetManager.getTargetGeneSpecies(translationalGenes);
        final Map<String, List<HomologGroup>> homologueGroups = homologManager.searchHomolog(genesOfInterest);
        final Map<String, List<HomologeneEntry>> homologenes = homologeneManager.searchHomologenes(genesOfInterest);
        final List<TargetHomologue> homology = new ArrayList<>();
        for (Map.Entry<String, List<HomologGroup>> homologueEntry : homologueGroups.entrySet()) {
            for (HomologGroup group : homologueEntry.getValue()) {
                List<Gene> genes = group.getHomologs();
                if (CollectionUtils.isNotEmpty(species)) {
                    genes = genes.stream()
                            .filter(g -> species.stream().anyMatch(s -> s.equals(g.getTaxId())))
                            .collect(Collectors.toList());
                }
                for (Gene gene: genes) {
                    TargetHomologue export = new TargetHomologue();
                    export.setGeneId(homologueEntry.getKey());
                    export.setTarget(geneNames.get(homologueEntry.getKey().toLowerCase()));
                    export.setSpecies(gene.getSpeciesScientificName());
                    export.setHomologyType(group.getType().getName());
                    export.setHomologue(TextUtils.isBlank(gene.getSymbol()) ?
                            String.format(GENE_SYMBOL, gene.getGeneId()) : gene.getSymbol());
                    export.setHomologueId(gene.getGeneId().toString());
                    export.setHomologyGroup(group.getProteinName());
                    export.setProtein(gene.getTitle());
                    export.setProteinLen(gene.getProtLen());
                    homology.add(export);
                }
            }
        }
        for (Map.Entry<String, List<HomologeneEntry>> homologenesMapEntry : homologenes.entrySet()) {
            for (HomologeneEntry entry : homologenesMapEntry.getValue()) {
                List<Gene> genes = entry.getGenes();
                if (CollectionUtils.isNotEmpty(species)) {
                    genes = genes.stream()
                            .filter(g -> species.stream().anyMatch(s -> s.equals(g.getTaxId())))
                            .collect(Collectors.toList());
                }
                for (Gene gene: genes) {
                    TargetHomologue export = new TargetHomologue();
                    export.setGeneId(homologenesMapEntry.getKey());
                    export.setTarget(geneNames.get(homologenesMapEntry.getKey().toLowerCase()));
                    export.setSpecies(gene.getSpeciesScientificName());
                    export.setHomologyType(HomologType.HOMOLOGUE.getName());
                    export.setHomologue(TextUtils.isBlank(gene.getSymbol()) ?
                            String.format(GENE_SYMBOL, gene.getGeneId()) : gene.getSymbol());
                    export.setHomologueId(gene.getGeneId().toString());
                    export.setHomologyGroup(entry.getCaption());
                    export.setProtein(gene.getTitle());
                    export.setProteinLen(gene.getProtLen());
                    homology.add(export);
                }
            }
        }
        return homology;
    }

    public long getHomologyCount(final List<String> genesOfInterest, final List<String> translationalGenes)
            throws IOException, ParseException {
        final List<Long> species = targetManager.getTargetGeneSpecies(translationalGenes);
        final Map<String, List<HomologGroup>> homologueGroups = homologManager.searchHomolog(genesOfInterest);
        final Map<String, List<HomologeneEntry>> homologenes = homologeneManager.searchHomologenes(genesOfInterest);
        long homology = 0;
        for (Map.Entry<String, List<HomologGroup>> homologueEntry : homologueGroups.entrySet()) {
            for (HomologGroup group : homologueEntry.getValue()) {
                List<Gene> genes = group.getHomologs().stream()
                        .filter(g -> species.stream().anyMatch(s -> s.equals(g.getTaxId())))
                        .collect(Collectors.toList());
                homology += genes.size();
            }
        }
        for (Map.Entry<String, List<HomologeneEntry>> homologenesMapEntry : homologenes.entrySet()) {
            for (HomologeneEntry entry : homologenesMapEntry.getValue()) {
                List<Gene> genes = entry.getGenes().stream()
                        .filter(g -> species.stream().anyMatch(s -> s.equals(g.getTaxId())))
                        .collect(Collectors.toList());
                homology += genes.size();
            }
        }
        return homology;
    }

    public Map<String, String> getTargetGeneNames(final List<String> geneIds) throws ParseException, IOException {
        final Map<String, String> genesMap = new HashMap<>();
        final List<TargetGene> genes = targetManager.getTargetGenes(geneIds);
        for (TargetGene gene: genes) {
            genesMap.put(gene.getGeneId().toLowerCase(),
                    String.format(TARGET_NAME, gene.getGeneName(), gene.getSpeciesName()));
        }
        return genesMap;
    }

    public Map<String, String> getTargetGeneNames(final String geneId) throws ParseException, IOException {
        final Map<String, String> genesMap = new HashMap<>();
        final List<String> geneNames = identificationManager.getGeneNames(Collections.singletonList(geneId));
        for (String geneName: geneNames) {
            genesMap.put(geneId.toLowerCase(), geneName);
        }
        return genesMap;
    }

    public Map<String, String> getTargetGeneNames(final Map<String, TargetGene> genes) {
        final Map<String, String> genesMap = new HashMap<>();
        genes.forEach((k, v) -> {
            genesMap.put(k, String.format(TARGET_NAME, v.getGeneName(), v.getSpeciesName()));
        });
        return genesMap;
    }

    public Map<String, TargetGene> getTargetGenesMap(final List<String> geneIds) throws ParseException, IOException {
        final Map<String, TargetGene> genesMap = new HashMap<>();
        final List<TargetGene> genes = targetManager.getTargetGenes(geneIds);
        for (TargetGene gene: genes) {
            genesMap.put(gene.getGeneId().toLowerCase(), gene);
        }
        return genesMap;
    }

    public List<DiseaseAssociation> getDiseaseAssociations(final List<String> geneIds,
                                                           final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<DiseaseAssociation> diseaseAssociations = diseaseAssociationManager.search(geneIds);
        if (MapUtils.isNotEmpty(genesMap)) {
            diseaseAssociations.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        }
        return diseaseAssociations;
    }

    public List<DrugAssociation> getDrugAssociations(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<DrugAssociation> drugAssociations = drugAssociationManager.search(geneIds);
        if (MapUtils.isNotEmpty(genesMap)) {
            drugAssociations.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        }
        return drugAssociations;
    }

    public List<PharmGKBDisease> getPharmGKBDiseases(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<PharmGKBDisease> pharmGKBDiseases = pharmGKBDiseaseAssociationManager.search(geneIds);
        if (MapUtils.isNotEmpty(genesMap)) {
            pharmGKBDiseases.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        }
        return pharmGKBDiseases;
    }

    public List<PharmGKBDrug> getPharmGKBDrugs(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<PharmGKBDrug> pharmGKBDrugs = pharmGKBDrugAssociationManager.search(geneIds);
        if (MapUtils.isNotEmpty(genesMap)) {
            pharmGKBDrugs.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        }
        return pharmGKBDrugs;
    }

    public List<DGIDBDrugAssociation> getDGIDBDrugs(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<DGIDBDrugAssociation> dgidbDrugAssociations = dgidbDrugAssociationManager.search(geneIds);
        if (MapUtils.isNotEmpty(genesMap)) {
            dgidbDrugAssociations.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        }
        return dgidbDrugAssociations;
    }

    public List<Structure> getStructures(final List<String> geneIds) throws ParseException, IOException {
        final List<String> geneNames = identificationManager.getGeneNames(geneIds);
        return pdbEntriesManager.getAllStructures(geneNames);
    }

    public List<PdbFile> getPdbFiles(final List<String> geneIds) {
        return pdbFileManager.load(geneIds);
    }

    public List<GeneSequence> getSequenceTable(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final List<GeneRefSection> sequencesTable = identificationManager.getGeneSequencesTable(geneIds,
                false, true, true);
        final List<GeneSequence> result = new ArrayList<>();
        for (GeneRefSection geneRefSection : sequencesTable) {
            for (com.epam.catgenome.entity.target.GeneSequence sequence :
                    Optional.ofNullable(geneRefSection.getSequences()).orElse(Collections.emptyList())){
                GeneSequence sequenceExport = new GeneSequence();
                sequenceExport.setGeneId(geneRefSection.getGeneId());
                if (MapUtils.isNotEmpty(genesMap)) {
                    sequenceExport.setTarget(genesMap.get(geneRefSection.getGeneId().toLowerCase()));
                }
                if (geneRefSection.getReference() != null) {
                    sequenceExport.setReference(geneRefSection.getReference().getId());
                }
                if (sequence.getMRNA() != null) {
                    sequenceExport.setMRNA(sequence.getMRNA().getId());
                    sequenceExport.setMRNALength(sequence.getMRNA().getLength());
                }
                if (sequence.getProtein() != null) {
                    sequenceExport.setProtein(sequence.getProtein().getId());
                    sequenceExport.setProteinLength(sequence.getProtein().getLength());
                    sequenceExport.setProteinName(sequence.getProtein().getName());
                }
                result.add(sequenceExport);
            }
        }
        return result;
    }

    public static List<String> getGeneIds(final List<String> genesOfInterest, final List<String> translationalGenes) {
        return Stream.concat(genesOfInterest.stream(),
                        Optional.ofNullable(translationalGenes).orElse(Collections.emptyList()).stream())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }
}
