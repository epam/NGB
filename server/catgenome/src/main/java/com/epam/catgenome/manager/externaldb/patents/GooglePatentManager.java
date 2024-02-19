/*
 * MIT License
 *
 * Copyright (c) 2024 EPAM Systems
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

package com.epam.catgenome.manager.externaldb.patents;

import com.epam.catgenome.controller.vo.target.PatentsSearchRequest;
import com.epam.catgenome.entity.externaldb.patents.google.GooglePatent;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.customsearch.v1.CustomSearchAPI;
import com.google.api.services.customsearch.v1.model.Search;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GooglePatentManager {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_SEARCH_RESULTS = 100;
    private final CustomSearchAPI api;
    private final List<String> keywords;
    private final String searchHost;
    private final String apiKey;
    private final String searchEngine;

    @SneakyThrows
    public GooglePatentManager(
            final @Value("${patent.google.host:https://patents.google.com}") String googlePatentHost,
            final @Value("${patent.google.keywords:drug,target,gene,type:PATENT}") String googleKeywords,
            final @Value("${patent.google.api.key:}") String apiKey,
            final @Value("${patent.google.search.engine:}") String searchEngine) {
        final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        final JacksonFactory defaultInstance = JacksonFactory.getDefaultInstance();
        this.api = new CustomSearchAPI.Builder(netHttpTransport, defaultInstance, null)
                .setApplicationName("patent search")
                .build();
        this.searchHost = googlePatentHost;
        this.apiKey = apiKey;
        this.searchEngine = searchEngine;
        this.keywords = Arrays.asList(StringUtils.split(googleKeywords, ","));
    }

    @SneakyThrows
    public SearchResult<GooglePatent> getProteinPatentsGoogle(final PatentsSearchRequest request) {
        final Search result = getSearch(buildTargetQuery(request.getName()),
                request.getPageSize() * (request.getPage() - 1) + 1,
                request.getPageSize());

        final List<GooglePatent> items = mapPatents(result);
        return new SearchResult<>(items, Integer.parseInt(result.getSearchInformation().getTotalResults()));
    }


    public List<GooglePatent> getAllPatents(final String query) {
        final List<GooglePatent> result = new ArrayList<>();
        final String fullQuery = buildTargetQuery(query);
        Search currentSearch = getSearch(fullQuery, 1L, PAGE_SIZE);
        result.addAll(mapPatents(currentSearch));
        while (CollectionUtils.isNotEmpty(currentSearch.getQueries().getNextPage())) {
            final Search.Queries.NextPage nextPage = currentSearch.getQueries().getNextPage().get(0);
            if (nextPage.getStartIndex() >= MAX_SEARCH_RESULTS) {
                break;
            }
            currentSearch = getSearch(fullQuery, nextPage.getStartIndex(), PAGE_SIZE);
            result.addAll(mapPatents(currentSearch));
        }
        return result;
    }

    @SneakyThrows
    private Search getSearch(final String query,
                             final long startIndex,
                             final int pageSize) {
        final CustomSearchAPI.Cse.List list = api.cse().list();
        list.setNum(pageSize);
        list.setStart(startIndex);
        list.setCx(searchEngine);
        list.setQ(query);
        list.setSiteSearch(searchHost);
        list.setSiteSearchFilter("i");
        list.setKey(apiKey);
        return list.execute();
    }

    private Map<String, String> getMetaInfo(final Map<String, Object> pageName,
                                            final String name) {
        final Object metatags = pageName.get(name);
        if (metatags instanceof List) {
            final List<ArrayMap<String, String>> list = (List<ArrayMap<String, String>>) metatags;
            if (CollectionUtils.isEmpty(list)) {
                return Collections.emptyMap();
            }
            return list.get(0);
        } else {
            return Collections.emptyMap();
        }
    }

    private String buildTargetQuery(final String input) {
        final String lowerCase = input.toLowerCase();
        final List<String> terms = new ArrayList<>();
        terms.add(input);
        ListUtils.emptyIfNull(keywords).forEach(keyword -> {
            if (!StringUtils.containsIgnoreCase(lowerCase, keyword)) {
                terms.add(keyword);
            }
        });
        return String.format("(%s)", String.join(" ", terms));
    }

    @NotNull
    private List<GooglePatent> mapPatents(Search result) {
        return ListUtils.emptyIfNull(result.getItems())
                .stream()
                .map(item -> {
                    final Map<String, Object> pageName = MapUtils.emptyIfNull(item.getPagemap());
                    final Map<String, String> metatags = getMetaInfo(pageName, "metatags");
                    final Map<String, String> articleData = getMetaInfo(pageName, "scholarlyarticle");
                    return GooglePatent.builder()
                            .title(item.getTitle())
                            .link(item.getLink())
                            .snippet(item.getSnippet())
                            .date(metatags.get("dc.date"))
                            .description(metatags.get("dc.description"))
                            .fullTitle(metatags.get("dc.title"))
                            .contributor(metatags.get("dc.contributor"))
                            .patentNumber(metatags.get("citation_patent_publication_number"))
                            .pdfLink(metatags.get("citation_pdf_url"))
                            .articleData(GooglePatent.ArticleData.builder()
                                    .countryCode(articleData.get("countrycode"))
                                    .countryName(articleData.get("countryname"))
                                    .priorityDate(articleData.get("prioritydate"))
                                    .publicationDate(articleData.get("publicationdate"))
                                    .publicationNumber(articleData.get("publicationnumber"))
                                    .filingDate(articleData.get("filingdate"))
                                    .priorartDate(articleData.get("priorartdate"))
                                    .inventor(articleData.get("inventor"))
                                    .assignee(articleData.get("assigneeoriginal"))
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());
    }

}
