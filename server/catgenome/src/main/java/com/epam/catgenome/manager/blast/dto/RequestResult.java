package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestResult {
    private BlastRequestResult payload;
    private String status;
    private String message;
}
