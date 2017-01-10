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

package com.epam.catgenome.controller.vo.converter;

import java.util.ArrayList;
import java.util.List;

import com.epam.catgenome.controller.vo.externaldb.UniprotEntryVO;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.Entry;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.EvidencedStringType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.GeneNameType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.GeneType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.OrganismNameType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.OrganismType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.OrganismType.Lineage;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.ProteinType.SubmittedName;

/**
 *  <p>
 *  Class for converting from uniprot DB entry to our entry and vice versa
 *  </p>
 */
public final class UniprotConverter {
    
    private UniprotConverter(){}

    /**
     * Converts {@code Uniprot} Entry JAXB object into {@code UniprotEntryVO}
     * @param uniprotEntry
     *            {@code Uniprot}
     * @return {@code UniprotEntryVO}
     */
    public static UniprotEntryVO convertTo(final Entry uniprotEntry) {

        UniprotEntryVO vo = new UniprotEntryVO();
        setEntryName(uniprotEntry, vo);
        setAccession(uniprotEntry, vo);
        // convert protein names
        List<SubmittedName> submittedNamesList = uniprotEntry.getProtein().getSubmittedName();
        List<String> proteinNamesList = new ArrayList<>();

        submittedNamesList.forEach(s -> {
            EvidencedStringType fullName = s.getFullName();
            if (fullName != null) {
                proteinNamesList.add(fullName.getValue());
            }
        });

        vo.setProteinNames(proteinNamesList);

        // convert gene names
        setGeneNames(uniprotEntry, vo);

        // organism names and lineage
        OrganismType organism = uniprotEntry.getOrganism();

        setOrganismAndLineage(vo, organism);

        return vo;
    }

    private static void setOrganismAndLineage(UniprotEntryVO vo, OrganismType organism) {
        if (organism != null) {
            List<OrganismNameType> namesList = organism.getName();

            StringBuilder sb = new StringBuilder();

            for (OrganismNameType name : namesList) {
                String value = name.getValue();
                sb.append(value).append(' ');
            }
            vo.setOrganismName(sb.toString());

            Lineage lineage = organism.getLineage();

            if (lineage != null) {
                List<String> taxon = lineage.getTaxon();
                vo.setLineage(taxon);
            }
        }
    }

    private static void setGeneNames(Entry uniprotEntry, UniprotEntryVO vo) {
        List<GeneType> geneList = uniprotEntry.getGene();
        List<String> geneNamesList = new ArrayList<>();

        if (geneList != null) {
            GeneType geneType = geneList.get(0);

            if (geneType != null) {
                List<GeneNameType> nameList = geneType.getName();
                if (nameList != null) {
                    nameList.stream().forEach(name -> geneNamesList.add(name.getValue()));
                }
            }
        }

        vo.setGeneNames(geneNamesList);
    }

    private static void setAccession(Entry uniprotEntry, UniprotEntryVO vo) {
        List<String> accessions = uniprotEntry.getAccession();
        if (accessions != null && !accessions.isEmpty()) {
            String accession = accessions.get(0);
            if (accession != null) {
                vo.setAccession(accession);
            }
        }
    }

    private static void setEntryName(Entry uniprotEntry, UniprotEntryVO vo) {
        List<String> entryNames = uniprotEntry.getName();

        if (entryNames != null && !entryNames.isEmpty()) {
            String entryNameStr = entryNames.get(0);

            if (entryNameStr != null) {
                vo.setName(entryNameStr);
            }
        }
    }
}
