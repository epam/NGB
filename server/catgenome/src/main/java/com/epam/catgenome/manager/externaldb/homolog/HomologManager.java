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
import com.epam.catgenome.dao.homolog.HomologGeneAliasDao;
import com.epam.catgenome.dao.homolog.HomologGeneDescDao;
import com.epam.catgenome.dao.homolog.HomologGeneDomainDao;
import com.epam.catgenome.dao.homolog.HomologGroupDao;
import com.epam.catgenome.dao.homolog.HomologGroupGeneDao;
import com.epam.catgenome.entity.externaldb.homolog.HomologDatabase;
import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.epam.catgenome.entity.externaldb.homolog.HomologGroupGene;
import com.epam.catgenome.entity.externaldb.homolog.HomologType;
import com.epam.catgenome.entity.externaldb.homologene.Alias;
import com.epam.catgenome.entity.externaldb.homologene.Domain;
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
@RequiredArgsConstructor
public class HomologManager {

    private static final String FIELDS_LINE_DELIMITER = "\t";

    @Value("${homologene.index.directory}")
    private String indexDirectory;

    @Autowired
    HomologGroupDao homologGroupDao;

    @Autowired
    HomologGroupGeneDao homologGroupGeneDao;

    @Autowired
    HomologDatabaseDao homologDatabaseDao;

    @Autowired
    HomologGeneDescDao homologGeneDescDao;

    @Autowired
    HomologGeneAliasDao homologGeneAliasDao;

    @Autowired
    HomologGeneDomainDao homologGeneDomainDao;

    public SearchResult<HomologGroup> searchHomolog(final HomologSearchRequest request)
            throws IOException {
        final SearchResult<HomologGroup> searchResult = new SearchResult<>();
        //group
        QueryParameters groupQueryParams = buildSearchQuery(request);
        final List<HomologGroup> homologGroups = homologGroupDao.load(groupQueryParams);
        //database
        final List<Long> groupDatabaseIds = homologGroups.stream()
                .map(HomologGroup::getDatabaseId)
                .collect(Collectors.toList());
        QueryParameters databaseQueryParams = QueryParameters.builder()
                .filters(Collections.singletonList(
                        new Filter("id", "in", "(" + join(groupDatabaseIds, ",") + ")")))
                .build();
        final List<HomologDatabase> groupDatabases = homologDatabaseDao.load(databaseQueryParams);
        //genes
        final List<Long> groupIds = homologGroups.stream().map(HomologGroup::getId).collect(Collectors.toList());
        final List<Long> groupPrimaryGeneIds = homologGroups.stream()
                .map(HomologGroup::getGeneId)
                .collect(Collectors.toList());
        QueryParameters geneQueryParams = QueryParameters.builder()
                .filters(Collections.singletonList(
                        new Filter("group_id", "in", "(" + join(groupIds, ",") + ")")))
                .build();
        final List<HomologGroupGene> groupGenes = homologGroupGeneDao.load(geneQueryParams);
        //gene descriptions
        final List<Long> groupGeneIds = groupGenes.stream()
                .map(HomologGroupGene::getGeneId)
                .collect(Collectors.toList());
        groupGeneIds.addAll(groupPrimaryGeneIds);
        QueryParameters geneDescQueryParams = QueryParameters.builder()
                .filters(Collections.singletonList(
                        new Filter("id", "in", "(" + join(groupGeneIds, ",") + ")")))
                .build();
        final List<Gene> geneDescs = homologGeneDescDao.load(geneDescQueryParams);
        //aliases
        QueryParameters aliasesQueryParams = QueryParameters.builder()
                .filters(Collections.singletonList(
                        new Filter("gene_id", "in", "(" + join(groupGeneIds, ",") + ")")))
                .build();
        final List<Alias> aliases = homologGeneAliasDao.load(aliasesQueryParams);
        //domains
        final List<Domain> domains = homologGeneDomainDao.load(aliasesQueryParams);

        setHomologGroupsData(homologGroups, groupDatabases, groupGenes, geneDescs, aliases, domains);
        final int totalCount = homologGroupDao.getTotalCount(groupQueryParams.getFilters());
        searchResult.setItems(homologGroups);
        searchResult.setTotalCount(totalCount);
        return searchResult;
    }

