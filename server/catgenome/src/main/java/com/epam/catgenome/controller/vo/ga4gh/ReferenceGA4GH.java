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
 * Source:      ReferenceGA4GH
 * Created:     30.05.16, 13:00
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0, JDK 1.8
 * <p>
 * Represents a reference obtained from google genomic server.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceGA4GH extends ReferenceSet {

    private String id;

    /**
     * {@code String} the length of this reference's sequence.
     **/
    private String length;


    /**
     * {@code String} the name of this reference(chromosome).
     */
    private String name;

    /**
     * {@code ArrayList<String>} all known corresponding accession IDs in INSDC.
     */
    private List<String> sourceAccessions;


    public ReferenceGA4GH() {
        // no-op
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    @Override
    public List<String> getSourceAccessions() {
        return sourceAccessions;
    }

    @Override
    public void setSourceAccessions(List<String> sourceAccessions) {
        this.sourceAccessions = sourceAccessions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
