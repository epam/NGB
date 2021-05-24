package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestResult {
    private RequestResultPayload payload;
    private String status;
    private String message;
}
