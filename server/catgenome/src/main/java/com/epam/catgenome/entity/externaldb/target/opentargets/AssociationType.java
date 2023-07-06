/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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

package com.epam.catgenome.entity.externaldb.target.opentargets;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum AssociationType {
    OVERALL("overall"),
    GENETIC_ASSOCIATIONS("genetic_association"),
    SOMATIC_MUTATIONS("somatic_mutation"),
    DRUGS("known_drug"),
    PATHWAYS("affected_pathway"),
    TEXT_MINING("literature"),
    RNA_EXPRESSION("rna_expression"),
    ANIMAL_MODELS("animal_model");
    private final String name;
    private static final Map<String, AssociationType> VALUES_MAP = new HashMap<>();

    static {
        VALUES_MAP.put("overall", OVERALL);
        VALUES_MAP.put("genetic_association", GENETIC_ASSOCIATIONS);
        VALUES_MAP.put("somatic_mutation", SOMATIC_MUTATIONS);
        VALUES_MAP.put("known_drug", DRUGS);
        VALUES_MAP.put("affected_pathway", PATHWAYS);
        VALUES_MAP.put("literature", TEXT_MINING);
        VALUES_MAP.put("rna_expression", RNA_EXPRESSION);
        VALUES_MAP.put("animal_model", ANIMAL_MODELS);
    }

    public static AssociationType getByName(String name) {
        return VALUES_MAP.get(name);
    }
}
