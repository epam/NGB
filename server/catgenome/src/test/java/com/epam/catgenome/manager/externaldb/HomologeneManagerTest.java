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
package com.epam.catgenome.manager.externaldb;

import com.epam.catgenome.entity.externaldb.homologene.HomologeneEntry;
import com.epam.catgenome.manager.externaldb.homologene.HomologeneManager;
import com.epam.catgenome.manager.externaldb.homologene.HomologeneSearchRequest;
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomyManager;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class HomologeneManagerTest extends TestCase {

    public static final int ENTRIES_COUNT = 7;

    @Autowired
    private HomologeneManager homologeneManager;

    @Autowired
    private TaxonomyManager taxonomyManager;

    @Autowired
    private ApplicationContext context;

    private String fileName;

    @Before
    public void setUp() throws IOException, ParseException {
        this.fileName = context.getResource("classpath:homologene//homologene.xml").getFile().getPath();
        homologeneManager.importHomologeneDatabase(fileName);
        // searchHomologenes resolves species names through the taxonomy Lucene index, which lives in
        // a directory shared by the whole suite and is not shipped with the repo. Without this the
        // test only passed when TaxonomyManagerTest happened to have run first (or when a previous
        // run left the directory behind) and otherwise died on IndexNotFoundException.
        taxonomyManager.writeLuceneTaxonomyIndex(
                context.getResource("classpath:taxonomy//names.dmp").getFile().getPath());
    }

    @Test
    public void searchTest() throws IOException, ParseException {
        HomologeneSearchRequest query = new HomologeneSearchRequest("ACADML", 1, 5);
        SearchResult<HomologeneEntry> searchResult = homologeneManager.searchHomologenes(query);
        assertNotNull(searchResult);
        assertEquals(1, searchResult.getItems().size());
    }

    @Test
    public void readHomologenesTest() {
        List<HomologeneEntry> entries = homologeneManager.readHomologenes(fileName);
        assertNotNull(entries);
        assertEquals(ENTRIES_COUNT, entries.size());
    }
}
