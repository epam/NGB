package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.exception.GeneReadingException;

import java.util.List;
import java.util.Map;

/**
 * An interface, which implementations create {@link FeatureIndexEntry}'es for various features
 */
public interface FeatureIndexBuilder<F, E extends FeatureIndexEntry> {
    /**
     * Adds a feature to feature index
     *
     * @param feature
     * @param chromosomeMap map of chromosomes to their names
     */
    void add(F feature, Map<String, Chromosome> chromosomeMap);

    /**
     * Post processes fetched index entries to add gene information and split them to resolve ambiguous fields
     *
     * @param geneFiles  list of {@link GeneFile}s to fetch gene information from
     * @param chromosome a {@code Chromosome}, from which entries came
     * @return a list of post-processed index entries, ready to write into index
     * @throws GeneReadingException if an exception was thrown when reading genes information
     */
    List<E> build(List<GeneFile> geneFiles, Chromosome chromosome);

    /**
     * Clears an FeatureIndexBuilder
     */
    void clear();
}
