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

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.HeatmapRegistrationRequest;
import com.epam.catgenome.dao.heatmap.HeatmapDao;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.heatmap.Heatmap;
import com.epam.catgenome.entity.heatmap.HeatmapAnnotationType;
import com.epam.catgenome.entity.heatmap.HeatmapDataType;
import com.epam.catgenome.entity.heatmap.HeatmapTree;
import com.epam.catgenome.entity.heatmap.HeatmapTreeNode;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.util.FileFormat;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.olduvai.treejuxtaposer.TreeParser;
import net.sourceforge.olduvai.treejuxtaposer.drawer.Tree;
import net.sourceforge.olduvai.treejuxtaposer.drawer.TreeNode;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static org.forester.io.parsers.util.ParserUtils.createReader;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapManager {

    @Value("${heatmap.values.max.size:100}")
    private int valuesMaxSize;

    private final HeatmapDao heatmapDao;
    private final BiologicalDataItemManager biologicalDataItemManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public Heatmap createHeatmap(final HeatmapRegistrationRequest request) throws IOException {
        String path = request.getPath();
        File file = getFile(path);
        Heatmap heatmap = getHeatmap(request, path);
        Map<String, String> labelAnnotation = readLabelAnnotation(heatmap.getLabelAnnotationPath());
        updateHeatmapLabels(heatmap, labelAnnotation);
        byte[] content = FileUtils.readFileToByteArray(file);
        byte[] cellAnnotation = readFileContent(heatmap.getCellAnnotationPath(), heatmap, this::checkCellAnnotation);
        byte[] rowTree = readFileContent(heatmap.getRowTreePath(),
            heatmap,
            h -> checkTree(getLabelSet(h.getRowLabels()), h.getRowTreePath()));
        byte[] columnTree = readFileContent(heatmap.getColumnTreePath(),
            heatmap,
            h -> checkTree(getLabelSet(h.getColumnLabels()), h.getColumnTreePath()));
        biologicalDataItemManager.createBiologicalDataItem(heatmap);
        heatmap.setBioDataItemId(heatmap.getId());
        return heatmapDao.saveHeatmap(heatmap,
                content,
                cellAnnotation,
                rowTree,
                columnTree);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateLabelAnnotation(final long heatmapId, final String path) throws IOException {
        Heatmap heatmap = getHeatmap(heatmapId);
        if (!TextUtils.isBlank(path)) {
            getFile(path);
            Map<String, String> labelAnnotation = readLabelAnnotation(path);
            updateHeatmapLabels(heatmap, labelAnnotation);
        }
        heatmapDao.updateLabelAnnotation(heatmap, path);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateCellAnnotation(final long heatmapId, final String path) throws IOException {
        Heatmap heatmap = getHeatmap(heatmapId);
        File file = null;
        if (!TextUtils.isBlank(path)) {
            file = getFile(path);
            heatmap.setCellAnnotationPath(path);
            checkCellAnnotation(heatmap);
        }
        heatmapDao.updateCellAnnotation(heatmapId, file == null ? null :
                FileUtils.readFileToByteArray(file), path);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateRowTree(final long heatmapId, final String path) throws IOException {
        File file = getFile(path);
        Heatmap heatmap = getHeatmap(heatmapId);
        heatmap.setRowTreePath(path);
        checkTree(getLabelSet(heatmap.getRowLabels()), path);
        heatmapDao.updateHeatmapRowTree(heatmapId, FileUtils.readFileToByteArray(file), path);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateColumnTree(final long heatmapId, final String path) throws IOException {
        File file = getFile(path);
        Heatmap heatmap = getHeatmap(heatmapId);
        heatmap.setColumnTreePath(path);
        checkTree(getLabelSet(heatmap.getColumnLabels()), path);
        heatmapDao.updateHeatmapColumnTree(heatmapId, FileUtils.readFileToByteArray(file), path);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteHeatmap(final long heatmapId) {
        Heatmap heatmap = getHeatmap(heatmapId);
        heatmapDao.deleteHeatmap(heatmapId);
        biologicalDataItemManager.deleteBiologicalDataItem(heatmap.getBioDataItemId());
    }

    public Heatmap loadHeatmap(final long heatmapId) {
        return heatmapDao.loadHeatmap(heatmapId);
    }

    public List<Heatmap> loadHeatmaps() {
        return heatmapDao.loadHeatmaps();
    }

    public List<List<List<String>>> getContent(final long heatmapId) throws IOException {
        Heatmap heatmap = getHeatmap(heatmapId);
        try (InputStream heatmapIS = heatmapDao.loadHeatmapContent(heatmapId);
                InputStream annotationIS = heatmapDao.loadCellAnnotation(heatmapId)) {
            return getAnnotatedContent(heatmapIS, annotationIS, heatmap.getPath());
        }
    }

    public HeatmapTree getTree(final long heatmapId) throws IOException {
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        HeatmapTree heatmapTree = new HeatmapTree();
        if (heatmap != null) {
            Tree tree;
            try (InputStream rowTreeIS = heatmapDao.loadHeatmapRowTree(heatmapId)) {
                tree = readTree(rowTreeIS, heatmap.getRowTreePath());
                heatmapTree.setRow(tree == null ? null : convertTree(tree.getRoot()));
            }
            try (InputStream columnTreeIS = heatmapDao.loadHeatmapColumnTree(heatmapId)) {
                tree = readTree(columnTreeIS, heatmap.getColumnTreePath());
                heatmapTree.setColumn(tree == null ? null : convertTree(tree.getRoot()));
            }
        }
        return heatmapTree;
    }

    private HeatmapTreeNode convertTree(final TreeNode node) {
        HeatmapTreeNode heatmapTreeNode = HeatmapTreeNode.builder()
                .name(node.getName())
                .weight(node.getWeight())
                .build();
        List<HeatmapTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.numberChildren(); i++) {
            HeatmapTreeNode newickTreeNodeChild = convertTree(node.getChild(i));
            children.add(newickTreeNodeChild);
        }
        heatmapTreeNode.setChildren(children);
        return heatmapTreeNode;
    }

    private Tree readTree(final InputStream is, final String path) throws IOException {
        if (is == null) {
            return null;
        }
        BufferedReader r = createReader(is);
        TreeParser tp = new TreeParser(r);
        return tp.tokenize(FilenameUtils.getBaseName(path));
    }

    @NotNull
    private Heatmap getHeatmap(final HeatmapRegistrationRequest request, final String path) throws IOException {
        Heatmap heatmap = Heatmap.builder()
                .rowTreePath(request.getRowTreePath())
                .columnTreePath(request.getColumnTreePath())
                .cellAnnotationPath(request.getCellAnnotationPath())
                .cellAnnotationType(getAnnotationType(request.getCellAnnotationType()))
                .labelAnnotationPath(request.getLabelAnnotationPath())
                .rowAnnotationType(getAnnotationType(request.getRowAnnotationType()))
                .columnAnnotationType(getAnnotationType(request.getColumnAnnotationType()))
                .build();
        heatmap.setPath(path);
        heatmap.setName(TextUtils.isBlank(request.getName()) ? FilenameUtils.getBaseName(path) : request.getName());
        heatmap.setPrettyName(TextUtils.isBlank(request.getPrettyName()) ?
                FilenameUtils.getBaseName(path) :
                request.getPrettyName());
        heatmap.setType(BiologicalDataItemResourceType.FILE);
        heatmap.setFormat(BiologicalDataItemFormat.HEATMAP);
        heatmap.setCreatedDate(new Date());
        heatmap.setSource(path);
        readHeatmap(heatmap);
        return heatmap;
    }

    private void readHeatmap(final Heatmap heatmap) throws IOException {
        final String path = heatmap.getPath();
        final String separator = getSeparator(path);

        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            String[] cells = line.split(separator);
            List<List<String>> columnLabels = new LinkedList<>();
            for (int i = 1; i < cells.length; i++) {
                columnLabels.add(Collections.singletonList(cells[i].trim()));
            }
            heatmap.setColumnLabels(columnLabels);
            int columnsNum = cells.length;
            List<List<String>> rowLabels = new LinkedList<>();
            Set<String> values = new HashSet<>();
            Set<Integer> types = new HashSet<>(3);
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    break;
                }
                cells = line.split(separator);
                Assert.isTrue(cells.length == columnsNum,
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
                rowLabels.add(Collections.singletonList(cells[0].trim()));

                for (int i = 1; i < cells.length; i++) {
                    try {
                        Integer.parseInt(cells[i]);
                        types.add(HeatmapDataType.INTEGER.getId());
                    } catch (NumberFormatException e) {
                        try {
                            Double.parseDouble(cells[i]);
                            types.add(HeatmapDataType.DOUBLE.getId());
                        } catch (NumberFormatException e1) {
                            types.add(HeatmapDataType.STRING.getId());
                        }
                    }
                    values.add(cells[i].trim());
                }
            }
            heatmap.setRowLabels(rowLabels);
            HeatmapDataType cellValueType = HeatmapDataType.getById(Collections.max(types));
            heatmap.setCellValueType(cellValueType);
            switch (cellValueType) {
                case INTEGER:
                    heatmap.setCellValues(values.stream()
                            .map(Integer::parseInt)
                            .sorted()
                            .limit(valuesMaxSize)
                            .collect(Collectors.toSet()));
                    break;
                case DOUBLE:
                    heatmap.setCellValues(values.stream()
                            .map(Double::parseDouble)
                            .sorted()
                            .limit(valuesMaxSize)
                            .collect(Collectors.toSet()));
                    break;
                default:
                    heatmap.setCellValues(values.stream().limit(valuesMaxSize).collect(Collectors.toSet()));
                    break;
            }
            if (cellValueType != HeatmapDataType.STRING) {
                heatmap.setMaxCellValue(Collections.max(values
                        .stream()
                        .map(Double::parseDouble)
                        .collect(Collectors.toList())));
                heatmap.setMinCellValue(Collections.min(values
                        .stream()
                        .map(Double::parseDouble)
                        .collect(Collectors.toList())));
            }
        }
    }

    @NotNull
    private List<List<String>> updateLabels(final List<List<String>> labels,
                                            final Map<String, String> labelAnnotation) {
        return labels.stream()
                .map(l -> l.get(0))
                .map(k -> labelAnnotation.containsKey(k) ?
                        Arrays.asList(k, labelAnnotation.get(k)) :
                        Collections.singletonList(k))
                .collect(Collectors.toList());
    }

    private void updateHeatmapLabels(final Heatmap heatmap, final Map<String, String> labelAnnotation) {
        heatmap.setColumnLabels(updateLabels(heatmap.getColumnLabels(), labelAnnotation));
        heatmap.setRowLabels(updateLabels(heatmap.getRowLabels(), labelAnnotation));
    }

    private Heatmap getHeatmap(final long heatmapId) {
        final Heatmap heatmap = loadHeatmap(heatmapId);
        Assert.notNull(heatmap, getMessage(MessagesConstants.ERROR_HEATMAP_NOT_FOUND, heatmapId));
        return heatmap;
    }

    private byte[] readFileContent(final String path, final Heatmap heatmap, final Consumer<Heatmap> validator) {
        if (path != null) {
            File file = getFile(path);
            validator.accept(heatmap);
            try {
                return  FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                log.debug(e.getMessage(), e);
            }
        }
        return null;
    }

    private Map<String, String> readLabelAnnotation(final String path) throws IOException {
        Map<String, String> annotation = new HashMap<>();
        if (path != null) {
            final String separator = getSeparator(path);
            try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (StringUtils.isBlank(line)) {
                        break;
                    }
                    String[] cells = line.split(separator);
                    Assert.isTrue(cells.length == 2, getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
                    annotation.put(cells[0].trim(), cells[1].trim());
                }
            }
        }
        return annotation;
    }

    @NotNull
    private File getFile(final String path) {
        Assert.isTrue(!TextUtils.isBlank(path), getMessage(MessagesConstants.PATH_IS_REQUIRED));
        File file = new File(path);
        Assert.isTrue(file.isFile() && file.canRead(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        return file;
    }

    private String getSeparator(final String path) {
        final String fileExtension = FilenameUtils.getExtension(path);
        final String separator = FileFormat.getSeparatorByExtension(fileExtension);
        Assert.notNull(separator, getMessage(MessagesConstants.ERROR_UNSUPPORTED_HEATMAP_FILE_EXTENSION));
        return separator;
    }

    private List<List<List<String>>> getAnnotatedContent(final InputStream heatmapInputStream,
                                                           final InputStream annotationInputStream,
                                                           final String path) throws IOException {
        String separator = getSeparator(path);
        List<List<String>> content = getDataAsList(heatmapInputStream, separator);
        List<List<String>> annotation = annotationInputStream != null ?
                getDataAsList(annotationInputStream, separator) :
                null;
        List<List<List<String>>> annotatedContent = new LinkedList<>();

        for (int i = 1; i < content.size(); i++) {
            final List<String> contentRow = content.get(i);
            final List<String> annotationRow = annotation != null ? annotation.get(i) : null;
            List<List<String>> annotatedContentRow = new LinkedList<>();
            for (int j = 1; j < contentRow.size(); j++) {
                List<String> annotatedCell = annotation == null ?
                        Collections.singletonList(contentRow.get(j)) :
                        Arrays.asList(contentRow.get(j), annotationRow.get(j));
                annotatedContentRow.add(annotatedCell);
            }
            annotatedContent.add(annotatedContentRow);
        }
        return annotatedContent;
    }

    private List<List<String>> getDataAsList(final InputStream inputStream, final String separator) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<List<String>> content = new LinkedList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cells = line.split(separator);
                content.add(Arrays.asList(cells));
            }
            return content;
        }
    }

    @SneakyThrows
    private void checkCellAnnotation(final Heatmap heatmap) {
        final String path = heatmap.getCellAnnotationPath();
        final String separator = getSeparator(path);
        final List<List<String>> rowLabels = heatmap.getRowLabels();
        final List<List<String>> columnLabels = heatmap.getColumnLabels();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            String[] cells = line.split(separator);
            for (int i = 1; i < cells.length; i++) {
                Assert.isTrue(columnLabels.get(i - 1).get(0).equals(cells[i].trim()),
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
            }
            int rowNum = 0;
            int columnsCount = columnLabels.size() + 1;
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(separator);
                Assert.isTrue(cells.length == columnsCount,
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
                Assert.isTrue(rowLabels.get(rowNum).get(0).equals(cells[0]),
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
                rowNum++;
            }
        }
    }

    @NotNull
    private HeatmapAnnotationType getAnnotationType(final HeatmapAnnotationType columnAnnotationType) {
        return Optional.ofNullable(columnAnnotationType)
                .orElse(HeatmapAnnotationType.NONE);
    }

    @SneakyThrows
    private void checkTree(final Set<String> labels, final String path) {
        BufferedReader r = createReader(path);
        TreeParser tp = new TreeParser(r);
        Tree tree = tp.tokenize(FilenameUtils.getBaseName(path));
        List<String> treeLabels = tree.nodes.stream()
                .map(TreeNode::getName)
                .filter(f -> !f.isEmpty())
                .filter(t -> !labels.contains(t))
                .collect(Collectors.toList());
        Assert.isTrue(treeLabels.isEmpty(), getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
    }

    private Set<String> getLabelSet(final List<List<String>> labels) {
        return ListUtils.emptyIfNull(labels).stream().map(l -> l.get(0)).collect(Collectors.toSet());
    }
}
