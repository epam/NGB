package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import htsjdk.variant.vcf.VCFHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public class BigVcfFeatureIndexer extends VcfFeatureIndexer {
    public BigVcfFeatureIndexer(VcfFilterInfo filterInfo, VCFHeader vcfHeader, FeatureIndexDao featureIndexDao) {
        super(filterInfo, vcfHeader, featureIndexDao);
    }

    @Override
    protected List<VcfIndexEntry> simplify(VcfIndexEntry indexEntry, Set<VariationGeneInfo> geneIds,
                                           String geneIdsString, String geneNamesString, Set<VariationType> types)
    {
        indexEntry.setGeneIds(geneIdsString);
        indexEntry.setGeneNames(geneNamesString);
        indexEntry.setVariationTypes(types);
        indexEntry.setFailedFilters(indexEntry.getVariantContext().getFilters());

        List<String> geneIdList = new ArrayList<>(geneIds.size());
        List<String> geneNameList = new ArrayList<>(geneIds.size());
        for (VariationGeneInfo i : geneIds) {
            geneIdList.add(i.geneId);
            geneNameList.add(i.geneName);
        }

        indexEntry.setGeneIdList(geneIdList);
        indexEntry.setGeneIdList(geneNameList);

        return Collections.singletonList(indexEntry);
    }
}
