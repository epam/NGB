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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.tmap.Compound;
import com.epam.catgenome.entity.tmap.TMapDrug;
import com.epam.catgenome.entity.tmap.TMapDrugField;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.TMapException;
import com.epam.catgenome.manager.export.ExportUtils;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIAuxiliaryManager;
import com.epam.catgenome.manager.externaldb.pug.NCBIPugManager;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugAssociationManager;
import com.epam.catgenome.util.FileFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TMapManager {
    private static final String PUB_CHEM_COMPOUND_DB = "pccompound";
    private static final Integer BATCH_SIZE = 50;
    private static final int RET_MAX = 500;
    private static final String CSV = ".csv";
    private final LaunchIdentificationManager launchIdentificationManager;
    private final PharmGKBDrugAssociationManager pharmGKBDrugAssociationManager;
    private final DGIDBDrugAssociationManager dgidbDrugAssociationManager;
    private final DrugAssociationManager drugAssociationManager;
    private final NCBIPugManager ncbiPugManager;
    private final String command;
    private final String tMapReportPath;
    private final String tMapReportName;
    private final NCBIAuxiliaryManager ncbiAuxiliaryManager;

    public TMapManager(LaunchIdentificationManager launchIdentificationManager,
                       PharmGKBDrugAssociationManager pharmGKBDrugAssociationManager,
                       DGIDBDrugAssociationManager dgidbDrugAssociationManager,
                       DrugAssociationManager drugAssociationManager,
                       NCBIPugManager ncbiPugManager,
                       NCBIAuxiliaryManager ncbiAuxiliaryManager,
                       final @Value("${target.drugs.tmap.command:}") String command,
                       final @Value("${target.drugs.tmap.report.path:}") String tMapReportPath,
                       final @Value("${target.drugs.tmap.report.name:TMAP.html}") String tMapReportName) {
        this.launchIdentificationManager = launchIdentificationManager;
        this.pharmGKBDrugAssociationManager = pharmGKBDrugAssociationManager;
        this.dgidbDrugAssociationManager = dgidbDrugAssociationManager;
        this.drugAssociationManager = drugAssociationManager;
        this.ncbiPugManager = ncbiPugManager;
        this.ncbiAuxiliaryManager = ncbiAuxiliaryManager;
        this.command = command;
        this.tMapReportPath = tMapReportPath;
        this.tMapReportName = tMapReportName;
    }

    public String generateTMapReport(final List<String> geneIds)
            throws IOException, ParseException, ExternalDbUnavailableException, InterruptedException, TMapException {
        final String folderName = geneIds.stream()
                .map(String::toLowerCase)
                .sorted()
                .collect(Collectors.joining("-"));
        final Path outputPath = Paths.get(tMapReportPath, folderName);
        final File outputFile = outputPath.toFile();
        if (!outputFile.exists()) {
            getTMapReport(generateCSV(geneIds), outputPath.toString());
        }
        return FilenameUtils.concat(folderName, tMapReportName);
    }

    public String generateTMapReport(final String diseaseId)
            throws IOException, ParseException, ExternalDbUnavailableException, InterruptedException, TMapException {
        final Path outputPath = Paths.get(tMapReportPath, diseaseId);
        final File outputFile = outputPath.toFile();
        if (!outputFile.exists()) {
            getTMapReport(generateCSV(diseaseId), outputPath.toString());
        }
        return FilenameUtils.concat(diseaseId, tMapReportName);
    }

    private String generateCSV(final List<String> geneIds)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final File tempFile = File.createTempFile(UUID.randomUUID().toString(), CSV);
        FileUtils.writeByteArrayToFile(tempFile, export(geneIds));
        return tempFile.toString();
    }

    private String generateCSV(final String diseaseId)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final File tempFile = File.createTempFile(UUID.randomUUID().toString(), CSV);
        FileUtils.writeByteArrayToFile(tempFile, export(diseaseId));
        return tempFile.toString();
    }

    private void getTMapReport(final String inputFilePath, final String outputFilePath)
            throws IOException, TMapException, InterruptedException {
        final String execCommand = String.format(command, inputFilePath, outputFilePath);
        final Process process = Runtime.getRuntime().exec(execCommand);
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            final BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            final StringBuilder result = new StringBuilder();
            String s;
            while ((s = stdError.readLine()) != null) {
                result.append(s);
                result.append('\n');
            }
            throw new TMapException(result.toString());
        }
    }

    private List<TMapDrug> getDrugs(final Long targetId, final List<String> geneIds)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final Map<String, TMapDrug> result = getDrugsByGeneIds(geneIds);
        final List<TMapDrug> drugs = getTMapDrugs(result);
        if (CollectionUtils.isNotEmpty(drugs)) {
            final Map<String, String> geneNames = launchIdentificationManager.getGeneNamesMap(targetId, geneIds);
            drugs.forEach(d -> {
                if (geneNames.containsKey(d.getGeneId().toLowerCase())) {
                    d.setGeneName(geneNames.get(d.getGeneId().toLowerCase()));
                }
            });
        }
        return drugs;
    }

    private List<TMapDrug> getTMapDrugs(final Map<String, TMapDrug> result)
            throws ExternalDbUnavailableException, JsonProcessingException {
        if (MapUtils.isEmpty(result)) {
            return Collections.emptyList();
        }
        final List<TMapDrug> drugs = new ArrayList<>();
        final List<Compound> compounds = getCompounds(new ArrayList<>(result.keySet()));
        if (CollectionUtils.isEmpty(compounds)) {
            return Collections.emptyList();
        }
        for (Map.Entry<String, TMapDrug> entry : result.entrySet()) {
            String k = entry.getKey();
            TMapDrug v = entry.getValue();
            Compound compound = null;
            for (Compound c : compounds) {
                if (c.getSynonyms().stream().anyMatch(s -> s.equalsIgnoreCase(k))) {
                    compound = c;
                }
            }
            if (compound != null) {
                v.setSmiles(compound.getSmiles());
                drugs.add(v);
            }
        }
        return drugs;
    }

    private Map<String, TMapDrug> getDrugsByGeneIds(final List<String> geneIds) throws ParseException, IOException {
        final List<PharmGKBDrug> pharmGKBDrugs = pharmGKBDrugAssociationManager.searchByGeneIds(geneIds);
        final List<DGIDBDrugAssociation> dgidbDrugs = dgidbDrugAssociationManager.searchByGeneIds(geneIds);
        final List<DrugAssociation> drugAssociations = drugAssociationManager.searchByGeneIds(geneIds);
        final Map<String, TMapDrug> result = new HashMap<>();
        pharmGKBDrugs.forEach(d -> result.put(d.getName().toLowerCase(), TMapDrug.builder()
                    .geneId(d.getGeneId().toLowerCase())
                    .drugId(d.getId())
                    .drugName(d.getName())
                    .build()));
        dgidbDrugs.forEach(d -> result.put(d.getName().toLowerCase(), TMapDrug.builder()
                .geneId(d.getGeneId().toLowerCase())
                .drugId(d.getId())
                .drugName(d.getName())
                .build()));
        drugAssociations.forEach(d -> result.put(d.getName().toLowerCase(), TMapDrug.builder()
                .geneId(d.getGeneId().toLowerCase())
                .drugId(d.getId())
                .drugName(d.getName())
                .build()));
        return result;
    }

    private List<TMapDrug> getDrugs(final String diseaseId)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final Map<String, TMapDrug> result = getDrugsByDiseaseId(diseaseId);
        return getTMapDrugs(result);
    }

    private Map<String, TMapDrug> getDrugsByDiseaseId(final String diseaseId) throws ParseException, IOException {
        final List<DrugAssociation> drugAssociations = drugAssociationManager.search(diseaseId);
        final Map<String, TMapDrug> result = new HashMap<>();
        drugAssociations.forEach(d -> result.put(d.getName().toLowerCase(), TMapDrug.builder()
                .geneId(d.getGeneId().toLowerCase())
                .geneName(d.getGeneSymbol())
                .drugId(d.getId())
                .drugName(d.getName())
                .build()));
        return result;
    }

    private List<Compound> getCompounds(final List<String> drugNames)
            throws ExternalDbUnavailableException, JsonProcessingException {
        final List<String> drugIds = getDrugIds(drugNames);
        if (CollectionUtils.isEmpty(drugIds)) {
            return Collections.emptyList();
        }
        final Map<String, String> smiles = ncbiPugManager.getSmiles(drugIds);
        final Map<String, List<String>> synonyms = ncbiPugManager.getSynonyms(drugIds);
        final List<Compound> compounds = new ArrayList<>();
        smiles.forEach((k, v) -> {
            Compound compound = Compound.builder()
                    .cid(k)
                    .smiles(v)
                    .build();
            if (synonyms.containsKey(k)) {
                compound.setSynonyms(synonyms.get(k));
            }
            compounds.add(compound);
        });
        return compounds;
    }

    private List<String> getDrugIds(final List<String> drugNames) throws ExternalDbUnavailableException {
        final List<String> drugIdsAll = new ArrayList<>();
        final List<List<String>> subSets = Lists.partition(drugNames, BATCH_SIZE);
        for (List<String> subSet : subSets) {
            String term = subSet.stream()
                    .map(d -> String.format("\"%s\"[CSYNO]", d))
                    .collect(Collectors.joining(" OR "));
            List<String> drugIds = ncbiAuxiliaryManager.searchDbForIds(PUB_CHEM_COMPOUND_DB,
                    term, null, RET_MAX);
            drugIdsAll.addAll(drugIds);
        }
        return drugIdsAll;
    }

    private byte[] export(final List<String> geneIds)
            throws IOException, ParseException, ExternalDbUnavailableException {
        return ExportUtils.export(getDrugs(null, geneIds), Arrays.asList(TMapDrugField.values()), FileFormat.CSV, true);
    }

    private byte[] export(final String diseaseId)
            throws IOException, ParseException, ExternalDbUnavailableException {
        return ExportUtils.export(getDrugs(diseaseId), Arrays.asList(TMapDrugField.values()), FileFormat.CSV, true);
    }
}
