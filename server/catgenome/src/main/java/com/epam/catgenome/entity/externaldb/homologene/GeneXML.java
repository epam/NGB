/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.entity.externaldb.homologene;

import lombok.Getter;

import javax.xml.bind.annotation.XmlElement;

@Getter
public class GeneXML {
    @XmlElement(name = "HG-Gene_geneid")
    private Long geneId;
    @XmlElement(name = "HG-Gene_symbol")
    private String symbol;
    @XmlElement(name = "HG-Gene_aliases")
    private GeneAliasesXML geneAliases;
    @XmlElement(name = "HG-Gene_title")
    private String title;
    @XmlElement(name = "HG-Gene_taxid")
    private Long taxId;
    @XmlElement(name = "HG-Gene_prot-gi")
    private Long protGi;
    @XmlElement(name = "HG-Gene_prot-acc")
    private String protAcc;
    @XmlElement(name = "HG-Gene_prot-len")
    private Long protLen;
    @XmlElement(name = "HG-Gene_nuc-gi")
    private Long nucGi;
    @XmlElement(name = "HG-Gene_nuc-acc")
    private String nucAcc;
    @XmlElement(name = "HG-Gene_domains")
    private GeneDomainsXML geneDomains;
    @XmlElement(name = "HG-Gene_locus-tag")
    private String locusTag;
}
