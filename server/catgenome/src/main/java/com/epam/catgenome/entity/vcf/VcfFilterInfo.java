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

package com.epam.catgenome.entity.vcf;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Contains data to fill filter form on UI for a VCF FILTER and INFO header lines
 */
public class VcfFilterInfo {
    @JsonIgnore
    private Map<String, InfoItem> infoItemMap; // map for inner representation only

    private Collection<InfoItem> infoItems;
    private Set<String> availableFilters;

    public Collection<InfoItem> getInfoItems() {
        return infoItems;
    }

    public Map<String, InfoItem> getInfoItemMap() {
        return infoItemMap;
    }

    public void setInfoItemMap(Map<String, InfoItem> infoItemMap) {
        this.infoItemMap = infoItemMap;
        this.infoItems = infoItemMap.values();
    }

    public void setInfoItems(Collection<InfoItem> infoItems) {
        this.infoItems = infoItems;
    }

    public Set<String> getAvailableFilters() {
        return availableFilters;
    }

    public void setAvailableFilters(Set<String> availableFilters) {
        this.availableFilters = availableFilters;
    }
}
