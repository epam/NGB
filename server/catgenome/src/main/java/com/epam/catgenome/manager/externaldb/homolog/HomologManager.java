/*
 * MIT License
 *
 * Copyright (c) 2021-2023 EPAM Systems
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

import com.epam.catgenome.dao.homolog.HomologDatabaseDao;
import com.epam.catgenome.dao.homolog.HomologGroupDao;
import com.epam.catgenome.dao.homolog.HomologGroupGeneDao;
import com.epam.catgenome.entity.externaldb.homolog.HomologDatabase;
import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.epam.catgenome.entity.externaldb.homolog.HomologGroupGene;
import com.epam.catgenome.entity.externaldb.homolog.HomologType;
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIEnsemblIdsManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomyManager;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneManager;
import com.epam.catgenome.util.FileFormat;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.externaldb.homologene.HomologeneManager.setEnsemblIds;
import static com.epam.catgenome.manager.externaldb.homologene.HomologeneManager.setGeneSpeciesNames;
import static com.epam.catgenome.util.NgbFileUtils.getFile;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
@RequiredArgsConstructor
public class HomologManager {

    @Value("${homolog.groups.batch.size:500}")
    private int batchSize;

    @Autowired
    private TaxonomyManager taxonomyManager;
    @Autowired
    private HomologGroupDao homologGroupDao;
    @Autowired
    private HomologGroupGeneDao homologGroupGeneDao;
    @Autowired
    private HomologDatabaseDao homologDatabaseDao;
    @Autowired
    private NCBIGeneManager ncbiGeneManager;
    @Autowired
    private NCBIEnsemblIdsManager ncbiEnsemblIdsManager;

    @Autowired
    private NCBIGeneIdsManager geneIdsManager;

    public SearchResult<HomologGroup> searchHomolog(final HomologSearchRequest request)
            throws IOException, ParseException {
        Assert.isTrue(request.getGeneId() != null, "Gene id is required");

        final SearchResult<HomologGroup> searchResult = new SearchResult<>();

        try {
            final String geneId = ncbiGeneManager.fetchExternalId(request.getGeneId());
            final QueryParameters groupQueryParams = buildSearchQuery(request);
            final List<Filter> geneIdFilters = Collections.singletonList(Filter.builder()
                    .field("gene_id")
                    .operator("=")
                    .value(geneId)
                    .build());
            groupQueryParams.setFilters(geneIdFilters);
            final List<String> groupIds = homologGroupGeneDao.loadGroupIds(groupQueryParams);
            if (!CollectionUtils.isEmpty(groupIds)) {
                final Filter groupIdsFilter = Filter.builder()
                        .field("group_id")
                        .operator("in")
                        .value("(" + join(groupIds, ",") + ")")
                        .build();
                final QueryParameters queryParams = QueryParameters.builder()
                        .filters(Collections.singletonList(groupIdsFilter))
                        .build();
                final List<HomologGroup> homologGroups = homologGroupDao.load(queryParams);
                final List<Gene> genes = homologGroupGeneDao.load(queryParams);

                for (HomologGroup group: homologGroups) {
                    List<Gene> groupGenes = genes.stream()
                            .filter(gn -> gn.getGroupId().equals(group.getGroupId()))
                            .collect(Collectors.toList());
                    group.setHomologs(groupGenes);
                }
                setSpeciesNames(homologGroups);
                setGeneIds(homologGroups);
                searchResult.setItems(homologGroups);
                final List<Long> allGroupIds = homologGroupGeneDao.loadAllGroupIds(geneIdFilters);
                searchResult.setTotalCount(allGroupIds.size());
            }
        } catch (ExternalDbUnavailableException e) {
            log.error(e.getMessage());
        }
        return searchResult;
    }

    public Map<String, List<HomologGroup>> searchHomolog(final List<String> geneIds)
            throws IOException, ParseException {
        Assert.isTrue(!CollectionUtils.isEmpty(geneIds), "Gene ids are required");
        final Map<String, List<HomologGroup>> result = new HashMap<>();
        List<HomologGroup> homologGroups;
        final List<GeneId> ncbiGeneIds = geneIdsManager.getNcbiGeneIds(geneIds);
        for (GeneId geneId: ncbiGeneIds) {
            List<String> groupIds = homologGroupGeneDao.loadGroupsByGeneIds(
                    Collections.singletonList(geneId.getEntrezId()));
            if (!CollectionUtils.isEmpty(groupIds)) {
                final Filter groupIdsFilter = Filter.builder()
                        .field("group_id")
                        .operator("in")
                        .value("(" + join(groupIds, ",") + ")")
                        .build();
                final QueryParameters queryParams = QueryParameters.builder()
                        .filters(Collections.singletonList(groupIdsFilter))
                        .build();
                homologGroups = homologGroupDao.load(queryParams);
                List<Gene> genes = homologGroupGeneDao.load(queryParams);
                for (HomologGroup group: homologGroups) {
                    List<Gene> groupGenes = genes.stream()
                            .filter(gn -> gn.getGroupId().equals(group.getGroupId()))
                            .collect(Collectors.toList());
                    group.setHomologs(groupGenes);
                }
                result.put(geneId.getEnsemblId(), homologGroups);
            }
        }
        setSpeciesNames(result);
        setGeneIds(result);
        return result;
    }

    public void importHomologData(final String databaseName, final String databasePath)
            throws IOException {
        Assert.isTrue(!TextUtils.isBlank(databaseName), "Database name is required");
        getFile(databasePath);
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
                String[] cells = line.split(FileFormat.TSV.getSeparator());
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
                    gene = HomologGroupGene.builder()
                            .databaseId(databaseId)
                            .groupId(groupId)
                            .taxId(lineTaxId)
                            .geneId(lineGeneId)
                            .build();
                    genes.add(gene);
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
        final int pageNum = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
        final int pageSize = (request.getPageSize() == null || request.getPageSize() <= 0) ? DEFAULT_PAGE_SIZE
                : request.getPageSize();
        final PagingInfo pagingInfo = new PagingInfo(pageSize, pageNum);
        return QueryParameters.builder()
                .pagingInfo(pagingInfo)
                .build();
    }

    private void setSpeciesNames(final List<HomologGroup> homologGroups) {
        final Set<Long> taxIds = new HashSet<>();
        homologGroups.forEach(group -> {
            taxIds.add(group.getTaxId());
            taxIds.addAll(group.getHomologs().stream().map(Gene::getTaxId).collect(Collectors.toList()));
        });
        final List<Taxonomy> organisms = taxIds.isEmpty() ? Collections.emptyList()
                : taxonomyManager.searchOrganismsByIds(new HashSet<>(taxIds));
        for (HomologGroup group: homologGroups) {
            Taxonomy organism = organisms
                    .stream()
                    .filter(o -> o.getTaxId().equals(group.getTaxId()))
                    .findFirst()
                    .orElse(null);
            if (organism != null) {
                group.setSpeciesCommonName(organism.getCommonName());
                group.setSpeciesScientificName(organism.getScientificName());
            }
            setGeneSpeciesNames(group.getHomologs(), organisms);
        }
    }

    private void setSpeciesNames(final Map<String, List<HomologGroup>> homologGroups) {
        final Set<Long> taxIds = new HashSet<>();
        homologGroups.forEach((key, value) -> value.forEach(group -> {
            taxIds.add(group.getTaxId());
            taxIds.addAll(group.getHomologs().stream().map(Gene::getTaxId).collect(Collectors.toList()));
        }));
        final List<Taxonomy> organisms = taxIds.isEmpty() ? Collections.emptyList()
                : taxonomyManager.searchOrganismsByIds(new HashSet<>(taxIds));
        homologGroups.forEach((key, value) -> value.forEach(group -> {
            Taxonomy organism = organisms
                    .stream()
                    .filter(o -> o.getTaxId().equals(group.getTaxId()))
                    .findFirst()
                    .orElse(null);
            if (organism != null) {
                group.setSpeciesCommonName(organism.getCommonName());
                group.setSpeciesScientificName(organism.getScientificName());
            }
            setGeneSpeciesNames(group.getHomologs(), organisms);
        }));
    }

    private void setGeneIds(final List<HomologGroup> homologGroups) throws ParseException, IOException {
        final Set<String> allGeneIds = new HashSet<>();
        homologGroups.forEach(group -> {
            allGeneIds.add(group.getGeneId().toString());
            allGeneIds.addAll(group.getHomologs().stream()
                    .map(g -> g.getGeneId().toString())
                    .collect(Collectors.toList()));
        });
        final List<GeneId> geneIds = ncbiEnsemblIdsManager.searchByEntrezIds(new ArrayList<>(allGeneIds));
        final Map<Long, GeneId> genesMap = geneIds.stream()
                .collect(Collectors.toMap(GeneId::getEntrezId, Function.identity()));
        for (HomologGroup group: homologGroups) {
            Long groupGeneId = group.getGeneId();
            String ensembleGeneId = genesMap.get(groupGeneId).getEnsemblId();
            group.setEnsemblId(ensembleGeneId);
            setEnsemblIds(group.getHomologs(), geneIds);
        }
    }

    private void setGeneIds(final Map<String, List<HomologGroup>> homologGroups) throws ParseException, IOException {
        final Set<String> allGeneIds = new HashSet<>();
        homologGroups.forEach((key, value) -> value.forEach(group -> {
            allGeneIds.add(group.getGeneId().toString());
            allGeneIds.addAll(group.getHomologs().stream()
                    .map(g -> g.getGeneId().toString()).collect(Collectors.toList()));
        }));
        final List<GeneId> geneIds = ncbiEnsemblIdsManager.searchByEntrezIds(new ArrayList<>(allGeneIds));
        final Map<Long, GeneId> genesMap = geneIds.stream()
                .collect(Collectors.toMap(GeneId::getEntrezId, Function.identity()));
        homologGroups.forEach((key, value) -> value.forEach(group -> {
            Long groupGeneId = group.getGeneId();
            String ensembleGeneId = genesMap.get(groupGeneId).getEnsemblId();
            group.setEnsemblId(ensembleGeneId);
            setEnsemblIds(group.getHomologs(), geneIds);
        }));
    }
}
