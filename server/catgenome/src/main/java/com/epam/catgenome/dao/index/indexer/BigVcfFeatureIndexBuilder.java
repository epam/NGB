package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link FeatureIndexBuilder}, that indexes <b>large</b> VCF file entries: {@link VariantContext}
 */
public class BigVcfFeatureIndexBuilder extends VcfFeatureIndexBuilder {
    public BigVcfFeatureIndexBuilder(VcfFilterInfo filterInfo, VCFHeader vcfHeader,
            FeatureIndexDao featureIndexDao, int maxFeaturesInMemory) {
        super(filterInfo, vcfHeader, featureIndexDao, maxFeaturesInMemory);
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
}
