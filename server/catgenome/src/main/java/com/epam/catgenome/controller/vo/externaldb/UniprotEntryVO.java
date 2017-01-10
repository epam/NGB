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

package com.epam.catgenome.controller.vo.externaldb;

import java.util.List;

/**
 * Source: UniprotEntryVO 
 * Created: 3.02.16 
 * Project: CATGenome Browser 
 * Make: Eclipse Mars.1, JDK 1.8
 *
 * <p>
 * class for Uniprot DB data
 * </p>
 *
 */
public class UniprotEntryVO {

    private String name;
    private String accession;

    private List<String> proteinNames;
    private List<String> geneNames;
    private String organismName;
    private List<String> lineage;
    private String status;
    private String url;
    private String function;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getProteinNames() {
        return proteinNames;
    }

    public void setProteinNames(List<String> proteinNames) {
        this.proteinNames = proteinNames;
    }

    public List<String> getGeneNames() {
        return geneNames;
    }

    public void setGeneNames(List<String> geneNames) {
        this.geneNames = geneNames;
    }

    public String getOrganismName() {
        return organismName;
    }

    public void setOrganismName(String organismName) {
        this.organismName = organismName;
    }

    public List<String> getLineage() {
        return lineage;
    }

    public void setLineage(List<String> lineage) {
        this.lineage = lineage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }
}
