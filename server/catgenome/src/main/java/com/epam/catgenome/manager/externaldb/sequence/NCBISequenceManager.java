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

package com.epam.catgenome.manager.externaldb.sequence;

import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.entity.externaldb.target.UrlEntity;
import com.epam.catgenome.entity.target.*;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIDataManager;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBISequenceDatabase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.http.util.TextUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.entity.target.Sequence.parseStrand;
import static com.epam.catgenome.util.Utils.NEW_LINE;
import static com.epam.catgenome.util.Utils.SPACE;
import static com.epam.catgenome.util.Utils.SPACES;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.join;


@Service
@RequiredArgsConstructor
public class NCBISequenceManager {

    private static final String NCBI_GENE_INFO_LINK = "https://api.ncbi.nlm.nih.gov/datasets/v1/gene/id/%s";
    private static final String NCBI_PROTEIN_LINK = "https://www.ncbi.nlm.nih.gov/protein/%s";
    private static final String NCBI_NUCCORE_LINK = "https://www.ncbi.nlm.nih.gov/nuccore/%s";
    private static final String ACCESSION = "ACCESSION";
    private static final Integer BATCH_SIZE = 100;
    private final NCBIDataManager ncbiDataManager;

    @SneakyThrows
    public List<GeneSequences> fetchGeneSequences(final Map<String, GeneId> entrezMap) {
        final String link = String.format(NCBI_GENE_INFO_LINK, join(entrezMap.keySet(), "%2C"));
        final String json = ncbiDataManager.getResultFromURL(link);
        return parseSequences(json, entrezMap);
    }

    public List<GeneRefSection> getGeneSequencesTable(final Map<String, GeneId> entrezMap, final Boolean getComments)
            throws ExternalDbUnavailableException, IOException {
        final String link = String.format(NCBI_GENE_INFO_LINK, join(entrezMap.keySet(), "%2C"));
        final String json = ncbiDataManager.getResultFromURL(link);
        final List<GeneRefSection> geneRefSections = parseSequencesAsTable(json, entrezMap);
        if (getComments) {
            final List<String> proteinIds = geneRefSections.stream()
                    .map(sec -> sec.getSequences().stream()
                            .filter(s -> Objects.nonNull(s.getProtein()))
                            .map(s -> s.getProtein().getId())
                            .collect(Collectors.toList()))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            final Map<String, String> descriptions = getTranscriptDescriptions(proteinIds);
            for (GeneRefSection geneRefSection : geneRefSections){
                for (GeneSequence sequence : geneRefSection.getSequences()) {
                    if (sequence.getProtein() != null) {
                        String description = descriptions.get(sequence.getProtein().getId().split("\\.")[0]);
                        sequence.setDescription(description);
                    }
                }
            }
        }
        return geneRefSections;
    }

