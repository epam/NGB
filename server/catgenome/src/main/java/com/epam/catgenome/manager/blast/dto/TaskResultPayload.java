package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TaskResultPayload {
    private long size;
    private List<Entry> entries;
}