/*
 * MIT License
 *
 * Copyright (c) 2021-2022 EPAM Systems
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

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.LineageTreeRegistrationRequest;
import com.epam.catgenome.dao.lineage.LineageTreeDao;
import com.epam.catgenome.dao.lineage.LineageTreeEdgeDao;
import com.epam.catgenome.dao.lineage.LineageTreeNodeDao;
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.lineage.LineageTree;
import com.epam.catgenome.entity.lineage.LineageTreeEdge;
import com.epam.catgenome.entity.lineage.LineageTreeNode;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.util.FileFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.Constants.DATE_FORMAT;
import static com.epam.catgenome.util.NgbFileUtils.getBioDataItemName;
import static com.epam.catgenome.util.NgbFileUtils.getCellValue;
import static com.epam.catgenome.util.NgbFileUtils.getFile;
import static com.epam.catgenome.util.Utils.parseAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class LineageTreeManager {

    private static final int NODES_FILE_COLUMNS = 6;
    private static final int EDGES_FILE_COLUMNS = 4;
    private final LineageTreeDao lineageTreeDao;
    private final LineageTreeNodeDao lineageTreeNodeDao;
    private final LineageTreeEdgeDao lineageTreeEdgeDao;
    private final BiologicalDataItemManager biologicalDataItemManager;
    private final ProjectDao projectDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public LineageTree createLineageTree(final LineageTreeRegistrationRequest request) throws IOException {
        final String nodesPath = request.getNodesPath();
        final String edgesPath = request.getEdgesPath();
        getFile(nodesPath);
        getFile(edgesPath);
        final List<LineageTreeNode> nodes = readNodes(nodesPath);
        final List<LineageTreeEdge> edges = readEdges(edgesPath,
                nodes.stream().map(LineageTreeNode::getName).collect(Collectors.toList()));
        final LineageTree lineageTree = getLineageTree(request, nodesPath, edgesPath);
        biologicalDataItemManager.createBiologicalDataItem(lineageTree);
        lineageTree.setBioDataItemId(lineageTree.getId());
        lineageTreeDao.saveLineageTree(lineageTree);
        nodes.forEach(n -> n.setLineageTreeId(lineageTree.getLineageTreeId()));
        edges.forEach(n -> n.setLineageTreeId(lineageTree.getLineageTreeId()));
        lineageTreeNodeDao.save(nodes);
        lineageTreeEdgeDao.save(edges, getNodesMap(nodes));
        lineageTree.setNodes(nodes);
        lineageTree.setEdges(edges);
        return lineageTree;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateLineageTreePaths(List<LineageTree> lineageTrees) {
        lineageTreeDao.updateLineageTreePaths(lineageTrees);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteLineageTree(final long lineageTreeId) {
        final LineageTree lineageTree = getLineageTree(lineageTreeId);
        lineageTreeEdgeDao.deleteLineageTreeEdges(lineageTreeId);
        lineageTreeNodeDao.deleteLineageTreeNodes(lineageTreeId);
        lineageTreeDao.deleteLineageTree(lineageTreeId);
        biologicalDataItemManager.deleteBiologicalDataItem(lineageTree.getBioDataItemId());
    }

    public LineageTree loadLineageTree(final long lineageTreeId) {
        final LineageTree lineageTree = lineageTreeDao.loadLineageTree(lineageTreeId);
        if (lineageTree == null) {
            return null;
        }
        lineageTree.setNodes(lineageTreeNodeDao.loadLineageTreeNodes(lineageTreeId));
        lineageTree.setEdges(lineageTreeEdgeDao.loadLineageTreeEdges(lineageTreeId));
        return lineageTree;
    }

    public List<LineageTree> loadLineageTrees(final long referenceId) {
        final List<LineageTreeNode> referenceNodes = lineageTreeNodeDao.loadLineageTreeNodesByReference(referenceId);
        final Set<Long> allTreeIds = referenceNodes.stream()
                .map(LineageTreeNode::getLineageTreeId)
                .collect(Collectors.toSet());
        return lineageTreeDao.loadLineageTrees(allTreeIds);
    }

    public List<LineageTree> loadAllLineageTrees() {
        return lineageTreeDao.loadAllLineageTrees();
    }

    private List<LineageTreeNode> readNodes(final String path) throws IOException {
        final String separator = FileFormat.TSV.getSeparator();
        final List<LineageTreeNode> nodes = new ArrayList<>();
        final List<String> nodeNames = new ArrayList<>();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            String[] cells;
            String nodeName;
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    break;
                }
                cells = line.split(separator);
                Assert.isTrue(cells.length == NODES_FILE_COLUMNS,
                        getMessage(MessagesConstants.ERROR_LINEAGE_INCORRECT_COLUMN_NUM, NODES_FILE_COLUMNS));
                nodeName = getCellValue(cells[0]);
                Assert.notNull(nodeName, getMessage(MessagesConstants.ERROR_LINEAGE_NODE_NAME_REQUIRED));
                Assert.isTrue(!nodeNames.contains(nodeName),
                        getMessage(MessagesConstants.ERROR_LINEAGE_NOT_UNIQUE_NODE_NAME, nodeName));

                LineageTreeNode node = LineageTreeNode.builder()
                        .name(nodeName)
                        .description(getCellValue(cells[1]))
                        .referenceId(getCellValue(cells[2]) == null ? null : Long.valueOf(getCellValue(cells[2])))
                        .projectId(getProjectId(getCellValue(cells[3])))
                        .creationDate(getCellValue(cells[4]) == null ? null :
                                LocalDate.parse(getCellValue(cells[4]), DateTimeFormatter.ofPattern(DATE_FORMAT)))
                        .attributes(parseAttributes(getCellValue(cells[5])))
                        .build();
                nodes.add(node);
                nodeNames.add(nodeName);
            }
        }
        return nodes;
    }

    private Long getProjectId(final String value) {
        if (value == null) {
            return null;
        }
        Project project;
        try {
            final long projectId = Long.parseLong(value);
            project = projectDao.loadProject(projectId);
            return project == null ? null : project.getId();
        } catch (NumberFormatException e) {
            project = projectDao.loadProject(value);
            return project == null ? null : project.getId();
        }
    }

    private List<LineageTreeEdge> readEdges(final String path, final List<String> nodes) throws IOException {
        final String separator = FileFormat.TSV.getSeparator();
        final List<LineageTreeEdge> edges = new ArrayList<>();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            String[] cells;
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    break;
                }
                cells = line.split(separator);
                Assert.isTrue(cells.length == EDGES_FILE_COLUMNS,
                        getMessage(MessagesConstants.ERROR_LINEAGE_INCORRECT_COLUMN_NUM, EDGES_FILE_COLUMNS));
                String nodeFromName = getCellValue(cells[0]);
                Assert.notNull(nodeFromName, getMessage(MessagesConstants.ERROR_LINEAGE_NODE_NAME_REQUIRED));
                Assert.isTrue(nodes.contains(nodeFromName),
                        getMessage(MessagesConstants.ERROR_LINEAGE_NODE_NOT_FOUND, nodeFromName));
                String nodeToName = getCellValue(cells[1]);
                Assert.notNull(nodeToName, getMessage(MessagesConstants.ERROR_LINEAGE_NODE_NAME_REQUIRED));
                Assert.isTrue(nodes.contains(nodeToName),
                        getMessage(MessagesConstants.ERROR_LINEAGE_NODE_NOT_FOUND, nodeToName));
                Assert.isTrue(!nodeFromName.equals(nodeToName),
                        getMessage(MessagesConstants.ERROR_LINEAGE_INCORRECT_EDGE, nodeFromName));
                LineageTreeEdge edge = LineageTreeEdge.builder()
                        .nodeFromName(nodeFromName)
                        .nodeToName(nodeToName)
                        .attributes(parseAttributes(getCellValue(cells[2])))
                        .typeOfInteraction(getCellValue(cells[3]))
                        .build();
                edges.add(edge);
            }
        }
        return edges;
    }

    @NotNull
    private LineageTree getLineageTree(final LineageTreeRegistrationRequest request,
                                       final String nodesPath,
                                       final String edgesPath) {
        final LineageTree lineageTree = LineageTree.builder()
                .nodesPath(nodesPath)
                .edgesPath(edgesPath)
                .description(request.getDescription())
                .build();
        lineageTree.setPath(nodesPath);
        lineageTree.setName(getBioDataItemName(request.getName(), nodesPath));
        lineageTree.setPrettyName(request.getPrettyName());
        lineageTree.setType(BiologicalDataItemResourceType.FILE);
        lineageTree.setFormat(BiologicalDataItemFormat.LINEAGE_TREE);
        lineageTree.setCreatedDate(new Date());
        lineageTree.setSource(nodesPath);
        return lineageTree;
    }

    @NotNull
    private Map<String, LineageTreeNode> getNodesMap(final List<LineageTreeNode> nodes) {
        return nodes.stream().collect(Collectors.toMap(LineageTreeNode::getName, Function.identity()));
    }

    private LineageTree getLineageTree(final long lineageTreeId) {
        final LineageTree lineageTree = lineageTreeDao.loadLineageTree(lineageTreeId);
        Assert.notNull(lineageTree, getMessage(MessagesConstants.ERROR_LINEAGE_TREE_NOT_FOUND, lineageTreeId));
        return lineageTree;
    }
}
