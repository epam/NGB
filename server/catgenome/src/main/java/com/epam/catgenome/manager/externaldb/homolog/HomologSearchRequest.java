package com.epam.catgenome.manager.externaldb.homolog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HomologSearchRequest {
    private String geneId;
    private Integer page;
    private Integer pageSize;
}
