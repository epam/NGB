package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class BlastRequestInfo {
    private long requestId;
    private String status;
    private String taskType;
    private Date createdDate;
    private String reason;
}
