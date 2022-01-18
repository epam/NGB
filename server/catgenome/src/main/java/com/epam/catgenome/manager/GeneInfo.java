package com.epam.catgenome.manager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
public class GeneInfo {
    private String geneId;
    private String geneName;
    private boolean isExon;

    @Override public int hashCode() {
        return geneId.hashCode();
    }

    @Override public boolean equals(Object obj) {
        return obj != null && obj.getClass() == this.getClass() && Objects
                .equals(((GeneInfo) obj).geneId, geneId);
    }
}

