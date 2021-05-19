package com.epam.catgenome.util.db;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Filter {
    private String field;
    private String value;

    public Filter(String field, String value) {
        this.field = field;
        this.value = value;
    }
    public Filter() {}
}
