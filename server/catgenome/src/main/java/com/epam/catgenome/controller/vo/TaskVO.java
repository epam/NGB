package com.epam.catgenome.controller.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TaskVO {
    private Long id;
    private String title;
    private Date createdDate;
    private Long status;
    private Date endDate;
    private String statusReason;
    private String query;
    private String database;
    private List<Long> organisms;
    private List<Long> excludedOrganisms;
    private String owner;
    private String executable;
    private String algorithm;
    private Map<String, String> parameters;
    private String options;
}
