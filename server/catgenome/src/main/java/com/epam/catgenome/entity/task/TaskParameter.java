package com.epam.catgenome.entity.task;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskParameter {
    private Long parameterId;
    private Long taskId;
    private String parameter;
    private String value;
}