    public SequencesSummary getSequencesCount(final List<GeneId> ncbiGeneIds)
            throws IOException, ExternalDbUnavailableException {
        final Map<String, GeneId> entrezMap = ncbiGeneIds.stream()
                .collect(Collectors.toMap(i -> i.getEntrezId().toString(), Function.identity()));
        final List<GeneRefSection> refSections = getGeneSequencesTable(entrezMap, false);

        final List<GeneSequence> sequences = refSections.stream()
                .map(sec -> Optional.ofNullable(sec.getSequences()).orElse(Collections.emptyList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        final List<String> genomic = sequences.stream()
                .map(s -> s.getMRNA() == null ? null : s.getMRNA().getGenomic())
                .collect(Collectors.toList());
        final List<String> references = refSections.stream()
                .map(s -> s.getReference() == null ? null : s.getReference().getId())
                .collect(Collectors.toList());
        genomic.addAll(references);
        final long dNAs = genomic.stream().filter(Objects::nonNull).distinct().count();
        final long mRNAs = sequences.stream()
                .map(s -> s.getMRNA()).filter(Objects::nonNull)
                .count();
        final long proteins = sequences.stream()
                .map(s -> s.getProtein()).filter(Objects::nonNull)
                .count();
        return SequencesSummary.builder()
                .dNAs(dNAs)
                .mRNAs(mRNAs)
                .proteins(proteins)
                .build();
    }

    public Map<String, SequencesSummary> getSequencesCountMap(final List<GeneId> ncbiGeneIds)
            throws IOException, ExternalDbUnavailableException {
        final Map<String, GeneId> entrezMap = ncbiGeneIds.stream()
                .collect(Collectors.toMap(i -> i.getEntrezId().toString(), Function.identity()));
        final List<GeneRefSection> refSections = getGeneSequencesTable(entrezMap, false);
        final Map<String, List<GeneRefSection>> refSectionsMap = refSections.stream()
                .collect(groupingBy(GeneRefSection::getGeneId));
        final Map<String, SequencesSummary> summaries = new HashMap<>();
        for (Map.Entry<String, List<GeneRefSection>> entry : refSectionsMap.entrySet()) {
            String k = entry.getKey();
            List<GeneRefSection> v = entry.getValue();
            List<GeneSequence> sequences = v.stream()
                    .map(geneRefSection -> geneRefSection.getSequences() == null ? null : geneRefSection.getSequences())
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            List<String> genomic = sequences.stream()
                    .map(s -> s.getMRNA() == null ? null : s.getMRNA().getGenomic())
                    .collect(Collectors.toList());
            List<String> references = v.stream()
                    .map(s -> s.getReference() == null ? null : s.getReference().getId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            genomic.addAll(references);
            long dNAs = genomic.stream().filter(Objects::nonNull).distinct().count();
            long mRNAs = sequences.stream()
                    .map(s -> s.getMRNA()).filter(Objects::nonNull)
                    .count();
            long proteins = sequences.stream()
                    .map(s -> s.getProtein()).filter(Objects::nonNull)
                    .count();
            SequencesSummary summary = SequencesSummary.builder()
                    .dNAs(dNAs)
                    .mRNAs(mRNAs)
                    .proteins(proteins)
                    .build();
            summaries.put(k, summary);
        }
        return summaries;
    }


    public String getFasta(final NCBISequenceDatabase database, final String id) throws ExternalDbUnavailableException {
        return ncbiDataManager.fetchTextById(database.getName(), id, "fasta");
    }

    private Map<String, String> getTranscriptDescriptions(final List<String> proteinIds)
            throws ExternalDbUnavailableException, IOException {
        final List<List<String>> subSets = Lists.partition(proteinIds, BATCH_SIZE);
        final Map<String, String> descriptions = new HashMap<>();
        for (List<String> subIds : subSets) {
            final String genbank = getProteinsGenbank(subIds);
            Map<String, String> descriptionsSubSet = getTranscriptDescriptions(genbank);
            if (!descriptionsSubSet.isEmpty()) {
                descriptions.putAll(descriptionsSubSet);
            }
        }
        return descriptions;
    }

    private Map<String, String> getTranscriptDescriptions(final String genbank) throws IOException {
        final Map<String, String> result = new HashMap<>();
        final String[] sequences = genbank.split("//\n\n");
        String line;
        String sequenceId = null;
        String description;
        for (String s: sequences) {
            String[] desc = s.split("Transcript Variant: ");
            if (desc.length > 1) {
                description = desc[1].split("\n( )+\n")[0]
                        .replaceAll(NEW_LINE, SPACE)
                        .replaceAll(SPACES, SPACE);
                BufferedReader reader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(s)));
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(ACCESSION)) {
                        sequenceId = line.replace(ACCESSION, "").trim();
                    }
                }
                if (!TextUtils.isBlank(sequenceId) && !TextUtils.isBlank(description)) {
                    result.put(sequenceId, description);
                }
            }
        }
        return result;
    }

    private String getProteinsGenbank(final List<String> proteinIds) throws ExternalDbUnavailableException {
        return ncbiDataManager.fetchTextById("protein", join(proteinIds, ","), "gb");
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
                sequences.setGeneId(entrezMap.get(entrezId).getEnsemblId());

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
                        if (!TextUtils.isBlank(mRNAId)) {
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
                        }

                        String proteinId = transcriptsNode.at("/protein/accession_version").asText();
                        if (!TextUtils.isBlank(proteinId)) {
                            String proteinName= transcriptsNode.at("/protein/name").asText();
                            String isoformName= transcriptsNode.at("/protein/isoform_name").asText();
                            UrlEntity protein = new UrlEntity();
                            protein.setId(proteinId);
                            protein.setName(proteinName + " " + isoformName);
                            protein.setUrl(String.format(NCBI_PROTEIN_LINK, proteinId));
                            proteins.add(protein);
                        }
                    }
                    sequences.setMRNAs(mRNAs);
                    sequences.setProteins(proteins);
                }
                result.add(sequences);
            }
        }
        return result;
    }

    private List<GeneRefSection> parseSequencesAsTable(final String json, final Map<String, GeneId> entrezMap)
            throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonNodes = objectMapper.readTree(json);
        final JsonNode genes = jsonNodes.at("/genes");
        final List<GeneRefSection> result = new ArrayList<>();
        if (genes.isArray()) {
            final Iterator<JsonNode> elements = genes.elements();
            while (elements.hasNext()) {
                final GeneRefSection geneRefSection = new GeneRefSection();
                final JsonNode node = elements.next();

                final String entrezId = node.at("/gene/gene_id").asText();
                geneRefSection.setGeneId(entrezMap.get(entrezId).getEnsemblId());

                final JsonNode referenceNodes = node.at("/gene/reference_standards");
                if (referenceNodes.isArray()) {
                    final Iterator<JsonNode> referenceElements = referenceNodes.elements();
                    if (referenceElements.hasNext()) {
                        JsonNode referenceNode = referenceElements.next();
                        String referenceId = referenceNode.at("/gene_range/accession_version").asText();
                        UrlEntity reference = new UrlEntity();
                        reference.setId(referenceId);
                        reference.setUrl(String.format(NCBI_NUCCORE_LINK, referenceId));
                        geneRefSection.setReference(reference);
                    }
                }
                final List<GeneSequence> geneSequences = new ArrayList<>();
                final JsonNode transcriptsNodes = node.at("/gene/transcripts");
                if (transcriptsNodes.isArray()) {
                    final Iterator<JsonNode> transcriptsElements = transcriptsNodes.elements();
                    while (transcriptsElements.hasNext()) {
                        JsonNode transcriptsNode = transcriptsElements.next();
                        GeneSequence geneSequence = new GeneSequence();

                        String mRNAId = transcriptsNode.at("/accession_version").asText();
                        if (!TextUtils.isBlank(mRNAId)) {
                            Sequence mRNA = new Sequence();
                            mRNA.setId(mRNAId);
                            mRNA.setLength(transcriptsNode.at("/length").asInt());
                            mRNA.setUrl(String.format(NCBI_NUCCORE_LINK, mRNAId));
                            mRNA.setGenomic(transcriptsNode.at("/genomic_range/accession_version").asText());
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
                            geneSequence.setMRNA(mRNA);
                        }

                        String proteinId = transcriptsNode.at("/protein/accession_version").asText();
                        if (!TextUtils.isBlank(proteinId)) {
                            String proteinName= transcriptsNode.at("/protein/name").asText();
                            String isoformName= transcriptsNode.at("/protein/isoform_name").asText();
                            Sequence protein = new Sequence();
                            protein.setId(proteinId);
                            protein.setLength(transcriptsNode.at("/protein/length").asInt());
                            protein.setName(proteinName + " " + isoformName);
                            protein.setUrl(String.format(NCBI_PROTEIN_LINK, proteinId));
                            geneSequence.setProtein(protein);
                        }
                        geneSequences.add(geneSequence);
                    }
                    geneRefSection.setSequences(geneSequences);
                }
                final JsonNode proteinNodes = node.at("/gene/proteins");
                if (proteinNodes.isArray()) {
                    final Iterator<JsonNode> proteinElements = proteinNodes.elements();
                    while (proteinElements.hasNext()) {
                        JsonNode proteinNode = proteinElements.next();
                        GeneSequence geneSequence = new GeneSequence();
                        String proteinId = proteinNode.at("/accession_version").asText();
                        if (!TextUtils.isBlank(proteinId)) {
                            String proteinName= proteinNode.at("/name").asText();
                            Sequence protein = new Sequence();
                            protein.setId(proteinId);
                            protein.setLength(proteinNode.at("/length").asInt());
                            protein.setName(proteinName);
                            protein.setUrl(String.format(NCBI_PROTEIN_LINK, proteinId));
                            geneSequence.setProtein(protein);
                        }
                        geneSequences.add(geneSequence);
                    }
                    geneRefSection.setSequences(geneSequences);
                }
                result.add(geneRefSection);
            }
        }
        return result;
    }
}
