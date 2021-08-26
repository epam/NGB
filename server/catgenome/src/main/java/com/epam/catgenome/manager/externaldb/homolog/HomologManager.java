/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.homolog;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.dao.homolog.HomologDatabaseDao;
import com.epam.catgenome.dao.homolog.HomologGroupDao;
import com.epam.catgenome.dao.homolog.HomologGroupGeneDao;
import com.epam.catgenome.entity.externaldb.homolog.HomologDatabase;
import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.epam.catgenome.entity.externaldb.homolog.HomologGroupGene;
import com.epam.catgenome.entity.externaldb.homolog.HomologType;
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.QueryParameters;
import com.epam.catgenome.util.db.SortInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
@RequiredArgsConstructor
public class HomologManager {

    private static final String FIELDS_LINE_DELIMITER = "\t";

    @Value("${homolog.groups.batch.size:1000}")
    private int batchSize;

    @Autowired
    private HomologGroupDao homologGroupDao;
    @Autowired
    private HomologGroupGeneDao homologGroupGeneDao;
    @Autowired
    private HomologDatabaseDao homologDatabaseDao;

    public SearchResult<HomologGroup> searchHomolog(final HomologSearchRequest request)
            throws IOException {
        final SearchResult<HomologGroup> searchResult = new SearchResult<>();
        final QueryParameters groupQueryParams = buildSearchQuery(request);
        final List<HomologGroup> homologGroups = homologGroupDao.load(groupQueryParams);

        if (homologGroups.size() > 0) {
            final List<String> groupIds = homologGroups.stream()
                    .map(h -> String.valueOf(h.getGroupId()))
                    .collect(Collectors.toList());
            final Filter geneQueryFilter = Filter.builder()
                    .field("group_id")
                    .operator("in")
                    .value("(" + join(groupIds, ",") + ")")
                    .build();
            final SortInfo geneIdSortInfo = SortInfo.builder()
                    .field("gene_id")
                    .ascending(true)
                    .build();
            final SortInfo domainIdSortInfo = SortInfo.builder()
                    .field("domain_id")
                    .ascending(true)
                    .build();
            final QueryParameters geneQueryParams = QueryParameters.builder()
                    .filters(Collections.singletonList(geneQueryFilter))
                    .sortInfos(Arrays.asList(geneIdSortInfo, domainIdSortInfo))
                    .build();
            final List<Gene> genes = homologGroupGeneDao.load(geneQueryParams);
            for (HomologGroup group: homologGroups) {
                List<Gene> groupGenes = genes.stream()
                        .filter(gn -> gn.getGroupId().equals(group.getGroupId()))
                        .collect(Collectors.toList());
                group.setHomologs(groupGenes);
            }
        }

        final int totalCount = homologGroupDao.getTotalCount(groupQueryParams.getFilters());
        searchResult.setItems(homologGroups);
        searchResult.setTotalCount(totalCount);
        return searchResult;
    }

    public void importHomologData(final String databaseName, final String databasePath)
            throws IOException {
        Assert.isTrue(!TextUtils.isBlank(databaseName), "Database name is required");
        File file = new File(databasePath);
        Assert.isTrue(file.isFile() && file.canRead(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        deleteData(databaseName);
        HomologDatabase database = HomologDatabase.builder()
                .name(databaseName.trim().toUpperCase())
                .path(databasePath)
                .build();
        database = homologDatabaseDao.save(database);
        final long databaseId = database.getDatabaseId();
        readAndSaveData(databasePath, databaseId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteData(final String databaseName) {
        final Filter filter = Filter.builder()
                .field("name")
                .operator("=")
                .value("'" + databaseName.trim().toUpperCase() + "'")
                .build();
        List<HomologDatabase> databases = homologDatabaseDao.load(QueryParameters.builder()
                .filters(Collections.singletonList(filter))
                .build());
        if (!CollectionUtils.isEmpty(databases)) {
            long databaseId = databases.get(0).getDatabaseId();
            homologGroupGeneDao.delete(databaseId);
            homologGroupDao.delete(databaseId);
            homologDatabaseDao.delete(databaseId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveData(final List<HomologGroup> groups, final List<HomologGroupGene> genes) {
        homologGroupDao.save(groups);
        homologGroupGeneDao.save(genes);
    }

    public void readAndSaveData(final String databasePath, final long databaseId) throws IOException {
        try (Reader reader = new FileReader(databasePath); BufferedReader bufferedReader = new BufferedReader(reader)) {
            bufferedReader.readLine();
            String line;
            long taxId = 0;
            long geneId = 0;
            long lineTaxId;
            long lineGeneId;
            long groupId = 0;
            HomologGroup group;
            HomologGroupGene gene;
            List<HomologGroupGene> genes = new ArrayList<>();
            List<HomologGroup> groups = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                String[] cells = line.split(FIELDS_LINE_DELIMITER);
                Assert.isTrue(cells.length == 5, "Incorrect file format");
                lineTaxId = Long.parseLong(cells[0].trim());
                lineGeneId = Long.parseLong(cells[1].trim());
                if (lineTaxId != taxId || lineGeneId != geneId) {
                    if (groups.size() == batchSize) {
                        saveData(groups, genes);
                        groups = new ArrayList<>();
                        genes = new ArrayList<>();
                    }
                    groupId = homologGroupDao.nextVal();
                    group = HomologGroup.builder()
                            .groupId(groupId)
                            .databaseId(databaseId)
                            .taxId(lineTaxId)
                            .geneId(lineGeneId)
                            .type(HomologType.getByName(cells[2].trim()))
                            .build();
                    groups.add(group);
                    taxId = lineTaxId;
                    geneId = lineGeneId;
                }
                gene = HomologGroupGene.builder()
                        .databaseId(databaseId)
                        .groupId(groupId)
                        .taxId(Long.parseLong(cells[3].trim()))
                        .geneId(Long.parseLong(cells[4].trim()))
                        .build();
                genes.add(gene);
            }
            if (groups.size() > 0) {
                saveData(groups, genes);
            }
        }
    }

    private QueryParameters buildSearchQuery(final HomologSearchRequest request) {
        Assert.isTrue(request.getGeneId() != null, "Gene id is required");
        final int pageNum = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
        final int pageSize = (request.getPageSize() == null || request.getPageSize() <= 0) ? DEFAULT_PAGE_SIZE
                : request.getPageSize();
        final PagingInfo pagingInfo = new PagingInfo(pageSize, pageNum);
        final Filter filter = Filter.builder()
                .field("primary_gene_id")
                .operator("=")
                .value(String.valueOf(request.getGeneId()))
                .build();
        return QueryParameters.builder()
                .pagingInfo(pagingInfo)
                .filters(Collections.singletonList(filter))
                .build();
    }
}
