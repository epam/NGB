package com.epam.catgenome.entity.blast;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BlastDataBase {
    private Long id;
    private String name;
    private String path;
    private String type;
}
