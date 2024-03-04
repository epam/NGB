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
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.exception.ReferenceReadingException;
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

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;


@Service
@RequiredArgsConstructor
@Slf4j
public class TTDDrugsManager {

    @Value("${targets.parasite.proteins.directory:C:\\Users\\Oksana_Kolesnikova\\Documents\\ngb\\parasite_proteins}")
    private String parasiteProteinsDirectory;

    private final TTDDrugAssociationManager ttdDrugAssociationManager;
    private final ParasiteProteinsManager parasiteProteinsManager;
    private final TargetManager targetManager;
    private final SequencesManager sequencesManager;

    public SearchResult<TTDDrugAssociation> getTTDDrugs(final AssociationSearchRequest request,
                                                        final Map<String, Long> targetGenes)
            throws ParseException, IOException, BlastRequestException, InterruptedException {
        final List<TTDDrugAssociation> result = new ArrayList<>();
        for (String geneId : request.getGeneIds()) {
            List<String> geneNames = targetManager.getTargetGeneNames(Collections.singletonList(geneId));
            List<TTDDrugAssociation> byGeneNames = CollectionUtils.isEmpty(geneNames) ?
                    Collections.emptyList() :
                    ttdDrugAssociationManager.searchByGeneNames(request, geneNames);
            if (CollectionUtils.isNotEmpty(byGeneNames)) {
                result.addAll(byGeneNames);
            } else {
                List<String> blastSequences = getBlastSequences(targetGenes, geneId);
                if (CollectionUtils.isNotEmpty(blastSequences)) {
                    List<TTDDrugAssociation> bySequences = ttdDrugAssociationManager.searchBySequences(request,
                            blastSequences);
                    if (CollectionUtils.isNotEmpty(bySequences)) {
                        result.addAll(bySequences);
                    }
                }
            }
        }
        return getSearchResult(request, result);
    }

    public List<TTDDrugAssociation> getTTDDrugs(final List<String> geneIds, final Map<String, Long> targetGenes)
            throws ParseException, IOException, BlastRequestException, InterruptedException {
        final List<TTDDrugAssociation> result = new ArrayList<>();
        for (String geneId : geneIds) {
            List<String> geneNames = targetManager.getTargetGeneNames(Collections.singletonList(geneId));
            List<TTDDrugAssociation> byGeneNames = CollectionUtils.isEmpty(geneNames) ?
                    Collections.emptyList() :
                    ttdDrugAssociationManager.searchByGeneNames(geneNames);
            if (CollectionUtils.isNotEmpty(byGeneNames)) {
                result.addAll(byGeneNames);
            } else {
                List<String> blastSequences = getBlastSequences(targetGenes, geneId);
                if (CollectionUtils.isNotEmpty(blastSequences)) {
                    List<TTDDrugAssociation> bySequences = ttdDrugAssociationManager.searchBySequences(blastSequences);
                    if (CollectionUtils.isNotEmpty(bySequences)) {
                        result.addAll(bySequences);
                    }
                }
            }
        }
        return result;
    }

    public TTDDrugFieldValues getFieldValues(final List<String> geneIds, final Map<String, Long> targetGenes)
            throws IOException, ParseException, ReferenceReadingException, BlastRequestException, InterruptedException {
        final List<TTDDrugAssociation> result = getTTDDrugs(geneIds, targetGenes);
        final List<String> companies = result.stream()
                .map(TTDDrugAssociation::getCompany)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> types = result.stream()
                .map(TTDDrugAssociation::getType)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> therapeuticClasses = result.stream()
                .map(TTDDrugAssociation::getTherapeuticClass)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> statuses = result.stream()
                .map(TTDDrugAssociation::getStatus)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> compoundClasses = result.stream()
                .map(TTDDrugAssociation::getCompoundClass)
                .filter(Objects::nonNull)
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

    private static SearchResult<TTDDrugAssociation> getSearchResult(final AssociationSearchRequest request,
                                                                    final List<TTDDrugAssociation> result) {
        final SearchResult<TTDDrugAssociation> searchResult = new SearchResult<>();
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
