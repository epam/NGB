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

package com.epam.catgenome.controller.vo.ga4gh;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * <p>
 * Class for data from GA4GH (variant set)
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariantSet {

    /**
     * {@code String} The dataset to which this variant set belongs.
     */
    private String datasetId;

    /**
     * {@code String} The server-generated variant set ID, unique across all variant sets.
     */
    private String id;

    /**
     * {@code String }The reference set to which the variant set is mapped.
     * The reference set describes the alignment provenance of the variant set, while the referenceBounds describe the
     * shape of the actual variant data.
     * The reference set's reference names are a superset of those found in the referenceBounds.
     */
    private String referenceSetId;

    /**
     * {@code List} A list of all references used by the variants in a variant set with associated coordinate upper
     * bounds for each one.
     */
    private List<ReferenceBound> referenceBounds;

    /**
     * {@code List} The metadata associated with this variant set.
     */
    private List<VariantSetMetadata> metadata;

    /**
     * {@code String} User-specified, mutable name.
     */
    private String name;

    /**
     * {@code String} A textual description of this variant set.
     */
    private String description;

    public VariantSet() {
        //no-op
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReferenceSetId() {
        return referenceSetId;
    }

    public void setReferenceSetId(String referenceSetId) {
        this.referenceSetId = referenceSetId;
    }

    public List<ReferenceBound> getReferenceBounds() {
        return referenceBounds;
    }

    public void setReferenceBounds(List<ReferenceBound> referenceBounds) {
        this.referenceBounds = referenceBounds;
    }

    public List<VariantSetMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<VariantSetMetadata> metadata) {
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}


