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

package com.epam.catgenome.dao.blast;

import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseOrganism;
import com.epam.catgenome.entity.blast.BlastDatabaseSource;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional
public class BlastDatabaseOrganismDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final long TAX_ID = 36775L;
    @Autowired
    private BlastDatabaseOrganismDao databaseOrganismDao;
    @Autowired
    private BlastDatabaseDao  blastDatabaseDao;

    @Test
    public void testCreateOrganism() {
        final BlastDatabase database = createDatabase();
        createOrganisms(database.getId());
        List<BlastDatabaseOrganism> organismList = databaseOrganismDao.loadDatabaseOrganisms(
                Collections.singletonList(TAX_ID), database.getId());
        Assert.assertEquals(1, organismList.size());
    }

    @Test
    public void testDeleteOrganisms() {
        final BlastDatabase database = createDatabase();
        createOrganisms(database.getId());
        List<BlastDatabaseOrganism> organismList = databaseOrganismDao.loadDatabaseOrganisms(
                Collections.singletonList(TAX_ID), database.getId());
        Assert.assertEquals(1, organismList.size());
        databaseOrganismDao.delete(database.getId());
        organismList = databaseOrganismDao.loadDatabaseOrganisms(
                Collections.singletonList(TAX_ID), database.getId());
        Assert.assertTrue(CollectionUtils.isEmpty(organismList));
    }

    private void createOrganisms(final long databaseId) {
        final BlastDatabaseOrganism organism = BlastDatabaseOrganism.builder()
                .databaseId(databaseId)
                .taxId(TAX_ID)
                .build();
        databaseOrganismDao.save(Collections.singletonList(organism));
    }

    @NotNull
    private BlastDatabase createDatabase() {
        final BlastDatabase database = BlastDatabase.builder()
                .name("Human")
                .path("Human")
                .source(BlastDatabaseSource.CUSTOM)
                .type(BlastDatabaseType.NUCLEOTIDE)
                .build();
        blastDatabaseDao.saveDatabase(database);
        return database;
    }
}
