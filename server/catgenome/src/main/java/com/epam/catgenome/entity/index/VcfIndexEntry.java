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
public class VcfIndexEntry extends FeatureIndexEntry {
    private VariationType variationType;
    private String gene;
    private String geneIds;
    private String geneName;
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
        isExon = origin.getExon();
    }

    public VariationImpact getImpact() {
        return impact;
    }

    public void setImpact(VariationImpact impact) {
        this.impact = impact;
    }

    public VariationEffect getEffect() {
        return effect;
    }

    public void setEffect(VariationEffect effect) {
        this.effect = effect;
    }

    public VariationType getVariationType() {
        return variationType;
    }

    public void setVariationType(VariationType type) {
        this.variationType = type;
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public String getFailedFilter() {
        return failedFilter;
    }

    public void setFailedFilter(String failedFilter) {
        this.failedFilter = failedFilter;
    }

    public String getGeneIds() {
        return geneIds;
    }

    public void setGeneIds(String geneIds) {
        this.geneIds = geneIds;
    }

    public Map<String, Object> getInfo() {
        return info;
    }

    public void setInfo(Map<String, Object> info) {
        this.info = info;
    }

    public Double getQuality() {
        return quality;
    }

    public void setQuality(Double quality) {
        this.quality = quality;
    }

    public String getGeneName() {
        return geneName;
    }

    public void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    public String getGeneNames() {
        return geneNames;
    }

    public void setGeneNames(String geneNames) {
        this.geneNames = geneNames;
    }

    public Boolean getExon() {
        return isExon;
    }

    public void setExon(Boolean exon) {
        isExon = exon;
    }

    public VariantContext getVariantContext() {
        return variantContext;
    }

    public void setVariantContext(VariantContext variantContext) {
        this.variantContext = variantContext;
    }

    public List<String> getGeneIdList()
    {
        return geneIdList;
    }

    public void setGeneIdList(List<String> geneIdList)
    {
        this.geneIdList = geneIdList;
    }

    public List<String> getGeneNameList()
    {
        return geneNameList;
    }

    public void setGeneNameList(List<String> geneNameList)
    {
        this.geneNameList = geneNameList;
    }

    public Set<VariationType> getVariationTypes()
    {
        return variationTypes;
    }

    public Set<String> getFailedFilters()
    {
        return failedFilters;
    }

    public void setFailedFilters(Set<String> failedFilters)
    {
        this.failedFilters = failedFilters;
    }

    public void setVariationTypes(Set<VariationType> variationTypes)
    {
        this.variationTypes = variationTypes;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        VcfIndexEntry that = (VcfIndexEntry) o;

        if (variationType != that.variationType) {
            return false;
        }
        if ((gene != null) ? !gene.equals(that.gene) : (that.gene != null)) {
            return false;
        }
        if ((geneIds != null) ? !geneIds.equals(that.geneIds) : (that.geneIds != null)) {
            return false;
        }
        if ((geneName != null) ? !geneName.equals(that.geneName) : (that.geneName != null)) {
            return false;
        }
        if ((geneNames != null) ? !geneNames.equals(that.geneNames) : (that.geneNames != null)) {
            return false;
        }
        if ((failedFilter != null) ?
                !failedFilter.equals(that.failedFilter) : (that.failedFilter != null)) {
            return false;
        }
        if (impact != that.impact) {
            return false;
        }
        if (effect != that.effect) {
            return false;
        }
        if ((info != null) ? !info.equals(that.info) : (that.info != null)) {
            return false;
        }
        if ((quality != null) ? !quality.equals(that.quality) : (that.quality != null)) {
            return false;
        }
        return (isExon != null) ? isExon.equals(that.isExon) : (that.isExon == null);
    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (variationType != null ? variationType.hashCode() : 0);
        result = 31 * result + (gene != null ? gene.hashCode() : 0);
        result = 31 * result + (geneIds != null ? geneIds.hashCode() : 0);
        result = 31 * result + (geneName != null ? geneName.hashCode() : 0);
        result = 31 * result + (geneNames != null ? geneNames.hashCode() : 0);
        result = 31 * result + (failedFilter != null ? failedFilter.hashCode() : 0);
        result = 31 * result + (impact != null ? impact.hashCode() : 0);
        result = 31 * result + (effect != null ? effect.hashCode() : 0);
        result = 31 * result + (info != null ? info.hashCode() : 0);
        result = 31 * result + (quality != null ? quality.hashCode() : 0);
        result = 31 * result + (isExon != null ? isExon.hashCode() : 0);
        return result;
    }
}
