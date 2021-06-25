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

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.entity.blast.result.BlastFeatureLocatable;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIDataManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.epam.catgenome.component.MessageHelper.*;

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
    private final ReferenceGenomeManager genomeManager;

    @SneakyThrows
    public BlastFeatureLocatable fetchCoordinates(final String sequenceId, final BlastDatabaseType type,
                                                  final Long taxId) {
        try {
            final String featureName = fetchFeatureNameFromNcbi(sequenceId, type);
            Assert.notNull(featureName,
                    getMessage(MessagesConstants.ERROR_NCBI_CANT_LOAD_FEATURE_INFO, sequenceId, getNcbiDbType(type)));
            final Reference reference = ListUtils.emptyIfNull(genomeManager.loadAllReferenceGenomesByTaxId(taxId))
                    .stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            getMessage(MessagesConstants.ERROR_REFERENCE_NOT_FOUND_BY_TAX_ID, taxId)));
            final Optional<FeatureIndexEntry> featureSearchResult =
                    featureIndexManager.searchFeaturesByReference(featureName, reference.getId())
                            .getEntries().stream()
                            .filter(f -> f.getFeatureName().equalsIgnoreCase(featureName))
                            .findFirst();
            return featureSearchResult.map(feature -> BlastFeatureLocatable.builder()
                    .referenceId(reference.getId())
                    .blastSeqId(sequenceId)
                    .type(type)
                    .featureName(feature.getFeatureName())
                    .chromosomeId(feature.getChromosome().getId())
                    .start(feature.getStartIndex())
                    .end(feature.getEndIndex())
                    .build())
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    getMessage(MessagesConstants.ERROR_FEATURE_INDEX_ENTRY_NOT_FOUND, featureName)
                            )
                    );
        } catch (ExternalDbUnavailableException e) {
            throw new IllegalArgumentException(
                    getMessage(MessagesConstants.ERROR_NCBI_CANT_LOAD_FEATURE_INFO, sequenceId, getNcbiDbType(type)), e
            );
        }
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
