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
