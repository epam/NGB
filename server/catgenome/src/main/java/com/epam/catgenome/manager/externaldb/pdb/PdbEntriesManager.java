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

package com.epam.catgenome.manager.externaldb.pdb;

import com.epam.catgenome.controller.vo.target.StructuresSearchRequest;
import com.epam.catgenome.entity.externaldb.target.opentargets.UrlEntity;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.SearchResult;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.UnusedPrivateMethod")
public class PdbEntriesManager extends HttpDataManager {

    private static final String LOCATION = "https://www.rcsb.org/search/data";
    private static final String RCSB_LINK = "https://www.rcsb.org/structure/%s";
    private static final String GROUP = "group";
    private static final String AND = "and";
    private static final String STRUCT_TITLE = "struct.title";
    private static final int MAX_PAGE_SIZE = 1000;

    @SneakyThrows
    public SearchResult<Structure> getStructures(final StructuresSearchRequest request, final List<String> geneNames) {
        final ObjectMapper mapper = new ObjectMapper();
        final String jsonString = mapper.writeValueAsString(convertRequest(request, geneNames));
        final JSONObject jsonObject = new JSONObject(jsonString);
        final String structures = fetchData(LOCATION, jsonObject);
        final SearchResult<Structure> result = parse(structures);
        setProteinChains(result.getItems());
        return result;
    }

    @SneakyThrows
    public List<Structure> getAllStructures(final List<String> geneNames) {
        final ObjectMapper mapper = new ObjectMapper();
        final String jsonString = mapper.writeValueAsString(getAllStructuresRequest(geneNames));
        final JSONObject jsonObject = new JSONObject(jsonString);
        final String structures = fetchData(LOCATION, jsonObject);
        final List<Structure> result = parseStructures(structures);
        setProteinChains(result);
        return result;
    }

    @SneakyThrows
    public long getStructuresCount(final List<String> geneNames) {
        final ObjectMapper mapper = new ObjectMapper();
        final String jsonString = mapper.writeValueAsString(totalCountRequest(geneNames));
        final JSONObject jsonObject = new JSONObject(jsonString);
        final String structures = fetchData(LOCATION, jsonObject);
        return parseTotalCount(structures);
    }

    @SneakyThrows
    private Map<String, List<String>> getProteinChains(final List<String> entryIds) {
        final ObjectMapper mapper = new ObjectMapper();
        final String jsonString = mapper.writeValueAsString(getProteinChainsRequest(entryIds));
        final JSONObject jsonObject = new JSONObject(jsonString);
        final String structures = fetchData(LOCATION, jsonObject);
        return parseProteinChains(structures);
    }

    @SneakyThrows
    private SearchResult<Structure> parse(final String json) {
        final List<Structure> structures = parseStructures(json);
        final SearchResult<Structure> searchResult = new SearchResult<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode topNode = objectMapper.readTree(json);
        final Integer totalCount = topNode.at("/total_count").asInt();
        searchResult.setItems(structures);
        searchResult.setTotalCount(totalCount);
        return searchResult;
    }

    @SneakyThrows
    private List<Structure> parseStructures(final String json) {
        final List<Structure> structures = new ArrayList<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode topNode = objectMapper.readTree(json);
        final JsonNode resultSet = topNode.at("/result_set");
        if (resultSet.isArray()) {
            final Iterator<JsonNode> elements = resultSet.elements();
            while (elements.hasNext()) {
                JsonNode node = elements.next();
                String id = node.at("/data/entryId").asText();
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
                structures.add(structure);
            }
        }
        return structures;
    }

    private void setProteinChains(final List<Structure> result) {
        final List<String> entryIds = result.stream().map(UrlEntity::getId).collect(Collectors.toList());
        final Map<String, List<String>> chainsMap = getProteinChains(entryIds);
        result.forEach(s -> s.setProteinChains(chainsMap.get(s.getId())));
    }

    @SneakyThrows
    private Map<String, List<String>> parseProteinChains(final String json) {
        final Map<String, List<String>> chainsMap = new HashMap<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode topNode = objectMapper.readTree(json);
        final JsonNode resultSet = topNode.at("/result_set");
        if (resultSet.isArray()) {
            final Iterator<JsonNode> elements = resultSet.elements();
            while (elements.hasNext()) {
                JsonNode node = elements.next();
                String id = node.at("/data/entryId").asText();
                final JsonNode proteinChainsNode = node.at("/data/entity/chains");
                if (proteinChainsNode.isArray()) {
                    final Iterator<JsonNode> proteinChainsElements = proteinChainsNode.elements();
                    final List<String> proteinChains = new ArrayList<>();
                    while (proteinChainsElements.hasNext()) {
                        JsonNode proteinChainsElement = proteinChainsElements.next();
                        proteinChains.add(proteinChainsElement.asText());
                    }
                    if (chainsMap.containsKey(id)) {
                        chainsMap.get(id).addAll(proteinChains);
                    } else {
                        chainsMap.put(id, proteinChains);
                    }
                }
            }
        }
        return chainsMap;
    }

