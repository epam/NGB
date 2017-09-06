package com.epam.catgenome.dao.index.indexer;

import java.io.IOException;
import java.util.*;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.vcf.*;
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
                organismTypes.add(variation.getGenotypeData().getOrganismType());
            }

            if (!organismTypes.isEmpty() && organismTypes.stream()
                    .anyMatch(type -> type.equals(OrganismType.NO_VARIATION))) {
                return;
            }

            VcfIndexEntry indexEntry = build(masterEntry, geneFiles, chromosome);
            Document document = creator.buildDocument(indexEntry, vcfFile.getId());
            try {
                writer.addDocument(facetsConfig.build(document));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to create index");
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
