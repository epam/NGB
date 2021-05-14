package com.epam.catgenome.controller.vo;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
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
    private List<String> organisms;
    private String executable;
    private String algorithm;
    private Map<String, String> parameters;

}
