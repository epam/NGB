package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlastRequestInfo {
    private long requestId;
    private String status;
    private String taskType;
    private String createdDate;
    private String reason;
}
