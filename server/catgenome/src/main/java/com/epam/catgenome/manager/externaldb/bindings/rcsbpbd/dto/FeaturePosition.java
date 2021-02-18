package com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class FeaturePosition {
    @JsonProperty("beg_seq_id")
    public Integer start;

    @JsonProperty("end_seq_id")
    public Integer end;
}
