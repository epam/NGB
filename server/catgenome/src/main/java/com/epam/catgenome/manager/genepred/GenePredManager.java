/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
package com.epam.catgenome.manager.genepred;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.controller.tools.FeatureFileSortRequest;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.parser.GffCodec;
import com.epam.catgenome.manager.gene.parser.GffFeature;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.gene.writer.Gff3FeatureImpl;
import com.epam.catgenome.manager.gene.writer.Gff3Writer;
import com.epam.catgenome.manager.tools.ToolsManager;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.samtools.util.CloseableIterator;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.broad.igv.feature.BasicFeature;
import org.broad.igv.feature.Exon;
import org.broad.igv.feature.tribble.UCSCGeneTableCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.manager.genepred.GenePredUtils.SOURCE;
import static com.epam.catgenome.util.Utils.DOT;
import static java.util.stream.Collectors.groupingBy;

@Service
public class GenePredManager {

    @Autowired
    private ToolsManager toolsManager;

    public void genePredToGff(final String fileName,
                              final String path,
                              final Path gffFilePath,
                              final String tempDir) {
        final Path tempGffFilePath = Paths.get(tempDir,
                FilenameUtils.removeExtension(fileName) + GffCodec.GFF_EXTENSION);
        convert(path, tempGffFilePath);

        final FeatureFileSortRequest sortRequest = new FeatureFileSortRequest();
        sortRequest.setOriginalFilePath(tempGffFilePath.toString());
        sortRequest.setSortedFilePath(gffFilePath.toString());
        toolsManager.sortFeatureFile(sortRequest);
    }

    @SneakyThrows
    public void convert(final String genePredFile, final Path gffFilePath) {
        Assert.notNull(genePredFile, getMessage(MessageCode.RESOURCE_NOT_FOUND));
        final UCSCGeneTableCodec codec = new UCSCGeneTableCodec(UCSCGeneTableCodec.Type.GENEPRED, null);
        List<BasicFeature> basicFeatures;
        try (AbstractFeatureReader<BasicFeature, ?> reader = AbstractFeatureReader.getFeatureReader(genePredFile,
                codec, false, null)) {
            final CloseableIterator<BasicFeature> iterator = reader.iterator();
            Assert.isTrue(iterator.hasNext(), getMessage(MessageCode.ERROR_GENBANK_FILE_READING));
            basicFeatures = iterator.toList();
            iterator.close();
        }

        try (Gff3Writer gff3Writer = new Gff3Writer(gffFilePath)) {
            final Map<String, List<BasicFeature>> featuresByName = basicFeatures.stream()
                    .collect(groupingBy(BasicFeature::getName));
            for (String key : featuresByName.keySet()) {
                List<BasicFeature> features = featuresByName.get(key);
                BasicFeature gene = features.get(0);
                writeGene(gff3Writer, gene);
                for (BasicFeature feature : features) {
                    writeTranscript(gff3Writer, feature, gene.getName());
                    for (Exon exon: feature.getExons()) {
                        writeExon(gff3Writer, feature, exon, feature.getIdentifier());
                    }
                }
            }
        }
    }

    private void writeGene(final Gff3Writer gff3Writer, final BasicFeature f) throws IOException {
        final String type = GffFeature.GENE_FEATURE_NAME;
        final Map<String, List<String>> attributes = new LinkedHashMap<>();
        attributes.put(GeneUtils.ID_ATTR, Collections.singletonList(type + ":" + f.getName()));
        attributes.put(GeneUtils.NAME_ATTR, Collections.singletonList(f.getName()));
        attributes.put(GeneUtils.GENE_ID_ATTR, Collections.singletonList(f.getName()));
        attributes.put(GeneUtils.GENE_NAME_ATTR, Collections.singletonList(f.getName()));

        gff3Writer.addFeature(convertFeature(f, type, attributes, f.getStart(), f.getEnd()));
    }

    private void writeTranscript(final Gff3Writer gff3Writer,
                                 final BasicFeature f,
                                 final String geneId) throws IOException {

        final String type = GffFeature.TRANSCRIPT_FEATURE_NAME;
        final Map<String, List<String>> attributes = new LinkedHashMap<>();
        attributes.put(GeneUtils.ID_ATTR, Collections.singletonList(type + ":" + f.getIdentifier()));
        attributes.put(GeneUtils.PARENT_ATTR, Collections.singletonList("gene:" + geneId));
        attributes.put(GeneUtils.NAME_ATTR, Collections.singletonList(f.getIdentifier()));
        attributes.put(GeneUtils.TRANSCRIPT_ID_ATTR, Collections.singletonList(f.getIdentifier()));
        attributes.put(GeneUtils.TRANSCRIPT_NAME_ATTR, Collections.singletonList(f.getIdentifier()));

        gff3Writer.addFeature(convertFeature(f, type, attributes, f.getStart(), f.getEnd()));
    }

    private void writeExon(final Gff3Writer gff3Writer,
                           final BasicFeature f,
                           final Exon e,
                           final String transcriptId) throws IOException {
        final String type = GffFeature.EXON_FEATURE_NAME;

        final Map<String, List<String>> attributes = new LinkedHashMap<>();
        attributes.put(GeneUtils.ID_ATTR, Collections.singletonList(type + ":" + f.getIdentifier()));
        attributes.put(GeneUtils.PARENT_ATTR, Collections.singletonList("transcript:" + transcriptId));
        gff3Writer.addFeature(convertFeature(f, type, attributes, e.getStart(), e.getEnd()));
    }

    private Gff3FeatureImpl convertFeature(final BasicFeature f,
                                           final String type,
                                           final Map<String, List<String>> attributes,
                                           final int start,
                                           final int end) {
        return new Gff3FeatureImpl(
                f.getChr(),
                SOURCE,
                type,
                start,
                end,
                Float.isNaN(f.getScore()) ? DOT : String.valueOf(f.getScore()),
                StrandSerializable.valueOf(f.getStrand().name()),
                DOT,
                attributes
        );
    }
}
