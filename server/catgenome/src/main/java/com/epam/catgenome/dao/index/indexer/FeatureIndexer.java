package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.exception.GeneReadingException;

import java.util.List;
import java.util.Map;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public interface FeatureIndexer<F, E extends FeatureIndexEntry> {
    void addFeatureToIndex(F feature, Map<String, Chromosome> chromosomeMap);

    /**
     * Post processes fetched VCF index entries to add gene information and split them to resolve ambiguous fields
     * @param geneFiles list of {@link GeneFile}s to fetch gene information from
     * @param chromosome a {@code Chromosome}, from which entries came
     * @return a list of post-processed index entries, ready to write into index
     * @throws GeneReadingException if an exception was thrown when reading genes information
     */
    List<E> postProcessIndexEntries(List<GeneFile> geneFiles,
                                                    Chromosome chromosome);
    void clear();
}
