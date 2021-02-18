package com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class PolymerEntityInstance {
    @JsonProperty("rcsb_polymer_instance_feature")
    public List<InstanceFeature> instanceFeatures;

    @JsonProperty("rcsb_id")
    public String id;
}
