package com.epam.catgenome.util.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class PagingInfo {
    private int pageSize;
    private int pageNum;
}
