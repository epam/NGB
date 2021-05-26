package com.epam.catgenome.util.db;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class QueryParameters {
    private PagingInfo pagingInfo;
    private List<Filter> filters;
    private List<SortInfo> sortInfos;
}
