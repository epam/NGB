package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskResult {
    private TaskResultPayload payload;
    private String status;
    private String message;
}