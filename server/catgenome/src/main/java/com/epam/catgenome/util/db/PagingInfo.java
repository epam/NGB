package com.epam.catgenome.util.db;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PagingInfo {
    private int pageSize;
    private int pageNum;
}
