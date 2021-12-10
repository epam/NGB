/*
 * MIT License
 *
 * Copyright (c) 2017-2021 EPAM Systems
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

package com.epam.catgenome.entity.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epam.catgenome.entity.vcf.VariationEffect;
import com.epam.catgenome.entity.vcf.VariationImpact;
import com.epam.catgenome.entity.vcf.VariationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import htsjdk.variant.variantcontext.VariantContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Source:      VcfIndexEntry
 * Created:     28.04.16, 16:29
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * An entity, representing variation object in feature index
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VcfIndexEntry extends FeatureIndexEntry {
    private VariationType variationType; //TODO: remove, use variationTypes
    private String gene; //TODO: remove, use geneIdList
    private String geneIds;
    private String geneName; //TODO: remove, use geneNameList
    private String geneNames;
    private String failedFilter;
    private VariationImpact impact;
    private VariationEffect effect;
    private Map<String, Object> info;
    private Double quality;
    private Boolean isExon = false;

    // Big Vcf Fields
    private List<String> geneIdList;
    private List<String> geneNameList;
    private Set<VariationType> variationTypes;
    private Set<String> sampleNames;
    private Set<String> failedFilters;


    @JsonIgnore
    private VariantContext variantContext;

    public VcfIndexEntry() {
        super.setFeatureType(FeatureType.VARIATION);
    }

    public VcfIndexEntry(VcfIndexEntry origin) {
        super(origin);
        super.setFeatureType(FeatureType.VARIATION);
        variationType = origin.getVariationType();
        gene = origin.getGene();
        geneIds = origin.getGeneIds();
        geneName = origin.getGeneName();
        geneNames = origin.getGeneNames();
        failedFilter = origin.getFailedFilter();
        impact = origin.getImpact();
        effect = origin.getEffect();
        info = origin.getInfo() != null ? new HashMap<>(origin.getInfo()) : null;
        quality = origin.getQuality();
        isExon = origin.getIsExon();
    }
}
