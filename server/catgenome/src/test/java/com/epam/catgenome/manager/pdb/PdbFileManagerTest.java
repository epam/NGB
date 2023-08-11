/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.manager.pdb;

import com.epam.catgenome.entity.pdb.PdbFile;
import com.epam.catgenome.entity.pdb.PdbFileQueryParams;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.PagingInfo;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public class PdbFileManagerTest extends TestCase {

    @Autowired
    private PdbFileManager manager;
    @Autowired
    private ApplicationContext context;

    @Test
    public void createTest() throws IOException {
        final PdbFile pdbFile = create();
        assertNotNull(pdbFile);
    }

    @Test
    public void updateMetadataTest() throws IOException {
        final PdbFile pdbFile = create();
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        manager.updateMetadata(pdbFile.getPdbFileId(), metadata);
        final PdbFile updated = manager.load(pdbFile.getPdbFileId());
        assertEquals("value1", updated.getMetadata().get("key1"));
    }

    @Test
    public void loadTest() throws IOException {
        create();
        final PdbFileQueryParams queryParams = PdbFileQueryParams.builder()
                .geneIds(Collections.singletonList("ENSG00000133703"))
                .pagingInfo(PagingInfo.builder().pageSize(2).pageNum(1).build())
                .build();
        final Page<PdbFile> pdbFiles = manager.load(queryParams);
        assertEquals(1, pdbFiles.getItems().size());
        final PdbFileQueryParams queryParams1 = PdbFileQueryParams.builder()
                .geneIds(Collections.singletonList("NE"))
                .pagingInfo(PagingInfo.builder().pageSize(2).pageNum(1).build())
                .build();
        final Page<PdbFile> pdbFiles1 = manager.load(queryParams1);
        assertEquals(0, pdbFiles1.getItems().size());
    }

    @Test
    public void deleteTest() throws IOException {
        final PdbFile pdbFile = create();
        final PdbFile createdPdbFile = manager.load(pdbFile.getPdbFileId());
        assertNotNull(createdPdbFile);
        manager.delete(pdbFile.getPdbFileId());
        assertNull(manager.load(pdbFile.getPdbFileId()));
    }

    @Test
    public void loadContentTest() throws IOException {
        final PdbFile pdbFile = create();
        final byte[] content = manager.loadContent(pdbFile.getPdbFileId());
        assertNotNull(content);
    }

    private PdbFile create() throws IOException {
        final String path = context.getResource("classpath:pdb/test.cif").getFile().getPath();
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("key", "value");
        final PdbFile pdbFile = PdbFile.builder()
                .geneId("ENSG00000133703")
                .metadata(metadata)
                .build();
        pdbFile.setPath(path);
        pdbFile.setName("6GOG");
        pdbFile.setPrettyName("KRAS-169 Q61H GPPNHP");
        return manager.create(pdbFile);
    }
}
