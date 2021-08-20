package com.epam.catgenome.util.db;

import lombok.*;

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
