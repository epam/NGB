package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;

import java.util.List;
import java.util.Map;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public interface FeatureIndexer<T> {
    void addFeatureToIndex(T context, Map<String, Chromosome> chromosomeMap);
    List<? extends FeatureIndexEntry> postProcessIndexEntries(List<GeneFile> geneFiles,
                                                    Chromosome chromosome);
    void clear();
}
