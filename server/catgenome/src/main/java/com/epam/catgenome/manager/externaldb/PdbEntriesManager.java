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

import com.epam.catgenome.controller.vo.target.StructuresSearchRequest;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.StructureRequest;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.StructureRequestField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.util.TextUtils;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class PdbEntriesManager extends HttpDataManager{

    private static final String LOCATION = "https://www.rcsb.org/search/data";
    private static final String RCSB_LINK = "https://www.rcsb.org/structure/%s";
    private static final String GROUP = "group";
    private static final String AND = "and";

    @SneakyThrows
    public SearchResult<Structure> getStructures(final StructuresSearchRequest request, final List<String> geneNames) {
        final ObjectMapper mapper = new ObjectMapper();
        final String jsonString = mapper.writeValueAsString(convertRequest(request, geneNames));
        final JSONObject jsonObject = new JSONObject(jsonString);
        final String structures = fetchData(LOCATION, jsonObject);
        return parseStructures(structures);
    }

    @SneakyThrows
    private SearchResult<Structure> parseStructures(final String json) {
        final List<Structure> structures = new ArrayList<>();
        final SearchResult<Structure> searchResult = new SearchResult<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode topNode = objectMapper.readTree(json);
        final Integer totalCount = topNode.at("/total_count").asInt();
        final JsonNode resultSet = topNode.at("/result_set");
        if (resultSet.isArray()) {
            final Iterator<JsonNode> elements = resultSet.elements();
            while (elements.hasNext()) {
                JsonNode node = elements.next();
                String id = node.at("/identifier").asText();
                Structure structure = Structure.builder()
                        .id(id)
                        .name(node.at("/data/title").asText())
                        .url(String.format(RCSB_LINK, id))
                        .source("PDB")
                        .build();
                final JsonNode exptl = node.at("/data/exptl");
                if (exptl.isArray()) {
                    final Iterator<JsonNode> exptlElements = exptl.elements();
                    if (exptlElements.hasNext()) {
                        JsonNode exptlElement = exptlElements.next();
                        structure.setMethod(exptlElement.at("/method").asText());
                        String resolution = exptlElement.at("/resolution").asText();
                        if (!TextUtils.isBlank(resolution)) {
                            structure.setResolution(Float.parseFloat(resolution));
                        }
                    }
                }
                final JsonNode proteinChainsNode = node.at("/data/proteinChains");
                if (proteinChainsNode.isArray()) {
                    final Iterator<JsonNode> proteinChainsElements = proteinChainsNode.elements();
                    final List<String> proteinChains = new ArrayList<>();
                    while (proteinChainsElements.hasNext()) {
                        JsonNode proteinChainsElement = proteinChainsElements.next();
                        proteinChains.add(proteinChainsElement.asText());
                    }
                    structure.setProteinChains(proteinChains);
                }
                structures.add(structure);
            }
        }
        searchResult.setItems(structures);
        searchResult.setTotalCount(totalCount);
        return searchResult;
    }

    @SneakyThrows
    private StructureRequest convertRequest(final StructuresSearchRequest structureSearchRequest,
                                            final List<String> geneNames) {
        final StructureRequest.RequestOptions options = getOptions(structureSearchRequest);
        final StructureRequest.Query query = getQuery(structureSearchRequest, geneNames);
        final StructureRequest.Request request = StructureRequest.Request.builder()
                .requestOptions(options)
                .query(query)
                .returnType("entry")
                .build();
        return StructureRequest.builder()
                .report("search_summary")
                .request(request)
                .build();
    }

    @NotNull
    private static StructureRequest.Query getQuery(final StructuresSearchRequest structureSearchRequest,
                                                   final List<String> geneNames) {
        final List<StructureRequest.Query> nodes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(structureSearchRequest.getEntryIds())) {
            nodes.add(getEntriesQuery(structureSearchRequest.getEntryIds()));
        }
        if (CollectionUtils.isNotEmpty(geneNames)) {
            nodes.add(getGenesQuery(geneNames));
        }
        final StructureRequest.Query query = new StructureRequest.Query();
        query.setType(GROUP);
        query.setLogicalOperator(AND);
        query.setNodes(nodes);
        return query;
    }

    private static StructureRequest.Query getGenesQuery(final List<String> geneNames) {
        final List<StructureRequest.Query> parametersQueryList = new ArrayList<>();
        for (String geneName : geneNames) {
            StructureRequest.GeneNameParameters parameters = StructureRequest.GeneNameParameters.builder()
                    .value(geneName)
                    .build();
            StructureRequest.GeneNamesQuery parametersQuery = StructureRequest.GeneNamesQuery.builder()
                    .type("terminal")
                    .service("full_text")
                    .parameters(parameters)
                    .build();
            parametersQueryList.add(parametersQuery);
        }
        final StructureRequest.Query query = new StructureRequest.Query();
        query.setType(GROUP);
        query.setLogicalOperator("or");
        query.setNodes(parametersQueryList);

        final StructureRequest.Query genesQuery = new StructureRequest.Query();
        genesQuery.setType(GROUP);
        genesQuery.setLogicalOperator(AND);
        genesQuery.setLabel("full_text");
        genesQuery.setNodes(Collections.singletonList(query));

        return genesQuery;
    }

    private static StructureRequest.Query getEntriesQuery(final List<String> entryIds) {
        final StructureRequest.EntryParameters parameters = StructureRequest.EntryParameters.builder()
                .attribute(StructureRequestField.ENTRY_ID.getValue())
                .operator("in")
                .negation(false)
                .value(entryIds)
                .build();
        final StructureRequest.EntriesQuery parametersQuery = StructureRequest.EntriesQuery.builder()
                .type("terminal")
                .service("text")
                .parameters(parameters)
                .build();
        final StructureRequest.Query query = new StructureRequest.Query();
        query.setType(GROUP);
        query.setLogicalOperator(AND);
        query.setNodes(Collections.singletonList(parametersQuery));

        final StructureRequest.Query entriesQuery = new StructureRequest.Query();
        entriesQuery.setType(GROUP);
        entriesQuery.setLogicalOperator(AND);
        entriesQuery.setLabel("text");
        entriesQuery.setNodes(Collections.singletonList(query));

        return entriesQuery;
    }

    private static StructureRequest.RequestOptions getOptions(final StructuresSearchRequest structureSearchRequest) {
        final int start = ((structureSearchRequest.getPage() - 1) * structureSearchRequest.getPageSize());
        final StructureRequest.Paginate paginate = StructureRequest.Paginate.builder()
                .start(start)
                .rows(structureSearchRequest.getPageSize())
                .build();
        final StructureRequest.Sort sort = StructureRequest.Sort.builder()
                .sortBy(structureSearchRequest.getOrderBy().getValue())
                .direction(structureSearchRequest.isReverse() ? "desc" : "asc")
                .build();
        return StructureRequest.RequestOptions.builder()
                .paginate(paginate)
                .sort(Collections.singletonList(sort))
                .build();
    }
}
