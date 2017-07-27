package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;

import java.util.List;
import java.util.Map;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public interface FeatureIndexer<F, E extends FeatureIndexEntry> {
    void addFeatureToIndex(F feature, Map<String, Chromosome> chromosomeMap);
    List<E> postProcessIndexEntries(List<GeneFile> geneFiles,
                                                    Chromosome chromosome);
    void clear();
}
