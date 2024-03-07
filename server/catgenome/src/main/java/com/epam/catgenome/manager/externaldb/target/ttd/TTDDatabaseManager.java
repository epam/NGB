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

import com.epam.catgenome.entity.externaldb.target.ttd.TTDDiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDrugAssociation;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.manager.sequence.SequencesManager;
import com.epam.catgenome.manager.target.ParasiteProteinsManager;
import com.epam.catgenome.manager.target.TargetManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;


@Service
@RequiredArgsConstructor
@Slf4j
public class TTDDatabaseManager {

    @Value("${targets.parasite.proteins.directory:}")
    private String parasiteProteinsDirectory;

    private final TTDDrugAssociationManager ttdDrugAssociationManager;
    private final TTDDiseaseAssociationManager ttdDiseaseAssociationManager;
    private final ParasiteProteinsManager parasiteProteinsManager;
    private final TargetManager targetManager;
    private final SequencesManager sequencesManager;

    public void importData(final String drugsPath, final String targetsPath, final String diseasesPath)
            throws IOException, ParseException {
        ttdDrugAssociationManager.importData(drugsPath, targetsPath);
        ttdDiseaseAssociationManager.importData(diseasesPath);
    }

    public SearchResult<TTDDrugAssociation> getTTDDrugs(final AssociationSearchRequest request,
                                                        final Map<String, Long> targetGenes)
            throws ParseException, IOException, BlastRequestException, InterruptedException {
        final List<TTDDrugAssociation> result = new ArrayList<>();
        for (String geneId : request.getGeneIds()) {
            List<String> geneNames = targetManager.getTargetGeneNames(Collections.singletonList(geneId));
            List<TTDDrugAssociation> byGeneNames = CollectionUtils.isEmpty(geneNames) ?
                    Collections.emptyList() :
                    ttdDrugAssociationManager.searchByField(geneNames,
                            TTDDrugField.TTD_TARGET.name(), request.getFilters());
            if (CollectionUtils.isNotEmpty(byGeneNames)) {
                byGeneNames.forEach(i -> i.setGeneId(geneId));
                result.addAll(byGeneNames);
            } else {
                List<String> blastSequences = getBlastSequences(targetGenes, geneId);
                if (CollectionUtils.isNotEmpty(blastSequences)) {
                    List<TTDDrugAssociation> bySequences = ttdDrugAssociationManager.searchByField(blastSequences,
                            TTDDrugField.TTD_TARGET.name(), request.getFilters());
                    if (CollectionUtils.isNotEmpty(bySequences)) {
                        bySequences.forEach(i -> i.setGeneId(geneId));
                        result.addAll(bySequences);
                    }
                }
            }
        }
        return convertDrugs(request, result);
    }

    public SearchResult<TTDDiseaseAssociation> getTTDDiseases(final AssociationSearchRequest request,
                                                              final Map<String, Long> targetGenes)
            throws ParseException, IOException, BlastRequestException, InterruptedException {
        final List<TTDDiseaseAssociation> result = new ArrayList<>();
        for (String geneId : request.getGeneIds()) {
            List<String> geneNames = targetManager.getTargetGeneNames(Collections.singletonList(geneId));
            List<TTDDiseaseAssociation> byGeneNames = CollectionUtils.isEmpty(geneNames) ?
                    Collections.emptyList() :
                    ttdDiseaseAssociationManager.searchByField(geneNames,
                            TTDDiseaseField.TTD_TARGET.name(), request.getFilters());
            if (CollectionUtils.isNotEmpty(byGeneNames)) {
                byGeneNames.forEach(i -> i.setGeneId(geneId));
                result.addAll(byGeneNames);
            } else {
                List<String> blastSequences = getBlastSequences(targetGenes, geneId);
                if (CollectionUtils.isNotEmpty(blastSequences)) {
                    List<TTDDiseaseAssociation> bySequences = ttdDiseaseAssociationManager.searchByField(
                            blastSequences, TTDDiseaseField.TTD_TARGET.name(), request.getFilters());
                    if (CollectionUtils.isNotEmpty(bySequences)) {
                        bySequences.forEach(i -> i.setGeneId(geneId));
                        result.addAll(bySequences);
                    }
                }
            }
        }
        return convertDiseases(request, result);
    }

