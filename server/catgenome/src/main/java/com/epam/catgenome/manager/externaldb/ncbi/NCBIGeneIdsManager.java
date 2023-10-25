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
package com.epam.catgenome.manager.externaldb.ncbi;

import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.EnsemblDataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NCBIGeneIdsManager {

    private final NCBIEnsemblIdsManager ncbiEnsemblIdsManager;
    private final EnsemblDataManager ensemblDataManager;
    private final NCBIGeneManager geneManager;

    public List<GeneId> getNcbiGeneIds(final List<String> geneIds) throws ParseException, IOException {
        List<GeneId> ncbiGenes = new ArrayList<>();
        final List<String> alphaNumericGeneIds = geneIds.stream()
                .filter(g -> !StringUtils.isNumeric(g))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(alphaNumericGeneIds)) {
            ncbiGenes = ncbiEnsemblIdsManager.searchByEnsemblIds(alphaNumericGeneIds);
        }
        final List<String> ensemblGenes = ncbiGenes.stream().map(GeneId::getEnsemblId).collect(Collectors.toList());
        String ncbiGeneId;
        for (String geneId: alphaNumericGeneIds) {
            if (!ensemblGenes.contains(geneId.toLowerCase())) {
                ncbiGeneId = ensemblDataManager.fetchNcbiId(geneId);
                if (!TextUtils.isBlank(ncbiGeneId)) {
                    try {
                        ncbiGenes.add(GeneId.builder().ensemblId(geneId).entrezId(Long.parseLong(ncbiGeneId)).build());
                    } catch (NumberFormatException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        }
        final List<String> numericGeneIds = geneIds.stream()
                .filter(StringUtils::isNumeric)
                .collect(Collectors.toList());
        for (String numericGeneId: numericGeneIds) {
            ncbiGenes.add(GeneId.builder().ensemblId(numericGeneId).entrezId(Long.parseLong(numericGeneId)).build());
        }
        return ncbiGenes;
    }

    public static List<Long> getEntrezGeneIds(final List<GeneId> ncbiGeneIds) {
        return ncbiGeneIds.stream().map(GeneId::getEntrezId).collect(Collectors.toList());
    }

    public List<GeneId> getGeneIds(final Map<String, String> geneNames, final List<String> geneIds)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final List<GeneId> ncbiGeneIds = getNcbiGeneIds(geneIds);
        return geneManager.getGeneIds(ncbiGeneIds, geneNames);
    }
}
