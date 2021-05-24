package com.epam.catgenome.entity.blast;

import com.epam.catgenome.manager.blast.dto.Entry;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BlastTaskResult {
    private long size;
    private String tool;
    private List<Entry> entries;
    private String status;
    private String message;
}
