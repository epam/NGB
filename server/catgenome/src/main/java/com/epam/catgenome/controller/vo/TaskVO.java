package com.epam.catgenome.controller.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TaskVO {
    private String title;
    private String statusReason;
    private String query;
    private Long databaseId;
    private List<Long> organisms;
    private List<Long> excludedOrganisms;
    private String executable;
    private String algorithm;
    private Map<String, String> parameters;
    private String options;
}
