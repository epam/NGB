package com.epam.catgenome.util.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryParameters {
    private PagingInfo pagingInfo;
    private List<Filter> filters;
    private List<SortInfo> sortInfos;
}
