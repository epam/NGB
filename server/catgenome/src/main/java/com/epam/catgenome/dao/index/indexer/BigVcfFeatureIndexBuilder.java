/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.dao.index.indexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.vcf.GenotypeData;
import com.epam.catgenome.entity.vcf.OrganismType;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.manager.vcf.reader.VcfFileReader;
import com.epam.catgenome.util.Utils;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.commons.math3.util.MathUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epam.catgenome.manager.vcf.reader.VcfFileReader.NO_STRAIN_GENOTYPE_STRING;

/**
 * An implementation of {@link FeatureIndexBuilder}, that indexes <b>large</b> VCF file entries: {@link VariantContext}
 */
public class BigVcfFeatureIndexBuilder extends VcfFeatureIndexBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BigVcfFeatureIndexBuilder.class);

    private IndexWriter writer;
    private StandardAnalyzer analyzer;
    private List<GeneFile> geneFiles;
    private AbstractDocumentBuilder creator;
    private FacetsConfig facetsConfig;
    private VcfFile vcfFile;

    public BigVcfFeatureIndexBuilder(VcfFilterInfo filterInfo, VCFHeader vcfHeader,
            FeatureIndexDao featureIndexDao, VcfFile featureFile,
            FileManager fileManager, List<GeneFile> geneFiles, Integer indexBufferSize) throws IOException {
        super(filterInfo, vcfHeader, featureIndexDao);
        this.analyzer = new StandardAnalyzer();
        Directory index = fileManager.createIndexForFile(featureFile);
        this.writer = new IndexWriter(index, new IndexWriterConfig(analyzer).setOpenMode(
                IndexWriterConfig.OpenMode.CREATE_OR_APPEND).setRAMBufferSizeMB(indexBufferSize));
        this.geneFiles = geneFiles;
        this.creator = new BigVcfDocumentBuilder();
        this.facetsConfig = creator.createFacetsConfig(filterInfo);
        this.vcfFile = featureFile;
    }

    @Override
    protected List<VcfIndexEntry> simplify(VcfIndexEntry indexEntry, Set<VariationGeneInfo> geneIds,
            String geneIdsString, String geneNamesString, Set<VariationType> types) {
        indexEntry.setGeneIds(geneIdsString);
        indexEntry.setGeneNames(geneNamesString);
        indexEntry.setVariationTypes(types);
        indexEntry.setVariationType(types.stream().findFirst().orElse(VariationType.UNK));
        indexEntry.setFailedFilters(indexEntry.getVariantContext().getFilters());

        List<String> geneIdList = new ArrayList<>(geneIds.size());
        List<String> geneNameList = new ArrayList<>(geneIds.size());
        for (VariationGeneInfo i : geneIds) {
            geneIdList.add(i.geneId);
            geneNameList.add(i.geneName);
        }

        indexEntry.setGeneIdList(geneIdList);
        indexEntry.setGeneNameList(geneNameList);

        return Collections.singletonList(indexEntry);
    }

    @Override public void add(VariantContext context, Map<String, Chromosome> chromosomeMap) {
        if (chromosomeMap.containsKey(context.getContig()) || chromosomeMap
                .containsKey(Utils.changeChromosomeName(context.getContig()))) {
            Chromosome chromosome =
                    Utils.getFromChromosomeMap(chromosomeMap, context.getContig());
            VcfIndexEntry masterEntry = new VcfIndexEntry();
            masterEntry.setUuid(UUID.randomUUID());
            masterEntry.setFeatureId(context.getID());
            masterEntry
                    .setChromosome(chromosome);
            masterEntry.setStartIndex(context.getStart());
            masterEntry.setEndIndex(context.getEnd());
            masterEntry.setFeatureType(FeatureType.VARIATION);
            masterEntry.setInfo(filterInfoByWhiteList(context, getFilterInfo(), getVcfHeader()));
            masterEntry.setVariantContext(context);

            double qual = context.getPhredScaledQual();
            masterEntry.setQuality(
                    MathUtils.equals(qual, VcfManager.HTSJDK_WRONG_QUALITY) ? 0D : qual);

            List<OrganismType> organismTypes = new ArrayList<>();
            for (int i = 0; i < context.getAlternateAlleles().size(); i++) {
                Variation variation = VcfFileReader.createVariation(context, getVcfHeader(), i);
                organismTypes.addAll(variation.getGenotypeData().values().stream()
                        .map(GenotypeData::getOrganismType).collect(Collectors.toList()));
            }

            if (!organismTypes.isEmpty() && organismTypes.stream()
                    .anyMatch(type -> type.equals(OrganismType.NO_VARIATION))) {
                return;
            }

            for (String sampleName: context.getSampleNames()) {
                if (!context.getGenotype(sampleName).getGenotypeString().equals(NO_STRAIN_GENOTYPE_STRING)) {
                    Set<String> sampleNames = new HashSet<>();
                    sampleNames.add(sampleName);
                    masterEntry.setSampleNames(sampleNames);
                    VcfIndexEntry indexEntry = build(masterEntry, geneFiles, chromosome);
                    Document document = creator.buildDocument(indexEntry, vcfFile.getId());
                    try {
                        writer.addDocument(facetsConfig.build(document));
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Failed to create index");
                    }
                }
            }
        }
    }

    public void close() {
        try {
            this.writer.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
