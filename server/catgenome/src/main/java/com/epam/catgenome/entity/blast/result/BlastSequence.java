/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.catgenome.entity.blast.result;

import com.epam.catgenome.manager.blast.dto.Entry;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.math3.util.Precision;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class BlastSequence {
    private static final int TOTAL_SUM_PRECISION = 1;

    private String sequenceId;
    private String sequenceAccessionVersion;
    private String organism;
    private Long taxId;
    private Double maxScore;
    private Double totalScore;
    private Double queryCoverage;
    private Double eValue;
    private Double percentIdentity;
    private Integer matches;
    private List<Alignment> alignments;

    public static BlastSequence fromEntries(final List<Entry> entries) {
        Assert.state(!CollectionUtils.isEmpty(entries), "Entries shall be provided");
        final Entry maxScoringEntry = entries.stream()
                .max(Comparator.comparing(Entry::getBitScore)).orElse(entries.get(0));
        return BlastSequence.builder()
                .sequenceId(maxScoringEntry.getSeqSeqId())
                .sequenceAccessionVersion(maxScoringEntry.getSeqAccVersion())
                .organism(maxScoringEntry.getSeqSciName())
                .taxId(maxScoringEntry.getSeqTaxId())
                .matches(entries.size())
                .maxScore(maxScoringEntry.getBitScore())
                .eValue(maxScoringEntry.getExpValue())
                .queryCoverage(maxScoringEntry.getQueryCovS())
                .percentIdentity(maxScoringEntry.getPercentIdent())
                .totalScore(Precision.round(entries.stream().mapToDouble(Entry::getBitScore).sum(),
                        TOTAL_SUM_PRECISION))
                .alignments(entries.stream()
                        .map(Alignment::fromEntry)
                        .sorted(Comparator.comparing(Alignment::getEValue))
                        .collect(Collectors.toList()))
                .build();
    }
}
