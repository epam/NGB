package com.epam.catgenome.entity.task;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Organism {
    private Long id;
    private Long taskId;
    private String organism;
}
