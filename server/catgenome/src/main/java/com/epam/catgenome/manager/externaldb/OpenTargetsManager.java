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

package com.epam.catgenome.manager.externaldb;

import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.join;


@Service
public class OpenTargetsManager extends HttpDataManager{

    private static final String LOCATION = "https://api.platform.opentargets.org/api/v4/graphql";
    private static final String COMP_GENOMICS_QUERY = "{\"operationName\":\"CompGenomics\",\"query\":\"query " +
            "CompGenomics{targets(ensemblIds:[\\\"%s\\\"]){id\\nhomologues {speciesId\\nspeciesName\\n" +
            "homologyType\\ntargetGeneId\\ntargetGeneSymbol}}}\"}";

    @SneakyThrows
    public Map<String, List<HomologGroup>> getHomologues(final List<String> ensemblIds) {
        final JSONObject jsonObject = new JSONObject(String.format(COMP_GENOMICS_QUERY,
                join(ensemblIds, "\\\",\\\"")));
        final String structures = fetchData(LOCATION, jsonObject);
        return parseHomologues(structures);
    }

    @SneakyThrows
    private Map<String, List<HomologGroup>> parseHomologues(final String json) {
        final Map<String, List<HomologGroup>> homologuesMap = new HashMap<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode topNode = objectMapper.readTree(json);
        final JsonNode resultSet = topNode.at("/data/targets");
        if (resultSet.isArray()) {
            final Iterator<JsonNode> elements = resultSet.elements();
            while (elements.hasNext()) {
                List<HomologGroup> homologues = new ArrayList<>();
                JsonNode node = elements.next();
                String id = node.at("/id").asText();
                final JsonNode homologuesNode = node.at("/homologues");
                if (homologuesNode.isArray()) {
                    final Iterator<JsonNode> homologuesElements = homologuesNode.elements();
                    while (homologuesElements.hasNext()) {
                        JsonNode homologuesElement = homologuesElements.next();
                        HomologGroup homologGroup = HomologGroup.builder()
                                .ensemblId(homologuesElement.at("/targetGeneId").asText())
                                .geneName(homologuesElement.at("/targetGeneSymbol").asText())
                                .taxId(homologuesElement.at("/speciesId").asLong())
                                .speciesCommonName(homologuesElement.at("/speciesName").asText())
                                .homologyType(homologuesElement.at("/homologyType").asText())
                                .build();
                        homologues.add(homologGroup);
                    }
                }
                homologuesMap.put(id, homologues);
            }
        }
        return homologuesMap;
    }
}