    public List<TTDDrugAssociation> getTTDDrugs(final List<String> geneIds, final Map<String, Long> targetGenes)
            throws ParseException, IOException, BlastRequestException, InterruptedException {
        final List<TTDDrugAssociation> result = new ArrayList<>();
        for (String geneId : geneIds) {
            List<String> geneNames = targetManager.getTargetGeneNames(Collections.singletonList(geneId));
            List<TTDDrugAssociation> byGeneNames = CollectionUtils.isEmpty(geneNames) ?
                    Collections.emptyList() :
                    ttdDrugAssociationManager.searchByField(geneNames, TTDDrugField.TTD_TARGET.name());
            if (CollectionUtils.isNotEmpty(byGeneNames)) {
                byGeneNames.forEach(i -> i.setGeneId(geneId));
                result.addAll(byGeneNames);
            } else {
                List<String> blastSequences = getBlastSequences(targetGenes, geneId);
                if (CollectionUtils.isNotEmpty(blastSequences)) {
                    List<TTDDrugAssociation> bySequences = ttdDrugAssociationManager.search(
                            blastSequences, TTDDrugField.TTD_GENE_ID.name());
                    if (CollectionUtils.isNotEmpty(bySequences)) {
                        bySequences.forEach(i -> i.setGeneId(geneId));
                        result.addAll(bySequences);
                    }
                }
            }
        }
        return result;
    }

