package com.epam.catgenome.dao.index.searcher;

import java.io.IOException;
import java.util.List;

import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import org.apache.lucene.search.Query;

public interface LuceneIndexSearcher<T extends FeatureIndexEntry>  {

    IndexSearchResult<T> getSearchResults(List<? extends FeatureFile> files, Query query) throws IOException;
}
