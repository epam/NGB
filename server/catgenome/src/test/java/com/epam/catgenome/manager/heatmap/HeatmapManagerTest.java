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
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class HeatmapManagerTest extends TestCase {

    private static final int CELL_VALUES_SIZE = 15;
    private static final int CONTENT_SIZE = 6;
    private static final double MAX_CELL_VALUE = 0.001276598;
    private static final double MIN_CELL_VALUE = 0.0;
    private static final String GENE_1_LABEL = "gene1";
    private static final String GENE_1_ANNOTATION = "test1";
    @Autowired
    private HeatmapManager heatmapManager;

    @Autowired
    private ApplicationContext context;

    private String contentFileName;
    private String labelAnnotationFileName;
    private String treeFileName;


    @Before
    public void setUp() throws IOException, ParseException {
        this.contentFileName = context.getResource("classpath:heatmap//heatmap.tsv").getFile().getPath();
        this.labelAnnotationFileName = context.getResource("classpath:heatmap//label_annotation.tsv")
                .getFile().getPath();
        this.treeFileName = context.getResource("classpath:heatmap//tree.txt")
                .getFile().getPath();
    }

    @Test
    public void createHeatmapTest() throws IOException {
        HeatmapRegistrationRequest request = new HeatmapRegistrationRequest();
        request.setName("createHeatmapTest");
        request.setPath(contentFileName);
        request.setCellAnnotationPath(contentFileName);
        request.setLabelAnnotationPath(labelAnnotationFileName);
        request.setRowTreePath(treeFileName);
        request.setColumnTreePath(treeFileName);
        Heatmap heatmap = heatmapManager.createHeatmap(request);
        Heatmap createdHeatmap = heatmapManager.loadHeatmap(heatmap.getHeatmapId());
        assertNotNull(createdHeatmap);
        assertEquals(createdHeatmap.getName(), "createHeatmapTest");
        assertEquals(createdHeatmap.getPrettyName(), "heatmap");
        assertEquals(createdHeatmap.getType(), BiologicalDataItemResourceType.FILE);
        assertEquals(createdHeatmap.getRowLabels().size(), CONTENT_SIZE);
        assertTrue(createdHeatmap.getRowLabels().contains(GENE_1_LABEL));
        assertEquals(createdHeatmap.getColumnLabels().size(), CONTENT_SIZE);
        assertTrue(createdHeatmap.getColumnLabels().contains(GENE_1_LABEL));
        assertEquals(createdHeatmap.getCellValues().size(), CELL_VALUES_SIZE);
        assertEquals(createdHeatmap.getCellValueType(), HeatmapDataType.DOUBLE);
        assertEquals(createdHeatmap.getMinCellValue(), MIN_CELL_VALUE);
        assertEquals(createdHeatmap.getMaxCellValue(), MAX_CELL_VALUE);
        List<List<List<String>>> content = heatmapManager.getContent(heatmap.getHeatmapId());
        assertNotNull(content);
        Map<String, String> labelAnnotation = heatmapManager.getLabelAnnotation(heatmap.getHeatmapId());
        assertNotNull(labelAnnotation);
        HeatmapTree tree = heatmapManager.getTree(heatmap.getHeatmapId());
        assertNotNull(tree);
    }

    @Test
    public void updateLabelAnnotationTest() throws IOException {
        Heatmap heatmap = registerHeatmap("updateLabelAnnotationTest");
        heatmapManager.updateLabelAnnotation(heatmap.getHeatmapId(), labelAnnotationFileName);
        Map<String, String> labelAnnotation = heatmapManager.getLabelAnnotation(heatmap.getHeatmapId());
        assertNotNull(labelAnnotation);
        assertEquals(labelAnnotation.size(), CONTENT_SIZE);
        assertTrue(labelAnnotation.containsKey(GENE_1_LABEL));
        assertEquals(labelAnnotation.get(GENE_1_LABEL), GENE_1_ANNOTATION);
    }

    @Test
    public void updateCellAnnotationTest() throws IOException {
        Heatmap heatmap = registerHeatmap("updateCellAnnotationTest");
        heatmapManager.updateCellAnnotation(heatmap.getHeatmapId(), contentFileName);
        List<List<List<String>>> content = heatmapManager.getContent(heatmap.getHeatmapId());
        assertNotNull(content);
        assertEquals(content.size(), CONTENT_SIZE);
        assertEquals(content.get(0).size(), CONTENT_SIZE);
        assertEquals(content.get(0).get(0).get(1), "0");
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
        return heatmapManager.createHeatmap(request);
    }
}
