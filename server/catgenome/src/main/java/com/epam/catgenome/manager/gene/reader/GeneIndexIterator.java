/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.manager.gene.reader;

import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFilterForm;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;

public class GeneIndexIterator implements Iterator<GeneIndexEntry> {

    private final GeneFilterForm filterForm;
    private final GeneFile geneFile;
    private final BiFunction<GeneFilterForm, GeneFile, IndexSearchResult<GeneIndexEntry>> fetchFunction;

    private Iterator<GeneIndexEntry> iterator;

    public GeneIndexIterator(final GeneFilterForm filterForm,
                             final GeneFile geneFile,
                             final BiFunction<GeneFilterForm, GeneFile,
                                     IndexSearchResult<GeneIndexEntry>> fetchFunction) {
        this.filterForm = filterForm;
        this.geneFile = geneFile;
        this.fetchFunction = fetchFunction;
        fetch();
    }


    @Override
    public boolean hasNext() {
        if (Objects.isNull(iterator)) {
            return false;
        }

        final boolean hasNext = iterator.hasNext();
        if (hasNext) {
            return true;
        }

        fetch();
        return !Objects.isNull(iterator) && iterator.hasNext();
    }

    @Override
    public GeneIndexEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return iterator.next();
    }

    private void fetch() {
        final IndexSearchResult<GeneIndexEntry> result = fetchFunction.apply(filterForm, geneFile);
        if (Objects.isNull(result) || CollectionUtils.isEmpty(result.getEntries())) {
            iterator = null;
            return;
        }
        iterator = result.getEntries().iterator();
    }
}
