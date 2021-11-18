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
package com.epam.catgenome.manager.blast;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.blast.BlastDatabaseDao;
import com.epam.catgenome.dao.blast.BlastDatabaseOrganismDao;
import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseOrganism;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomyManager;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlastDatabaseManager {

    private final BlastDatabaseDao databaseDao;
    private final BlastDatabaseOrganismDao databaseOrganismDao;
    private final TaxonomyManager taxonomyManager;
    private final BlastRequestManager blastRequestManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public BlastDatabase save(final BlastDatabase database) throws IOException {
        Assert.isTrue(!TextUtils.isBlank(database.getName()), "Database name is required");
        Assert.isTrue(!TextUtils.isBlank(database.getPath()), "Database path is required");
        Assert.notNull(database.getType(), "Database type is required");
        Assert.notNull(database.getSource(), "Database source is required");
        databaseDao.saveDatabase(database);
        saveDatabaseOrganisms(database);
        return database;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveDatabaseOrganisms(final BlastDatabase database) throws IOException {
        try {
            List<BlastDatabaseOrganism> organisms = blastRequestManager.getTaxIds(database.getName()).stream()
                    .map(o -> BlastDatabaseOrganism.builder().taxId(o).build())
                    .collect(Collectors.toList());
            organisms.forEach(o -> o.setDatabaseId(database.getId()));
            if (!organisms.isEmpty()) {
                databaseOrganismDao.save(organisms);
            }
        } catch (BlastRequestException e) {
            log.debug(e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateDatabaseOrganisms(final long id) throws IOException {
        final BlastDatabase database = checkDatabase(id);
        databaseOrganismDao.delete(id);
        saveDatabaseOrganisms(database);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final long databaseId) {
        checkDatabase(databaseId);
        databaseOrganismDao.delete(databaseId);
        databaseDao.deleteDatabase(databaseId);
    }

    public BlastDatabase loadById(final long id) {
        final BlastDatabase database = databaseDao.loadDatabase(id);
        final List<BlastDatabaseOrganism> organisms = databaseOrganismDao.loadDatabaseOrganisms(id);
        if (!CollectionUtils.isEmpty(organisms)) {
            final Set<Long> taxIds = organisms.stream()
                    .map(BlastDatabaseOrganism::getTaxId)
                    .collect(Collectors.toSet());
            final List<Taxonomy> taxonomies = taxonomyManager.searchOrganismsByIds(taxIds);
            database.setOrganisms(taxonomies);
        }
        return database;
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

    public List<BlastDatabaseOrganism> loadOrganisms(final long databaseId) {
        checkDatabase(databaseId);
        return databaseOrganismDao.loadDatabaseOrganisms(databaseId);
    }

    private BlastDatabase checkDatabase(final long id) {
        final BlastDatabase database = databaseDao.loadDatabase(id);
        Assert.notNull(database, getMessage(MessagesConstants.ERROR_DATABASE_NOT_FOUND, id));
        return database;
    }
}
