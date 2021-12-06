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

package com.epam.catgenome.entity.vcf;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epam.catgenome.entity.track.Block;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents VCF track block, parsed from a VCF file, the structure of this class is
 * similar to the VCF file line.
 */
@Getter
@Setter
@NoArgsConstructor
public class Variation extends Block {

    private String identifier;
    private String referenceAllele;
    private List<String> alternativeAlleles;
    private Double quality;
    private List<Filter> failedFilters;
    private VariationType type;
    private Map<String, GenotypeData> genotypeData;
    private Map<String, InfoField> info;
    private Map<String, Object> bindInfo;
    private boolean structural = false;
    private Set<String> geneNames;

    /**
     * On smaller scales defines that a certain number of variations is present in this pixel
     */
    private Integer variationsCount;

    /**
     * @param startIndex border of a block
     * @param endIndex border of a block
     * @param referenceAllele reference sequence
     * @param alternativeAlleles {@code List} of variation sequences present at the block
     */
    public Variation(int startIndex, int endIndex, String referenceAllele, List<String> alternativeAlleles) {
        this.referenceAllele = referenceAllele;
        this.alternativeAlleles = alternativeAlleles;

        setStartIndex(startIndex);
        setEndIndex(endIndex);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class InfoField {
        private String description;
        private Object value;
        private InfoFieldTypes type = InfoFieldTypes.TRIVIAL;
        private List<String> header;

        public InfoField(String description, Object value) {
            this.description = description;
            this.value = value;
        }
    }

    public enum InfoFieldTypes {
        TRIVIAL, TABLE
    }
}
