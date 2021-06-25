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

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.blast.BlastDatabaseDao;
import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlastDatabaseManager {

    private final BlastDatabaseDao databaseDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public void save(final BlastDatabase database) {
        databaseDao.saveDatabase(database);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final long id) {
        BlastDatabase dataBase = databaseDao.loadDatabase(id);
        Assert.notNull(dataBase, MessageHelper.getMessage(MessagesConstants.ERROR_DATABASE_NOT_FOUND, id));
        databaseDao.deleteDatabase(id);
    }

    public BlastDatabase loadById(final long id) {
        BlastDatabase database = databaseDao.loadDatabase(id);
        Assert.notNull(database, MessageHelper.getMessage(MessagesConstants.ERROR_DATABASE_NOT_FOUND, id));
        return database;
    }

    public List<BlastDatabase> load(final BlastDatabaseType type, String path) {
        List<Filter> filters = new ArrayList<>();
        if (type != null) {
            filters.add(new Filter("type", "=", "'" + type.getTypeId().toString() + "'"));
        }
        if (path != null) {
            filters.add(new Filter("path", "=", "'" + path + "'"));
        }
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setFilters(filters);
        return databaseDao.loadDatabases(queryParameters);
    }
}
