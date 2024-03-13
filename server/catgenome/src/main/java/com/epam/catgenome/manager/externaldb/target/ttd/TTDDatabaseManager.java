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
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.manager.index.Filter;
import com.epam.catgenome.manager.sequence.SequencesManager;
import com.epam.catgenome.manager.target.ParasiteProteinsManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final SequencesManager sequencesManager;

    public void importData(final String drugsPath, final String targetsPath, final String diseasesPath)
            throws IOException, ParseException {
        ttdDrugAssociationManager.importData(drugsPath, targetsPath);
        ttdDiseaseAssociationManager.importData(diseasesPath);
    }

    public SearchResult<TTDDiseaseAssociation> getTTDDiseases(final List<TargetGene> targetGenes,
                                                              final AssociationSearchRequest request)
            throws ParseException, IOException {
        final List<TTDDiseaseAssociation> result = fetchTTDDiseases(targetGenes, request.getFilters());
        return convertDiseases(request, result);
    }

    public List<TTDDiseaseAssociation> fetchTTDDiseases(final List<TargetGene> targetGenes,
                                                        final List<Filter> filters)
            throws ParseException, IOException {
        final List<TTDDiseaseAssociation> result = new ArrayList<>();
        for (TargetGene gene : targetGenes) {
            final List<String> geneNames = Collections.singletonList(gene.getGeneName());
            final List<TTDDiseaseAssociation> byGeneNames =
                    ttdDiseaseAssociationManager.searchByTargetNames(geneNames, filters);
            if (CollectionUtils.isNotEmpty(byGeneNames)) {
                byGeneNames.forEach(i -> i.setGeneId(gene.getGeneId()));
                result.addAll(byGeneNames);
            }
            if (CollectionUtils.isNotEmpty(gene.getTtdTargets())) {
                final List<String> targetIds = gene.getTtdTargets();
                if (CollectionUtils.isNotEmpty(targetIds)) {
                    List<TTDDiseaseAssociation> bySequences =
                            ttdDiseaseAssociationManager.searchByTargetIds(targetIds, filters);
                    if (CollectionUtils.isNotEmpty(bySequences)) {
                        bySequences.forEach(i -> i.setGeneId(gene.getGeneId()));
                        result.addAll(bySequences);
                    }
                }
            }
        }
        return result;
    }

    public SearchResult<TTDDrugAssociation> getTTDDrugs(final List<TargetGene> genes,
                                                        final AssociationSearchRequest request)
            throws ParseException, IOException {
        final List<TTDDrugAssociation> result = fetchTTDDrugs(genes, request.getFilters());
        return convertDrugs(request, result);
    }

    public List<TTDDrugAssociation> fetchTTDDrugs(final List<TargetGene> genes,
                                                   final List<Filter> filters)
            throws ParseException, IOException {
        final List<TTDDrugAssociation> result = new ArrayList<>();
        for (TargetGene gene : genes) {
            List<String> geneNames = Collections.singletonList(gene.getGeneName());
            List<TTDDrugAssociation> byGeneNames =
                    ttdDrugAssociationManager.searchByTargetNames(geneNames, filters);
            if (CollectionUtils.isNotEmpty(byGeneNames)) {
                byGeneNames.forEach(i -> i.setGeneId(gene.getGeneId()));
                result.addAll(byGeneNames);
            }
            if (CollectionUtils.isNotEmpty(gene.getTtdTargets())) {
                List<String> targetIds = gene.getTtdTargets();
                if (CollectionUtils.isNotEmpty(targetIds)) {
                    List<TTDDrugAssociation> bySequences =
                            ttdDrugAssociationManager.searchByTargetIds(targetIds, filters);
                    if (CollectionUtils.isNotEmpty(bySequences)) {
                        bySequences.forEach(i -> i.setGeneId(gene.getGeneId()));
                        result.addAll(bySequences);
                    }
                }
            }
        }
        return result;
    }

    public TTDDrugFieldValues getDrugFieldValues(final List<TargetGene> targetGenes)
            throws IOException, ParseException {
        final List<TTDDrugAssociation> result = fetchTTDDrugs(targetGenes, Collections.emptyList());
        final List<String> ttdTargets = result.stream()
                .map(TTDDrugAssociation::getTtdTarget)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
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
                .ttdTargets(ttdTargets)
                .companies(companies)
                .types(types)
                .therapeuticClasses(therapeuticClasses)
                .statuses(statuses)
                .compoundClasses(compoundClasses)
                .build();
    }

    public TTDDiseaseFieldValues getDiseaseFieldValues(final List<TargetGene> targetGenes)
            throws IOException, ParseException {
        final List<TTDDiseaseAssociation> result = fetchTTDDiseases(targetGenes, Collections.emptyList());
        final List<String> phases = result.stream()
                .map(TTDDiseaseAssociation::getClinicalStatus)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> ttdTargets = result.stream()
                .map(TTDDiseaseAssociation::getTtdTarget)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return TTDDiseaseFieldValues.builder()
                .ttdTargets(ttdTargets)
                .phases(phases)
                .build();
    }


    public List<String> getBlastSequences(final String geneId, final long taxId)
            throws IOException, InterruptedException, BlastRequestException {
        final List<String> sequences = sequencesManager.getGeneProteins(geneId, taxId);
        final List<String> blastSequences = new ArrayList<>();
        for (String s : sequences) {
            if (StringUtils.isNotBlank(s)) {
                blastSequences.addAll(parasiteProteinsManager.getSequences(s));
            }
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
}
