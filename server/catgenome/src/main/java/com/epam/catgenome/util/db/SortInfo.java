package com.epam.catgenome.util.db;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
public class SortInfo {
    private String field;
    private boolean ascending;

    public SortInfo() {}
    public SortInfo(String field, boolean ascending) {
        this.field = field;
        this.ascending = ascending;
    }
}
