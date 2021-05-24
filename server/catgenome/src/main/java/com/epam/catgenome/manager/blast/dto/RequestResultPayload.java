package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RequestResultPayload {
    private long size;
    private String tool;
    private List<Entry> entries;
}
