package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class RequestInfoPayload {
    private int requestId;
    private String status;
    private String taskType;
    private Date createdDate;
}
