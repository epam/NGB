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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequencesSummary {
    private static final String TOTAL_PATTERN = "DNAs: %s, mRNAs: %s, proteins: %s";
    private Long dNAs;
    private Long mRNAs;
    private Long proteins;

    @Override
    public String toString() {
        return String.format(TOTAL_PATTERN, dNAs, mRNAs, proteins);
    }

    @JsonIgnore
    public SequencesSummary sum(final SequencesSummary other) {
        return SequencesSummary.builder()
                .dNAs(sum(dNAs, other.getDNAs()))
                .mRNAs(sum(mRNAs, other.getMRNAs()))
                .proteins(sum(proteins, other.getProteins()))
                .build();
    }

    private static Long sum(final Long l1, final Long l2) {
        final Long value1 = Optional.ofNullable(l1).orElse(0L);
        final Long value2 = Optional.ofNullable(l2).orElse(0L);
        return value1 + value2;
    }
}