    private void setHomologGroupsData(final List<HomologGroup> homologGroups,
                                      final List<HomologDatabase> groupDatabases,
                                      final List<HomologGroupGene> groupGenes,
                                      final List<Gene> geneDescs,
                                      final List<Alias> aliases,
                                      final List<Domain> domains) {
        for (HomologGroup group: homologGroups) {
            //database
            groupDatabases
                    .stream()
                    .filter(g -> g.getId().equals(group.getDatabaseId()))
                    .findFirst()
                    .ifPresent(database -> group.setHomologDatabase(database.getName()));
            //protein Name and gene Name
            Gene gene = geneDescs
                    .stream()
                    .filter(g -> g.getGeneId().equals(group.getGeneId()))
                    .findFirst()
                    .orElse(null);
            if (gene != null) {
                group.setProteinName(gene.getTitle());
                group.setGeneName(gene.getSymbol());
            }
            //genes
            List<Gene> geneDescList = new ArrayList<>();
            for (HomologGroupGene groupGene: groupGenes) {
                Gene geneDesc = geneDescs
                        .stream()
                        .filter(g -> g.getGeneId().equals(groupGene.getGeneId()))
                        .findFirst()
                        .orElse(null);
                if (geneDesc != null) {
                    List<Alias> geneAliases = aliases
                            .stream()
                            .filter(a -> a.getGeneId().equals(geneDesc.getGeneId()))
                            .collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(geneAliases)) {
                        List<String> aliasesList = geneAliases
                                .stream()
                                .map(Alias::getName)
                                .collect(Collectors.toList());
                        geneDesc.setAliases(aliasesList);
                    }
                    List<Domain> geneDomains = domains
                            .stream()
                            .filter(d -> d.getGeneId().equals(geneDesc.getGeneId()))
                            .collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(geneDomains)) {
                        geneDesc.setDomains(geneDomains);
                    }
                    geneDescList.add(geneDesc);
                } else {
                    geneDescList.add(Gene.builder()
                            .geneId(groupGene.getGeneId())
                            .taxId(groupGene.getTaxId())
                            .build());
                }
            }
            group.setHomologs(geneDescList);
        }
    }

    private QueryParameters buildSearchQuery(final HomologSearchRequest request) {
        Assert.isTrue(!TextUtils.isBlank(request.getGeneId()), "Gene id is required");
        final int pageNum = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
        final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                : request.getPageSize();
        PagingInfo pagingInfo = new PagingInfo(pageSize, pageNum);
        QueryParameters queryParameters = QueryParameters.builder()
                .pagingInfo(pagingInfo)
                .build();
        queryParameters.setFilters(Collections
                .singletonList(new Filter("primary_gene_id", "=", request.getGeneId())));
        return queryParameters;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void importHomologData(final String databaseName, final String databasePath)
            throws IOException {
        Assert.isTrue(!TextUtils.isBlank(databaseName), "Database name is required");
        File file = new File(databasePath);
        Assert.isTrue(file.isFile() && file.canRead(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        deleteData(databaseName);
        Map<Object, List<HomologRecord>> records = readLines(databasePath);
        if (!CollectionUtils.isEmpty(records)) {
            saveData(records, databaseName, databasePath);
        }
    }

    public Map<Object, List<HomologRecord>> readLines(final String path) throws IOException {
        List<HomologRecord> records = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        reader.readLine();
        String line;
        while ((line = reader.readLine()) != null) {
            HomologRecord record = HomologRecord.builder()
                    .taxId(Long.parseLong(line.split(FIELDS_LINE_DELIMITER)[0].trim()))
                    .geneId(Long.parseLong(line.split(FIELDS_LINE_DELIMITER)[1].trim()))
                    .relationship(line.split(FIELDS_LINE_DELIMITER)[2].trim())
                    .otherTaxId(Long.parseLong(line.split(FIELDS_LINE_DELIMITER)[3].trim()))
                    .otherGeneId(Long.parseLong(line.split(FIELDS_LINE_DELIMITER)[4].trim()))
                    .build();
            records.add(record);
        }
        return records
                .stream()
                .collect(groupingBy(r -> join(Arrays.asList(r.getGeneId(), r.getTaxId()), "")));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveData(final Map<Object, List<HomologRecord>> data,
                         final String databaseName,
                         final String databasePath) {
        HomologDatabase database = HomologDatabase.builder()
                .name(databaseName)
                .path(databasePath)
                .build();
        database = homologDatabaseDao.save(database);
        final long databaseId = database.getId();
        List<HomologGroup> groups = new ArrayList<>();
        List<HomologGroupGene> genes = new ArrayList<>();
        setDBData(data, databaseId, groups, genes);
        homologGroupDao.save(groups);
        homologGroupGeneDao.save(genes);
    }

    private void setDBData(final Map<Object, List<HomologRecord>> data,
                           final long databaseId,
                           final List<HomologGroup> groups,
                           final List<HomologGroupGene> genes) {
        List<Long> groupIds = homologGroupDao.nextVal(data.size());
        int entryNum = 0;
        for (Map.Entry<Object, List<HomologRecord>> entry : data.entrySet()) {
            List<HomologRecord> value = entry.getValue();
            HomologRecord record = value.get(0);
            HomologGroup group = HomologGroup.builder()
                    .id(groupIds.get(entryNum))
                    .databaseId(databaseId)
                    .geneId(record.getGeneId())
                    .taxId(record.getTaxId())
                    .type(HomologType.getByName(record.getRelationship()))
                    .build();
            groups.add(group);
            for (HomologRecord r : value) {
                HomologGroupGene groupGene = HomologGroupGene.builder()
                        .groupId(groupIds.get(entryNum))
                        .geneId(r.getOtherGeneId())
                        .taxId(r.getOtherTaxId())
                        .databaseId(databaseId)
                        .build();
                genes.add(groupGene);
            }
            entryNum++;
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    private void deleteData(final String databaseName) {
        List<HomologDatabase> databases = homologDatabaseDao.load(QueryParameters.builder()
                .filters(Collections.singletonList(new Filter("name", "=", "'" + databaseName + "'")))
                .build());
        if (!CollectionUtils.isEmpty(databases)) {
            long databaseId = databases.get(0).getId();
            homologGroupGeneDao.delete(databaseId);
            homologGroupDao.delete(databaseId);
            homologDatabaseDao.delete(databaseId);
        }
    }

    @Builder
    @Setter
    @Getter
    public static class HomologRecord {
        long taxId;
        long geneId;
        String relationship;
        long otherTaxId;
        long otherGeneId;
    }
}
