package com.epam.catgenome.entity.blast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlastTaskOrganism {
    private Long id;
    private Long taskId;
    private Long organism;
}
