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
package com.epam.catgenome.entity.target;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TargetIdentificationResult extends IdentificationResult{
    private Map<String, String> description;
    private Long diseasesCount;
    private Long publicationsCount;
    private SequencesSummary sequencesCount;
    private Long structuresCount;
    @Builder
    public TargetIdentificationResult(Map<String, String> description, Long diseasesCount, Long publicationsCount,
                                      SequencesSummary sequencesCount, Long structuresCount,
                                      Long knownDrugsCount, Long knownDrugsRecordsCount) {
        super(knownDrugsCount, knownDrugsRecordsCount);
        this.description = description;
        this.diseasesCount = diseasesCount;
        this.publicationsCount = publicationsCount;
        this.sequencesCount = sequencesCount;
        this.structuresCount = structuresCount;
    }
}
