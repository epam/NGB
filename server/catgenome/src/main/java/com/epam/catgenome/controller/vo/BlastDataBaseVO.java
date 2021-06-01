package com.epam.catgenome.controller.vo;

import com.epam.catgenome.entity.blast.BlastDataBaseType;
import lombok.Getter;

@Getter
public class BlastDataBaseVO {
    private long id;
    private String name;
    private String path;
    private BlastDataBaseType type;
}
