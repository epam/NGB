package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class BlastRequest {
    private String blastTool;
    private String algorithm;
    private String dbName;
    private List<Long> taxIds;
    private List<Long> excludedTaxIds;
    private String query;
    private Long maxTargetSequence;
    private Double expectedThreshold;
    private String options;
}
