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
package com.epam.catgenome.manager.blast;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.blast.BlastDatabaseDao;
import com.epam.catgenome.dao.blast.BlastDatabaseOrganismDao;
import com.epam.catgenome.dao.blast.BlastListSpeciesTaskDao;
import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseOrganism;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.entity.blast.BlastListSpeciesTask;
import com.epam.catgenome.entity.blast.BlastTaskStatus;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.blast.dto.BlastRequestInfo;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomyManager;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.Constants.DATE_TIME_FORMAT;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlastDatabaseManager {

    private final BlastDatabaseDao databaseDao;
    private final BlastDatabaseOrganismDao databaseOrganismDao;
    private final TaxonomyManager taxonomyManager;
    private final BlastRequestManager blastRequestManager;
    private final BlastListSpeciesTaskDao listSpeciesTaskDao;

    @Value("${blast.database.taxonomy.top.hits:100}")
    private int topHits;

    @Transactional(propagation = Propagation.REQUIRED)
    public BlastDatabase save(final BlastDatabase database) {
        Assert.isTrue(!TextUtils.isBlank(database.getName()), "Database name is required");
        Assert.isTrue(!TextUtils.isBlank(database.getPath()), "Database path is required");
        Assert.notNull(database.getType(), "Database type is required");
        Assert.notNull(database.getSource(), "Database source is required");
        databaseDao.saveDatabase(database);
        try {
            updateDatabaseOrganisms(database.getId());
        } catch (IOException e) {
            log.debug(e.getMessage());
        }
        return database;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveDatabaseOrganisms(final long taskId, final long databaseId) throws IOException {
        try {
            final List<BlastDatabaseOrganism> organisms = blastRequestManager.listSpecies(taskId).stream()
                    .map(o -> BlastDatabaseOrganism.builder().taxId(o).build())
                    .collect(Collectors.toList());
            organisms.forEach(o -> o.setDatabaseId(databaseId));
            if (!organisms.isEmpty()) {
                databaseOrganismDao.delete(databaseId);
                databaseOrganismDao.save(organisms);
            }
        } catch (BlastRequestException e) {
            log.debug(e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateDatabaseOrganisms(final long databaseId) throws IOException {
        final BlastDatabase database = checkDatabase(databaseId);
        final BlastRequestInfo requestInfo = blastRequestManager.createListSpeciesTask(database.getName());
        final BlastListSpeciesTask task = BlastListSpeciesTask.builder()
                .taskId(requestInfo.getRequestId())
                .databaseId(databaseId)
                .createdDate(LocalDateTime.parse(requestInfo.getCreatedDate(),
                        DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                .status(BlastTaskStatus.valueOf(requestInfo.getStatus()))
                .statusReason(requestInfo.getReason())
                .build();
        listSpeciesTaskDao.saveTask(task);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final long databaseId) {
        checkDatabase(databaseId);
        databaseOrganismDao.delete(databaseId);
        databaseDao.deleteDatabase(databaseId);
    }

    public BlastDatabase loadById(final long id) {
        return databaseDao.loadDatabase(id);
    }

    public List<BlastDatabase> load(final BlastDatabaseType type, final String path) {
        final List<Filter> filters = new ArrayList<>();
        if (type != null) {
            filters.add(new Filter("type", "=", "'" + type.getTypeId().toString() + "'"));
        }
        if (path != null) {
            filters.add(new Filter("path", "=", "'" + path + "'"));
        }
        final QueryParameters queryParameters = QueryParameters.builder()
                .filters(filters)
                .build();
        return databaseDao.loadDatabases(queryParameters);
    }

    public List<Taxonomy> searchOrganisms(final String term, final long databaseId) throws IOException, ParseException {
        final List<Taxonomy> taxonomies = taxonomyManager.searchOrganisms(term, topHits);
        if (CollectionUtils.isEmpty(taxonomies)) {
            return null;
        }
        final List<Long> taxIds = taxonomies.stream().map(Taxonomy::getTaxId).collect(Collectors.toList());
        final List<BlastDatabaseOrganism> organisms = databaseOrganismDao.loadDatabaseOrganisms(taxIds, databaseId);
        if (CollectionUtils.isEmpty(organisms)) {
            return null;
        }
        final List<Long> databaseTaxIds = organisms.stream()
                .map(BlastDatabaseOrganism::getTaxId)
                .collect(Collectors.toList());
        return taxonomies.stream().filter(t -> databaseTaxIds.contains(t.getTaxId())).collect(Collectors.toList());
    }

    private BlastDatabase checkDatabase(final long id) {
        final BlastDatabase database = databaseDao.loadDatabase(id);
        Assert.notNull(database, getMessage(MessagesConstants.ERROR_DATABASE_NOT_FOUND, id));
        return database;
    }
}
