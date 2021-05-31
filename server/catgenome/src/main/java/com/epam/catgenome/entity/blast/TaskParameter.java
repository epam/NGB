package com.epam.catgenome.entity.blast;

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