    public TTDDrugFieldValues getDrugFieldValues(final List<String> geneIds, final Map<String, Long> targetGenes)
            throws IOException, ParseException, BlastRequestException, InterruptedException {
        final List<TTDDrugAssociation> result = getTTDDrugs(geneIds, targetGenes);
        final List<String> companies = result.stream()
                .map(TTDDrugAssociation::getCompany)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> types = result.stream()
                .map(TTDDrugAssociation::getType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> therapeuticClasses = result.stream()
                .map(TTDDrugAssociation::getTherapeuticClass)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> statuses = result.stream()
                .map(TTDDrugAssociation::getStatus)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> compoundClasses = result.stream()
                .map(TTDDrugAssociation::getCompoundClass)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return TTDDrugFieldValues.builder()
                .companies(companies)
                .types(types)
                .therapeuticClasses(therapeuticClasses)
                .statuses(statuses)
                .compoundClasses(compoundClasses)
                .build();
    }

    public List<TTDDiseaseAssociation> getTTDDiseases(final List<String> geneIds, final Map<String, Long> targetGenes)
            throws ParseException, IOException, BlastRequestException, InterruptedException {
        final List<TTDDiseaseAssociation> result = new ArrayList<>();
        for (String geneId : geneIds) {
            List<String> geneNames = targetManager.getTargetGeneNames(Collections.singletonList(geneId));
            List<TTDDiseaseAssociation> byGeneNames = CollectionUtils.isEmpty(geneNames) ?
                    Collections.emptyList() :
                    ttdDiseaseAssociationManager.searchByField(geneNames, TTDDiseaseField.TTD_TARGET.name());
            if (CollectionUtils.isNotEmpty(byGeneNames)) {
                byGeneNames.forEach(i -> i.setGeneId(geneId));
                result.addAll(byGeneNames);
            } else {
                List<String> blastSequences = getBlastSequences(targetGenes, geneId);
                if (CollectionUtils.isNotEmpty(blastSequences)) {
                    List<TTDDiseaseAssociation> bySequences = ttdDiseaseAssociationManager.searchByField(
                            blastSequences, TTDDiseaseField.TTD_GENE_ID.name());
                    if (CollectionUtils.isNotEmpty(bySequences)) {
                        bySequences.forEach(i -> i.setGeneId(geneId));
                        result.addAll(bySequences);
                    }
                }
            }
        }
        return result;
    }

    public TTDDiseseFieldValues getDiseaseFieldValues(final List<String> geneIds, final Map<String, Long> targetGenes)
            throws IOException, ParseException, BlastRequestException, InterruptedException {
        final List<TTDDiseaseAssociation> result = getTTDDiseases(geneIds, targetGenes);
        final List<String> phases = result.stream()
                .map(TTDDiseaseAssociation::getClinicalStatus)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return TTDDiseseFieldValues.builder()
                .phases(phases)
                .build();
    }

    private List<String> getBlastSequences(final Map<String, Long> targetGenes, final String geneId)
            throws IOException, InterruptedException, BlastRequestException {
        List<String> blastSequences = Collections.emptyList();
        final String cashFilePath = getCashFilePath(geneId);
        final File cashFile = new File(cashFilePath);
        if (cashFile.exists()) {
            blastSequences = getBlastSequences(cashFile);
        } else {
            if (targetGenes.containsKey(geneId)) {
                blastSequences = getBlastSequences(geneId, targetGenes.get(geneId));
                cashSequences(blastSequences, cashFile);
            }
        }
        return blastSequences;
    }

    private void cashSequences(final List<String> blastSequences, final File cashFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cashFile, true))) {
            for (String s : blastSequences) {
                writer.write(s);
                writer.newLine();
            }
        }
    }

    private List<String> getBlastSequences(final File cashFile) throws IOException {
        final List<String> sequences = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(cashFile); BufferedReader bufferedReader = new BufferedReader(reader)) {
            while ((line = bufferedReader.readLine()) != null) {
                sequences.add(line);
            }
        }
        return sequences;
    }

    private List<String> getBlastSequences(final String geneId, final long taxId)
            throws IOException, InterruptedException, BlastRequestException {
        final List<String> sequences = sequencesManager.getGeneProteins(geneId, taxId);
        final List<String> blastSequences = new ArrayList<>();
        for (String s : sequences) {
            blastSequences.addAll(parasiteProteinsManager.getSequences(s));
        }
        return blastSequences;
    }

    private static SearchResult<TTDDrugAssociation> convertDrugs(final AssociationSearchRequest request,
                                                                 final List<TTDDrugAssociation> result) {
        result.sort((p1, p2) -> {
            CompareToBuilder builder = new CompareToBuilder();
            if (CollectionUtils.isEmpty(request.getOrderInfos())) {
                builder.append(p1.getTarget(), p2.getTarget());
            } else {
                request.getOrderInfos().forEach(f -> {
                    Function<TTDDrugAssociation, String> func = TTDDrugField.valueOf(f.getOrderBy()).getGetter();
                    if (f.isReverse()) {
                        builder.append(func.apply(p2), func.apply(p1));
                    } else {
                        builder.append(func.apply(p1), func.apply(p2));
                    }
                });
            }
            return builder.toComparison();
        });
        return getPage(request, result);
    }

    private static SearchResult<TTDDiseaseAssociation> convertDiseases(final AssociationSearchRequest request,
                                                                       final List<TTDDiseaseAssociation> result) {
        result.sort((p1, p2) -> {
            CompareToBuilder builder = new CompareToBuilder();
            if (CollectionUtils.isEmpty(request.getOrderInfos())) {
                builder.append(p1.getTarget(), p2.getTarget());
            } else {
                request.getOrderInfos().forEach(f -> {
                    Function<TTDDiseaseAssociation, String> func = TTDDiseaseField.valueOf(f.getOrderBy()).getGetter();
                    if (f.isReverse()) {
                        builder.append(func.apply(p2), func.apply(p1));
                    } else {
                        builder.append(func.apply(p1), func.apply(p2));
                    }
                });
            }
            return builder.toComparison();
        });
        return getPage(request, result);
    }

    private static <T> SearchResult<T> getPage(final AssociationSearchRequest request, final List<T> result) {
        final SearchResult<T> searchResult = new SearchResult<>();
        final int pageNum = request.getPage() == null ? 1 : Math.max(request.getPage(), 1);
        final int pageSize = request.getPageSize() == null ? DEFAULT_PAGE_SIZE : Math.max(request.getPageSize(), 1);
        searchResult.setItems(result.subList(Math.min((pageNum - 1) * pageSize, result.size()),
                Math.min((pageNum * pageSize), result.size())));
        searchResult.setTotalCount(result.size());
        return searchResult;
    }

    private String getCashFilePath(final String geneId) {
        return Paths.get(parasiteProteinsDirectory, String.format("%s.txt", geneId)).toString();
    }
}
