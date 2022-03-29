package com.epam.catgenome.entity.pathway;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpeciesDescription {
    private long taxId;
    private String speciesName;
}