    @SneakyThrows
    private long parseTotalCount(final String json) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode topNode = objectMapper.readTree(json);
        return topNode.at("/total_count").asLong();
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
        return buildSearchSummaryRequest(request);
    }

    @SneakyThrows
    private StructureRequest getProteinChainsRequest(final List<String> entryIds) {
        final StructureRequest.RequestOptions options = getEntryIdsOptions();
        final StructureRequest.Query query = getEntryIdsQuery(entryIds);
        final StructureRequest.Request request = StructureRequest.Request.builder()
                .requestOptions(options)
                .query(query)
                .returnType("polymer_entity")
                .build();
        return buildSearchSummaryRequest(request);
    }

    @SneakyThrows
    private StructureRequest getAllStructuresRequest(final List<String> geneNames) {
        final StructureRequest.RequestOptions options = getEntryIdsOptions();
        final StructureRequest.Query query = getGenesQuery(geneNames);
        final StructureRequest.Request request = StructureRequest.Request.builder()
                .requestOptions(options)
                .query(query)
                .returnType("entry")
                .build();
        return buildSearchSummaryRequest(request);
    }

    @SneakyThrows
    private StructureRequest totalCountRequest(final List<String> geneNames) {
        final StructureRequest.RequestOptions options = StructureRequest.RequestOptions.builder()
                .returnCounts(true)
                .build();
        final StructuresSearchRequest structureSearchRequest = new StructuresSearchRequest();
        final StructureRequest.Query query = getQuery(structureSearchRequest, geneNames);
        final StructureRequest.Request request = StructureRequest.Request.builder()
                .requestOptions(options)
                .query(query)
                .returnType("entry")
                .build();
        return buildSearchSummaryRequest(request);
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
        if (!TextUtils.isBlank(structureSearchRequest.getName())) {
            nodes.add(getNameQuery(structureSearchRequest.getName()));
        }
        final StructureRequest.Query query = new StructureRequest.Query();
        query.setType(GROUP);
        query.setLogicalOperator(AND);
        query.setNodes(nodes);
        return query;
    }

    private static StructureRequest.Query getEntryIdsQuery(final List<String> entryIds) {
        final List<StructureRequest.Query> nodes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(entryIds)) {
            nodes.add(getEntriesQuery(entryIds));
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
            StructureRequest.FullTextParameters parameters = StructureRequest.FullTextParameters.builder()
                    .value(geneName)
                    .build();
            StructureRequest.FullTextQuery parametersQuery = StructureRequest.FullTextQuery.builder()
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
        genesQuery.setNodes(Collections.singletonList(query));

        return genesQuery;
    }

    private static StructureRequest.Query getEntriesQuery(final List<String> entryIds) {
        final StructureRequest.AttributeListParameters parameters = new StructureRequest.AttributeListParameters();
        parameters.setAttribute(StructureRequestField.ENTRY_ID.getValue());
        parameters.setOperator("in");
        parameters.setNegation(false);
        parameters.setValue(entryIds);
        final StructureRequest.AttributesQuery attributesQuery = getTextAttributesQuery(parameters);
        final StructureRequest.Query query = new StructureRequest.Query();
        query.setType(GROUP);
        query.setLogicalOperator(AND);
        query.setNodes(Collections.singletonList(attributesQuery));

        final StructureRequest.Query entriesQuery = new StructureRequest.Query();
        entriesQuery.setType(GROUP);
        entriesQuery.setLogicalOperator(AND);
        entriesQuery.setNodes(Collections.singletonList(query));

        return entriesQuery;
    }

    private static StructureRequest.Query getNameQuery(final String name) {
        final StructureRequest.AttributeTextParameters parameters = new StructureRequest.AttributeTextParameters();
        parameters.setAttribute(STRUCT_TITLE);
        parameters.setOperator("contains_phrase");
        parameters.setNegation(false);
        parameters.setValue(name);
        final StructureRequest.AttributesQuery attributesQuery = getTextAttributesQuery(parameters);
        final StructureRequest.Query query = new StructureRequest.Query();
        query.setType(GROUP);
        query.setLogicalOperator(AND);
        query.setNodes(Collections.singletonList(attributesQuery));

        final StructureRequest.Query entriesQuery = new StructureRequest.Query();
        entriesQuery.setType(GROUP);
        entriesQuery.setLogicalOperator(AND);
        entriesQuery.setNodes(Collections.singletonList(query));

        return entriesQuery;
    }

    private static StructureRequest.RequestOptions getOptions(final StructuresSearchRequest request) {
        final int start = ((request.getPage() - 1) * request.getPageSize());
        final StructureRequest.Paginate paginate = StructureRequest.Paginate.builder()
                .start(start)
                .rows(request.getPageSize())
                .build();
        final StructureRequest.Sort sort = StructureRequest.Sort.builder()
                .sortBy(Optional.ofNullable(request.getOrderBy()).orElse(StructureRequestField.ENTRY_ID).getValue())
                .direction(Optional.ofNullable(request.getReverse()).orElse(false) ? "desc" : "asc")
                .build();
        return StructureRequest.RequestOptions.builder()
                .paginate(paginate)
                .sort(Collections.singletonList(sort))
                .build();
    }

    private static StructureRequest.RequestOptions getEntryIdsOptions() {
        final StructureRequest.Paginate paginate = StructureRequest.Paginate.builder()
                .start(0)
                .rows(MAX_PAGE_SIZE)
                .build();
        return StructureRequest.RequestOptions.builder()
                .paginate(paginate)
                .returnCounts(false)
                .build();
    }

    private static StructureRequest.AttributesQuery getTextAttributesQuery(
            final StructureRequest.AttributeParameters params) {
        return StructureRequest.AttributesQuery.builder()
                .type("terminal")
                .service("text")
                .parameters(params)
                .build();
    }

    private static StructureRequest buildSearchSummaryRequest(final StructureRequest.Request request) {
        return StructureRequest.builder()
                .report("search_summary")
                .request(request)
                .build();
    }
}
