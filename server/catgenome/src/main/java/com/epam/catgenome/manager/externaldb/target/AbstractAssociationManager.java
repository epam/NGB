/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.target;

import com.epam.catgenome.manager.export.ExportField;
import com.epam.catgenome.manager.export.ExportUtils;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.manager.index.Filter;
import com.epam.catgenome.util.FileFormat;
import lombok.Getter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;

public abstract class AbstractAssociationManager<T> extends AbstractIndexManager<T> {

    public AbstractAssociationManager(String indexDirectory, int topHits) {
        super(indexDirectory, topHits);
    }

    public SearchResult<T> search(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final Query query = buildQuery(request.getGeneIds(), request.getFilters());
        return search(request, query);
    }

    public static Query getByGeneIdsQuery(final List<String> ids) throws ParseException {
        return getByIdsQuery(ids, IndexFields.GENE_ID.name());
    }

    public List<T> searchByGeneIds(final List<String> ids) throws ParseException, IOException {
        return search(ids, IndexFields.GENE_ID.name());
    }

    public byte[] export(final List<String> geneIds, final FileFormat format, final boolean includeHeader)
            throws ParseException, IOException {
        final List<ExportField<T>> exportFields = getExportFields().stream().filter(AssociationExportField::isExport)
                .collect(Collectors.toList());
        return ExportUtils.export(searchByGeneIds(geneIds), exportFields, format, includeHeader);
    }

    public Query buildQuery(final List<String> geneIds, final List<Filter> filters) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(getByGeneIdsQuery(geneIds), BooleanClause.Occur.MUST);
        if (filters != null) {
            for (Filter filter: filters) {
                addFieldQuery(mainBuilder, filter);
            }
        }
        return mainBuilder.build();
    }

    public abstract void addFieldQuery(BooleanQuery.Builder builder, Filter filter);
    public abstract List<AssociationExportField<T>> getExportFields();

    @Getter
    private enum IndexFields {
        GENE_ID;
    }
}
