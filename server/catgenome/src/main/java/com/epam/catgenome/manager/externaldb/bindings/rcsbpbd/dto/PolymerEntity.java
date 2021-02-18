package com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class PolymerEntity {
    @JsonProperty("uniprots")
    private List<Uniprot> uniprots;

    @JsonProperty("rcsb_polymer_entity")
    private RCSBPolymerEntity rcsbPolymerEntity;

    @JsonProperty("rcsb_polymer_entity_container_identifiers")
    private Identifiers identifiers;

    @JsonProperty("polymer_entity_instances")
    private List<PolymerEntityInstance> entityInstances;

    @Getter
    public static class Uniprot {
        @JsonProperty("rcsb_uniprot_entry_name")
        private List<String> names;
    }

    @Getter
    public static class RCSBPolymerEntity {
        @JsonProperty("pdbx_description")
        private String description;
    }

    @Getter
    public static class Identifiers {
        @JsonProperty("auth_asym_ids")
        private List<String> ids;
    }
}
