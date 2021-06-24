/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.manager.blast;

import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.entity.blast.result.BlastFeatureLocatable;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIDataManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlastUtilManager {

    private static final Pattern FEATURE_TAG_PATTERN = Pattern.compile(".*/(gene|locus_tag|operon)=\"(.*)\".*");
    private static final String GENE_BANK_FORMAT = "gb";
    private static final String PROTEIN_DB = "protein";
    private static final String NUCLEOTIDE_DB = "nucleotide";
    private static final String GENE_BANK_ORIGIN_TAG = "ORIGIN";
    private static final String NEW_LINE = "\n";
    private static final String GENE_FEATURE_TAG = "gene";
    private static final String OPERON_FEATURE_TAG = "operon";
    private static final String LOCUS_TAG_FEATURE_TAG = "locus_tag";

    private final NCBIDataManager ncbiDataManager;
    private final FeatureIndexManager featureIndexManager;

    @SneakyThrows
    public BlastFeatureLocatable fetchCoordinates(final String sequenceId, final BlastDatabaseType type,
                                                  final Long referenceId) {
        final String featureName = fetchFeatureNameFromNcbi(sequenceId, type);
        Assert.notNull(featureName, "Can't load feature name from NCBI for sequence: " + sequenceId);
        final Optional<FeatureIndexEntry> featureSearchResult =
                featureIndexManager.searchFeaturesByReference(featureName, referenceId)
                        .getEntries().stream()
                        .filter(f -> f.getFeatureName().equalsIgnoreCase(featureName))
                        .findFirst();
        return featureSearchResult.map(feature -> BlastFeatureLocatable.builder()
                .referenceId(referenceId)
                .blastSeqId(sequenceId)
                .type(type)
                .featureName(feature.getFeatureName())
                .chromosomeId(feature.getChromosome().getId())
                .start(feature.getStartIndex())
                .end(feature.getEndIndex())
                .build())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Can't find information about a feature by name: " + featureName
                        )
                );
    }

    private String fetchFeatureNameFromNcbi(final String sequenceId, 
                                            final BlastDatabaseType type) throws ExternalDbUnavailableException {
        String geneBankFile = ncbiDataManager.fetchTextById(getNcbiDbType(type), sequenceId, GENE_BANK_FORMAT);

        final Map<String, String> featureTags = new HashMap<>();
        for (String line : geneBankFile.split(NEW_LINE)) {
            if(line.contains(GENE_BANK_ORIGIN_TAG)) {
                break;
            }
            final Matcher matcher = FEATURE_TAG_PATTERN.matcher(line);
            if (matcher.find()) {
                featureTags.put(matcher.group(1), matcher.group(2));
            }
        }

        if (featureTags.containsKey(GENE_FEATURE_TAG)) {
            return featureTags.get(GENE_FEATURE_TAG);
        } else if (featureTags.containsKey(OPERON_FEATURE_TAG)) {
            return featureTags.get(OPERON_FEATURE_TAG);
        } else {
            return featureTags.get(LOCUS_TAG_FEATURE_TAG);
        }
    }

    private String getNcbiDbType(final BlastDatabaseType type) {
       return  type == BlastDatabaseType.PROTEIN ? PROTEIN_DB : NUCLEOTIDE_DB;
    }
}
