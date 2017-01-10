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
 * Source:      ReferenceBasesGA4GH
 * Created:     30.05.16, 15:00
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0, JDK 1.8
 * <p>
 * Represents a reference set obtained from google genomic server.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceSet {

    /**
     * {@code String} The server-generated reference set ID, unique across all reference sets.
     */
    private String id;

    /**
     * {@code List<String>} The IDs of the reference objects that are part of this set.
     * Reference.md5checksum must be unique within this set.
     */
    private List<String> referenceIds;

    /**
     * {@code String} Order-independent MD5 checksum which identifies this reference set.
     * The checksum is computed by sorting all lower case hexidecimal string reference.md5checksum
     * (for all reference in this set) in ascending lexicographic order,
     * concatenating, and taking the MD5 of that value.
     * The resulting value is represented in lower case hexadecimal format.
     */
    private String md5checksum;

    /**
     * {@code String} ID from http://www.ncbi.nlm.nih.gov/taxonomy.
     */
    private Integer ncbiTaxonId;

    /**
     * {@code String} Free text description of this reference set.
     */
    private String description;

    /**
     * {@code String} Public id of this reference set, such as GRCh37.
     */
    private String assemblyId;

    /**
     * {@code String} The URI from which the references were obtained.
     */
    private String sourceURI;

    /**
     * {@code List<String>} All known corresponding accession IDs in INSDC (GenBank/ENA/DDBJ)
     * ideally with a version number.
     */
    private List<String> sourceAccessions;

    /**
     * {@code Boolean}
     */
    private Boolean isDerived;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMd5checksum() {
        return md5checksum;
    }

    public void setMd5checksum(String md5checksum) {
        this.md5checksum = md5checksum;
    }

    public Integer getNcbiTaxonId() {
        return ncbiTaxonId;
    }

    public void setNcbiTaxonId(Integer ncbiTaxonId) {
        this.ncbiTaxonId = ncbiTaxonId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssemblyId() {
        return assemblyId;
    }

    public void setAssemblyId(String assemblyId) {
        this.assemblyId = assemblyId;
    }

    public String getSourceURI() {
        return sourceURI;
    }

    public void setSourceURI(String sourceURI) {
        this.sourceURI = sourceURI;
    }

    public List<String> getSourceAccessions() {
        return sourceAccessions;
    }

    public void setSourceAccessions(List<String> sourceAccessions) {
        this.sourceAccessions = sourceAccessions;
    }

    public Boolean getDerived() {
        return isDerived;
    }

    public void setDerived(Boolean derived) {
        isDerived = derived;
    }

    public List<String> getReferenceIds() {
        return referenceIds;
    }

    public void setReferenceIds(List<String> referenceIds) {
        this.referenceIds = referenceIds;
    }
}
