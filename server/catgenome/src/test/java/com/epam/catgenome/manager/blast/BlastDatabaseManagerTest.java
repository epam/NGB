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

import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseSource;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BlastDatabaseManagerTest extends TestCase {

    @Autowired
    private BlastDatabaseManager manager;

    @Test
    public void createDatabaseOneTaxIdTest() throws IOException {
        BlastDatabase database = registerDatabase();
        database = manager.loadById(database.getId());
        assertNotNull(database);
        assertEquals("createDatabaseTest", database.getName());
        assertEquals("path", database.getPath());
        assertEquals(BlastDatabaseType.NUCLEOTIDE, database.getType());
        assertEquals(BlastDatabaseSource.CUSTOM, database.getSource());
    }

    @Test
    public void updateOrganismsTest() throws IOException {
        final BlastDatabase database = registerDatabase();
        assertNotNull(database);
        manager.updateDatabaseOrganisms(database.getId());
    }

    @Test
    public void deleteDatabaseTest() throws IOException {
        final BlastDatabase database = registerDatabase();
        manager.delete(database.getId());
        assertNull(manager.loadById(database.getId()));
    }

    private BlastDatabase registerDatabase() throws IOException {
        final BlastDatabase database = BlastDatabase.builder()
                .name("createDatabaseTest")
                .path("path")
                .type(BlastDatabaseType.NUCLEOTIDE)
                .source(BlastDatabaseSource.CUSTOM)
                .build();
        return manager.save(database);
    }
}
