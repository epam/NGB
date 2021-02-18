package com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Entry {
    private Struct struct;

    @JsonProperty("struct_keywords")
    private StructKeywords keywords;

    @JsonProperty("polymer_entities")
    private List<PolymerEntity> entities;

    @Getter
    public static class Struct {
        private String title;
    }

    @Getter
    public static class StructKeywords {
        @JsonProperty("pdbx_keywords")
        private String value;
    }


}
