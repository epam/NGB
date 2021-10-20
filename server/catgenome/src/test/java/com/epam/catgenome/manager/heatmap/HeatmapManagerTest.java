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
package com.epam.catgenome.manager.heatmap;

import com.epam.catgenome.controller.vo.registration.HeatmapRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.heatmap.Heatmap;
import com.epam.catgenome.entity.heatmap.HeatmapDataType;
import com.epam.catgenome.entity.heatmap.HeatmapTree;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;
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
public class HeatmapManagerTest extends TestCase {

    private static final int CELL_VALUES_SIZE = 11;
    private static final int CONTENT_SIZE = 5;
    private static final double MAX_CELL_VALUE = 0.001273579;
    private static final double MIN_CELL_VALUE = 0.0;
    private static final String GENE_1_LABEL = "gene1";
    private static final String GENE_1_ANNOTATION = "test1";

    @Autowired
    private HeatmapManager heatmapManager;

    @Autowired
    private ApplicationContext context;

    private String contentFileName;
    private String labelAnnotationFileName;
    private String cellAnnotationFileName;
    private String treeFileName;


    @Before
    public void setUp() throws IOException, ParseException {
        this.contentFileName = context.getResource("classpath:heatmap//heatmap.tsv").getFile().getPath();
        this.labelAnnotationFileName = context.getResource("classpath:heatmap//label_annotation.tsv")
                .getFile().getPath();
        this.cellAnnotationFileName = context.getResource("classpath:heatmap//cell_annotation.tsv")
                .getFile().getPath();
        this.treeFileName = context.getResource("classpath:heatmap//tree.txt").getFile().getPath();
    }

    @Test
    public void createHeatmapTest() throws IOException {
        HeatmapRegistrationRequest request = new HeatmapRegistrationRequest();
        request.setName("createHeatmapTest");
        request.setPrettyName("heatmap");
        request.setPath(contentFileName);
        request.setCellAnnotationPath(cellAnnotationFileName);
        request.setLabelAnnotationPath(labelAnnotationFileName);
        request.setRowTreePath(treeFileName);
        request.setColumnTreePath(treeFileName);
        request.setSkipColumns(1);
        request.setSkipRows(1);
        Heatmap heatmap = heatmapManager.createHeatmap(request);
        Heatmap createdHeatmap = heatmapManager.loadHeatmap(heatmap.getHeatmapId());
        assertNotNull(createdHeatmap);
        assertEquals("createHeatmapTest", createdHeatmap.getName());
        assertEquals("heatmap", createdHeatmap.getPrettyName());
        assertEquals(BiologicalDataItemResourceType.FILE, createdHeatmap.getType());
        assertEquals(CONTENT_SIZE, createdHeatmap.getRowLabels().size());
        assertTrue(createdHeatmap.getRowLabels().get(0).contains(GENE_1_LABEL));
        assertTrue(createdHeatmap.getRowLabels().get(0).contains(GENE_1_ANNOTATION));
        assertEquals(CONTENT_SIZE, createdHeatmap.getColumnLabels().size());
        assertTrue(createdHeatmap.getColumnLabels().get(0).contains(GENE_1_LABEL));
        assertTrue(createdHeatmap.getColumnLabels().get(0).contains(GENE_1_ANNOTATION));
        assertEquals(CELL_VALUES_SIZE, createdHeatmap.getCellValues().size());
        assertEquals(HeatmapDataType.DOUBLE, createdHeatmap.getCellValueType());
        assertEquals(MIN_CELL_VALUE, createdHeatmap.getMinCellValue());
        assertEquals(MAX_CELL_VALUE, createdHeatmap.getMaxCellValue());
        List<List<List<String>>> content = heatmapManager.getContent(heatmap.getHeatmapId());
        assertNotNull(content);
        HeatmapTree tree = heatmapManager.getTree(heatmap.getHeatmapId());
        assertNotNull(tree);
    }

    @Test
    public void updateLabelAnnotationTest() throws IOException {
        Heatmap heatmap = registerHeatmap("updateLabelAnnotationTest");
        heatmapManager.updateLabelAnnotation(heatmap.getHeatmapId(), labelAnnotationFileName);
        Heatmap updatedHeatmap = heatmapManager.loadHeatmap(heatmap.getHeatmapId());
        assertEquals(labelAnnotationFileName, updatedHeatmap.getLabelAnnotationPath());
        assertNotNull(updatedHeatmap.getRowLabels().get(0));
        assertEquals(2, updatedHeatmap.getRowLabels().get(0).size());
        assertEquals(GENE_1_ANNOTATION, updatedHeatmap.getRowLabels().get(0).get(1));
        assertNotNull(updatedHeatmap.getColumnLabels().get(0));
        assertEquals(2, updatedHeatmap.getColumnLabels().get(0).size());
        assertEquals(GENE_1_ANNOTATION, updatedHeatmap.getColumnLabels().get(0).get(1));
    }

    @Test
    public void updateCellAnnotationTest() throws IOException {
        Heatmap heatmap = registerHeatmap("updateCellAnnotationTest");
        heatmapManager.updateCellAnnotation(heatmap.getHeatmapId(), cellAnnotationFileName);
        List<List<List<String>>> content = heatmapManager.getContent(heatmap.getHeatmapId());
        assertNotNull(content);
        assertEquals(CONTENT_SIZE, content.size());
        assertEquals(CONTENT_SIZE, content.get(0).size());
        assertEquals("a", content.get(0).get(0).get(1));
    }

    @Test
    public void loadHeatmapTest() throws IOException {
        Heatmap heatmap = registerHeatmap("loadHeatmapTest");
        Heatmap createdHeatmap = heatmapManager.loadHeatmap(heatmap.getHeatmapId());
        assertNotNull(createdHeatmap);
    }

    @Test
    public void deleteHeatmapTest() throws IOException {
        Heatmap heatmap = registerHeatmap("deleteHeatmapTest");
        Heatmap createdHeatmap = heatmapManager.loadHeatmap(heatmap.getHeatmapId());
        heatmapManager.deleteHeatmap(createdHeatmap.getHeatmapId());
        createdHeatmap = heatmapManager.loadHeatmap(heatmap.getHeatmapId());
        assertNull(createdHeatmap);
    }

    @Test
    public void updateRowTreeTest() throws IOException {
        Heatmap heatmap = registerHeatmap("updateRowTreeTest");
        heatmapManager.updateRowTree(heatmap.getHeatmapId(), treeFileName);
        HeatmapTree tree = heatmapManager.getTree(heatmap.getHeatmapId());
        assertNotNull(tree);
        assertNotNull(tree.getRow());
    }

    @Test
    public void updateColumnTreeTest() throws IOException {
        Heatmap heatmap = registerHeatmap("updateColumnTreeTest");
        heatmapManager.updateColumnTree(heatmap.getHeatmapId(), treeFileName);
        HeatmapTree tree = heatmapManager.getTree(heatmap.getHeatmapId());
        assertNotNull(tree);
        assertNotNull(tree.getColumn());
    }

    @NotNull
    private Heatmap registerHeatmap(final String name) throws IOException {
        HeatmapRegistrationRequest request = new HeatmapRegistrationRequest();
        request.setName(name);
        request.setPath(contentFileName);
        request.setSkipRows(1);
        request.setSkipColumns(1);
        return heatmapManager.createHeatmap(request);
    }
}
