/*
 * MIT License
 *
 * Copyright (c) 2016-2023 EPAM Systems
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

package com.epam.catgenome.manager.externaldb.ncbi;

import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.entity.externaldb.target.opentargets.UrlEntity;
import com.epam.catgenome.entity.target.GeneSequences;
import com.epam.catgenome.entity.target.Sequence;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.entity.target.Sequence.parseStrand;
import static org.apache.commons.lang3.StringUtils.join;


@Service
@RequiredArgsConstructor
public class NCBIGeneSequencesManager extends HttpDataManager {

    private static final String NCBI_GENE_INFO_LINK = "https://api.ncbi.nlm.nih.gov/datasets/v1/gene/id/%s";
    private static final String NCBI_PROTEIN_LINK = "https://www.ncbi.nlm.nih.gov/protein/%s";
    private static final String NCBI_NUCCORE_LINK = "https://www.ncbi.nlm.nih.gov/nuccore/%s";

    @SneakyThrows
    public List<GeneSequences> fetchGeneSequences(final Map<String, GeneId> entrezMap) {
        final String link = String.format(NCBI_GENE_INFO_LINK, join(entrezMap.keySet(), "%2C"));
        final String json = getResultFromURL(link);
        return parseSequences(json, entrezMap);
    }

    private List<GeneSequences> parseSequences(final String json, final Map<String, GeneId> entrezMap)
            throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonNodes = objectMapper.readTree(json);
        final JsonNode genes = jsonNodes.at("/genes");
        final List<GeneSequences> result = new ArrayList<>();
        if (genes.isArray()) {
            final Iterator<JsonNode> elements = genes.elements();
            while (elements.hasNext()) {
                final GeneSequences sequences = new GeneSequences();
                final JsonNode node = elements.next();
                final String entrezId = node.at("/gene/gene_id").asText();
                sequences.setGeneId(entrezMap.get(entrezId).getEnsembleId());

                final JsonNode referenceNodes = node.at("/gene/reference_standards");
                if (referenceNodes.isArray()) {
                    final Iterator<JsonNode> referenceElements = referenceNodes.elements();
                    if (referenceElements.hasNext()) {
                        JsonNode referenceNode = referenceElements.next();
                        String referenceId = referenceNode.at("/gene_range/accession_version").asText();
                        UrlEntity reference = new UrlEntity();
                        reference.setId(referenceId);
                        reference.setUrl(String.format(NCBI_NUCCORE_LINK, referenceId));
                        sequences.setReference(reference);
                    }
                }

                final JsonNode transcriptsNodes = node.at("/gene/transcripts");
                if (transcriptsNodes.isArray()) {
                    final Iterator<JsonNode> transcriptsElements = transcriptsNodes.elements();
                    final List<Sequence> mRNAs = new ArrayList<>();
                    final List<UrlEntity> proteins = new ArrayList<>();
                    while (transcriptsElements.hasNext()) {
                        JsonNode transcriptsNode = transcriptsElements.next();

                        String mRNAId = transcriptsNode.at("/accession_version").asText();
                        Sequence mRNA = new Sequence();
                        mRNA.setId(mRNAId);
                        mRNA.setUrl(String.format(NCBI_NUCCORE_LINK, mRNAId));
                        final JsonNode rangeNodes = transcriptsNode.at("/genomic_range/range");
                        if (rangeNodes.isArray()) {
                            final Iterator<JsonNode> rangeElements = rangeNodes.elements();
                            if (rangeElements.hasNext()) {
                                JsonNode rangeNode = rangeElements.next();
                                mRNA.setBegin(rangeNode.at("/begin").asLong());
                                mRNA.setEnd(rangeNode.at("/end").asLong());
                                String orientation = rangeNode.at("/orientation").asText();
                                mRNA.setStrand(parseStrand(orientation));
                            }
                        }
                        mRNAs.add(mRNA);

                        String proteinId = transcriptsNode.at("/protein/accession_version").asText();
                        String proteinName= transcriptsNode.at("/protein/name").asText();
                        String isoformName= transcriptsNode.at("/protein/isoform_name").asText();
                        UrlEntity protein = new UrlEntity();
                        protein.setId(proteinId);
                        protein.setName(proteinName + " " + isoformName);
                        protein.setUrl(String.format(NCBI_PROTEIN_LINK, proteinId));
                        proteins.add(protein);
                    }
                    sequences.setMRNAs(mRNAs);
                    sequences.setProteins(proteins);
                }
                result.add(sequences);
            }
        }
        return result;
    }
}
