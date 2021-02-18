package com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class InstanceFeature {
    @JsonProperty("feature_positions")
    public List<FeaturePosition> positions;
}
