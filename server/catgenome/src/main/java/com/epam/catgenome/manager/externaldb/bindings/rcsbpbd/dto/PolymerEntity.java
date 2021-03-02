/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class PolymerEntity {
    @JsonProperty("uniprots")
    private List<Uniprot> uniprots;

    @JsonProperty("rcsb_polymer_entity")
    private RCSBPolymerEntity rcsbPoly;

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
