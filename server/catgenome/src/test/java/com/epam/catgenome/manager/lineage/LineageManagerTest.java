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
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.manager.project.ProjectManager;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class LineageManagerTest extends TestCase {

    private static final int FULL_TREE_NODES_NUM = 10;
    private static final int FULL_TREE_EDGES_NUM = 10;
    private static final int REFERENCE_ID = 5;
    private static final int ATTRIBUTES_MAP_SIZE = 2;
    private static final String NODES = "name\tdescription\treferenceId\tdataset\tcreation_date\tattributes\n" +
            "strain-01\t.\t1\t%s\t2020-11-10\t.\n";
    private static final String EDGES = "from\tto\tattributes\ttype_of_interaction\n";
    private Project project;

    @Autowired
    private LineageTreeManager lineageTreeManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private ApplicationContext context;

    private String nodesFileName;
    private String edgesFileName;

    @Before
    public void setUp() throws IOException {
        this.nodesFileName = context.getResource("classpath:lineage//nodes.txt").getFile().getPath();
        this.edgesFileName = context.getResource("classpath:lineage//edges.txt").getFile().getPath();
        project = createProject();
    }

    @After
    public void teardown() throws IOException {
        projectManager.delete(project.getId(), true);
    }

    @Test
    public void createLineageTreeTest() throws IOException {
        final LineageTree lineageTree = registerLineageTree("createLineageTreeTest", nodesFileName, edgesFileName);
        assertNotNull(lineageTree);
        assertEquals("createLineageTreeTest", lineageTree.getName());
        assertEquals("lineageTree", lineageTree.getPrettyName());
        assertEquals(BiologicalDataItemResourceType.FILE, lineageTree.getType());
        lineageTreeManager.deleteLineageTree(lineageTree.getLineageTreeId());
    }

    @Test
    public void createLineageTreeDatasetIdTest() throws IOException {
        final File nodesTmpFile = File.createTempFile("testNodes", "txt");
        final File edgesTmpFile = File.createTempFile("testEdges", "txt");
        Files.write(nodesTmpFile.toPath(), String.format(NODES, project.getId()).getBytes(StandardCharsets.UTF_8));
        Files.write(edgesTmpFile.toPath(), EDGES.getBytes(StandardCharsets.UTF_8));
        final LineageTree lineageTree = registerLineageTree("createLineageTreeDatasetIdTest",
                nodesTmpFile.toString(),
                edgesTmpFile.toString());
        nodesTmpFile.delete();
        edgesTmpFile.delete();
        assertNotNull(lineageTree);
        assertEquals(project.getId(), lineageTree.getNodes().get(0).getProjectId());
        lineageTreeManager.deleteLineageTree(lineageTree.getLineageTreeId());
    }

    @Test
    public void loadLineageTree() throws IOException {
        final LineageTree lineageTree = registerLineageTree("loadLineageTree", nodesFileName, edgesFileName);
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
        assertEquals(project.getId(), lineageTree.getNodes().get(0).getProjectId());
        assertNull(lineageTree.getNodes().get(1).getProjectId());
        assertNull(lineageTree.getNodes().get(2).getProjectId());
        assertEquals(ATTRIBUTES_MAP_SIZE, lineageTree.getNodes().get(0).getAttributes().size());
        assertEquals(ATTRIBUTES_MAP_SIZE, lineageTree.getEdges().get(0).getAttributes().size());
        assertEquals("value1", lineageTree.getNodes().get(0).getAttributes().get("key1"));
        assertEquals("value1", lineageTree.getEdges().get(0).getAttributes().get("key1"));
        lineageTreeManager.deleteLineageTree(lineageTree.getLineageTreeId());
    }

    @Test
    public void loadOneTreeByReferenceId() throws IOException {
        final LineageTree lineageTree = registerLineageTree("loadLineageTrees", nodesFileName, edgesFileName);
        final List<LineageTree> lineageTrees = lineageTreeManager.loadLineageTrees(REFERENCE_ID);
        assertFalse(lineageTrees.isEmpty());
        lineageTreeManager.deleteLineageTree(lineageTree.getLineageTreeId());
    }

    @Test
    public void deleteLineageTreeTest() throws IOException {
        final LineageTree lineageTree = registerLineageTree("deleteLineageTreeTest", nodesFileName, edgesFileName);
        LineageTree createdLineageTree = lineageTreeManager.loadLineageTree(lineageTree.getLineageTreeId());
        lineageTreeManager.deleteLineageTree(createdLineageTree.getLineageTreeId());
        createdLineageTree = lineageTreeManager.loadLineageTree(lineageTree.getLineageTreeId());
        assertNull(createdLineageTree);
    }

    @NotNull
    private LineageTree registerLineageTree(final String name,
                                            final String nodesFileName,
                                            final String edgesFileName) throws IOException {
        final LineageTreeRegistrationRequest request = new LineageTreeRegistrationRequest();
        request.setName(name);
        request.setPrettyName("lineageTree");
        request.setPath(nodesFileName);
        request.setNodesPath(nodesFileName);
        request.setEdgesPath(edgesFileName);
        return lineageTreeManager.createLineageTree(request);
    }

    @NotNull
    private Project createProject() {
        final Project project = new Project();
        project.setName("dataset");
        return projectManager.create(project);
    }
}
