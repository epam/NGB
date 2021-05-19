package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestInfo {
    private RequestInfoPayload payload;
    private String message;
    private String status;
}
