/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.controller.vo.externaldb.ensemblevo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Source: EnsemblVariationEntryVO
 * Created: 10.02.16
 * Project: CATGenome Browser 
 *
 * <p>
 * class for extarnale DB data (variation)
 * </p>
 *
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnsemblVariationEntryVO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "MAF")
    private Double maf;

    @JsonProperty(value = "ambiguity")
    private String ambiguity;

    @JsonProperty(value = "var_class")
    private String varClass;

    @JsonProperty(value = "synonyms")
    private String [] synonyms;

    @JsonProperty(value = "evidence")
    private String[] evidences;

    @JsonProperty(value = "ancestral_allele")
    private String ancestralAllele;

    @JsonProperty(value = "minor_allele")
    private String minorAllele;

    @JsonProperty(value = "most_severe_consequence")
    private String mostSevereConsequence;

    @JsonProperty(value = "mappings")
    private List<VariationMapping> mappings;

    @Getter
    @Setter
    public static class VariationMapping extends EnsemblBaseVO {

        @JsonProperty(value = "location")
        private String location;
        @JsonProperty(value = "assembly_name")
        private String assemblyName;
        @JsonProperty(value = "seq_region_name")
        private String seqRegionName;
        @JsonProperty(value = "strand")
        private Integer strand;
        @JsonProperty(value = "coord_system")
        private String coordSystem;
        @JsonProperty(value = "allele_string")
        private String alleleString;
    }
}
