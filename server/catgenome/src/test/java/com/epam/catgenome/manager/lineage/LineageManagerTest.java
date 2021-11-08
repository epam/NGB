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
package com.epam.catgenome.manager.lineage;

import com.epam.catgenome.controller.vo.registration.LineageTreeRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.lineage.LineageTree;
import junit.framework.TestCase;
import org.apache.commons.collections4.CollectionUtils;
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
public class LineageManagerTest extends TestCase {

    private static final int FULL_TREE_NODES_NUM = 10;
    private static final int FULL_TREE_EDGES_NUM = 10;
    private static final int REFERENCE_ID = 5;
    private static final int LAST_NODE_REFERENCE_ID = 10;
    private static final int BY_REFERENCE_NODES_NUM = 6;
    private static final int BY_REFERENCE_EDGES_NUM = 6;

    @Autowired
    private LineageTreeManager lineageTreeManager;

    @Autowired
    private ApplicationContext context;

    private String nodesFileName;
    private String edgesFileName;

    @Before
    public void setUp() throws IOException, ParseException {
        this.nodesFileName = context.getResource("classpath:lineage//nodes.txt").getFile().getPath();
        this.edgesFileName = context.getResource("classpath:lineage//edges.txt").getFile().getPath();
    }

    @Test
    public void createLineageTreeTest() throws IOException {
        LineageTree lineageTree = registerLineageTree("createLineageTreeTest");
        assertNotNull(lineageTree);
        assertEquals("createLineageTreeTest", lineageTree.getName());
        assertEquals("lineageTree", lineageTree.getPrettyName());
        assertEquals(BiologicalDataItemResourceType.FILE, lineageTree.getType());
    }

    @Test
    public void loadLineageTree() throws IOException {
        LineageTree lineageTree = registerLineageTree("loadLineageTree");
        assertNotNull(lineageTree);
        assertNotNull(lineageTree.getNodes());
        assertEquals(FULL_TREE_NODES_NUM, lineageTree.getNodes().size());
        assertEquals("strain-01", lineageTree.getNodes().get(0).getName());
        assertEquals(lineageTree.getLineageTreeId(), lineageTree.getNodes().get(0).getLineageTreeId());
        assertNotNull(lineageTree.getEdges());
        assertEquals(FULL_TREE_EDGES_NUM, lineageTree.getEdges().size());
        assertEquals(lineageTree.getNodes().get(0).getLineageTreeNodeId(),
                lineageTree.getEdges().get(0).getNodeFromId());
        assertEquals(lineageTree.getLineageTreeId(), lineageTree.getEdges().get(0).getLineageTreeId());
    }

    @Test
    public void loadOneTreeByReferenceId() throws IOException {
        registerLineageTree("loadLineageTrees");
        List<LineageTree> lineageTrees = lineageTreeManager.loadLineageTrees(REFERENCE_ID);
        assertFalse(lineageTrees.isEmpty());
        assertEquals(BY_REFERENCE_NODES_NUM, lineageTrees.get(0).getNodes().size());
        assertEquals(BY_REFERENCE_EDGES_NUM, lineageTrees.get(0).getEdges().size());
    }

    @Test
    public void loadTwoTreesByReferenceId() throws IOException {
        registerLineageTree("tree1");
        registerLineageTree("tree2");
        List<LineageTree> lineageTrees = lineageTreeManager.loadLineageTrees(REFERENCE_ID);
        assertFalse(lineageTrees.isEmpty());
        assertEquals(2, lineageTrees.size());
        assertEquals("tree1", lineageTrees.get(0).getName());
        assertEquals("tree2", lineageTrees.get(1).getName());
        assertEquals(BY_REFERENCE_NODES_NUM, lineageTrees.get(0).getNodes().size());
        assertEquals(BY_REFERENCE_EDGES_NUM, lineageTrees.get(0).getEdges().size());
    }

    @Test
    public void loadTreeByReferenceIdOneNode() throws IOException {
        registerLineageTree("loadTreeByReferenceIdOneNode");
        List<LineageTree> lineageTrees = lineageTreeManager.loadLineageTrees(LAST_NODE_REFERENCE_ID);
        assertFalse(lineageTrees.isEmpty());
        assertEquals(1, lineageTrees.get(0).getNodes().size());
        assertTrue(CollectionUtils.isEmpty(lineageTrees.get(0).getEdges()));
    }

    @Test
    public void deleteLineageTreeTest() throws IOException {
        LineageTree lineageTree = registerLineageTree("deleteLineageTreeTest");
        LineageTree createdLineageTree = lineageTreeManager.loadLineageTree(lineageTree.getLineageTreeId());
        lineageTreeManager.deleteLineageTree(createdLineageTree.getLineageTreeId());
        createdLineageTree = lineageTreeManager.loadLineageTree(lineageTree.getLineageTreeId());
        assertNull(createdLineageTree);
    }

    @NotNull
    private LineageTree registerLineageTree(final String name) throws IOException {
        LineageTreeRegistrationRequest request = new LineageTreeRegistrationRequest();
        request.setName(name);
        request.setPrettyName("lineageTree");
        request.setPath(nodesFileName);
        request.setNodesPath(nodesFileName);
        request.setEdgesPath(edgesFileName);
        return lineageTreeManager.createLineageTree(request);
    }
}
