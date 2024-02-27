package com.epam.catgenome.entity.llm;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomChatCompletion {
    private String model;
    private String prompt;
}
