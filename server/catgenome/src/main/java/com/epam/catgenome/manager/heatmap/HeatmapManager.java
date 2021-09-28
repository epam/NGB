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
import com.epam.catgenome.entity.heatmap.HeatmapDataType;
import com.epam.catgenome.entity.heatmap.HeatmapTree;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.util.FileFormat;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.olduvai.treejuxtaposer.TreeParser;
import net.sourceforge.olduvai.treejuxtaposer.drawer.Tree;
import net.sourceforge.olduvai.treejuxtaposer.drawer.TreeNode;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        Heatmap heatmap = Heatmap.builder()
                .rowTreePath(request.getRowTreePath())
                .columnTreePath(request.getColumnTreePath())
                .cellAnnotationPath(request.getCellAnnotationPath())
                .labelAnnotationPath(request.getLabelAnnotationPath())
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
        byte[] content = FileUtils.readFileToByteArray(file);
        byte[] labelAnnotation = readFileContent(heatmap.getLabelAnnotationPath(), heatmap, this::checkLabelAnnotation);
        byte[] cellAnnotation = readFileContent(heatmap.getCellAnnotationPath(), heatmap, this::checkCellAnnotation);
        byte[] rowTree = readFileContent(heatmap.getRowTreePath(),
                heatmap,
                h -> checkTree(h.getRowLabels(), h.getRowTreePath()));
        byte[] columnTree = readFileContent(heatmap.getColumnTreePath(),
                heatmap,
                h -> checkTree(h.getColumnLabels(), h.getColumnTreePath()));
        biologicalDataItemManager.createBiologicalDataItem(heatmap);
        heatmap.setBioDataItemId(heatmap.getId());
        return heatmapDao.saveHeatmap(heatmap,
                content,
                cellAnnotation,
                labelAnnotation,
                rowTree,
                columnTree);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateLabelAnnotation(final long heatmapId, final String path) throws IOException {
        Heatmap heatmap = getHeatmap(heatmapId);
        File file = null;
        if (!TextUtils.isBlank(path)) {
            file = getFile(path);
            heatmap.setLabelAnnotationPath(path);
            checkLabelAnnotation(heatmap);
        }
        heatmapDao.updateLabelAnnotation(heatmapId, file == null ? null :
                FileUtils.readFileToByteArray(file), path);
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
        checkTree(heatmap.getRowLabels(), path);
        heatmapDao.updateHeatmapRowTree(heatmapId, FileUtils.readFileToByteArray(file), path);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateColumnTree(final long heatmapId, final String path) throws IOException {
        File file = getFile(path);
        Heatmap heatmap = getHeatmap(heatmapId);
        heatmap.setColumnTreePath(path);
        checkTree(heatmap.getColumnLabels(), path);
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

    public List<List<Map<?, String>>> getContent(long heatmapId) throws IOException {
        Heatmap heatmap = getHeatmap(heatmapId);
        try (InputStream heatmapIS = heatmapDao.loadHeatmapContent(heatmapId);
                InputStream annotationIS = heatmapDao.loadCellAnnotation(heatmapId)) {
            return getAnnotatedContent(heatmapIS, annotationIS, heatmap.getCellValueType(), heatmap.getPath());
        }
    }

    public Map<String, String> getLabelAnnotation(final long heatmapId) throws IOException {
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        if (heatmap == null) {
            return null;
        }
        try (InputStream inputStream = heatmapDao.loadLabelAnnotation(heatmapId)) {
            return inputStream == null ? null : getDataAsMap(inputStream, heatmap.getLabelAnnotationPath());
        }
    }

    public HeatmapTree getTree(final long heatmapId) throws IOException {
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        HeatmapTree heatmapTree = new HeatmapTree();
        if (heatmap != null) {
            Tree tree;
            try (InputStream rowTreeIS = heatmapDao.loadHeatmapRowTree(heatmapId)) {
                tree = readTree(rowTreeIS, heatmap.getRowTreePath());
                heatmapTree.setRow(tree == null ? null : tree.getRoot());
            }
            try (InputStream columnTreeIS = heatmapDao.loadHeatmapColumnTree(heatmapId)) {
                tree = readTree(columnTreeIS, heatmap.getColumnTreePath());
                heatmapTree.setColumn(tree == null ? null : tree.getRoot());
            }
        }
        return heatmapTree;
    }

    private Tree readTree(final InputStream is, String path) throws IOException {
        if (is == null) {
            return null;
        }
        BufferedReader r = createReader(is);
        TreeParser tp = new TreeParser(r);
        return tp.tokenize(FilenameUtils.getBaseName(path));
    }

    private void readHeatmap(Heatmap heatmap) throws IOException {
        final String path = heatmap.getPath();
        final String separator = getSeparator(path);

        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            String[] cells = line.split(separator);
            List<String> columnLabels = new LinkedList<>();
            for (int i = 1; i < cells.length; i++) {
                columnLabels.add(cells[i]);
            }
            heatmap.setColumnLabels(columnLabels);
            int columnsNum = cells.length;
            List<String> rowLabels = new LinkedList<>();
            Set<String> values = new HashSet<>();
            Set<Integer> types = new HashSet<>(3);
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    break;
                }
                cells = line.split(separator);
                Assert.isTrue(cells.length == columnsNum,
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
                rowLabels.add(cells[0].trim());

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

    @NotNull
    private File getFile(final String path) {
        Assert.isTrue(!TextUtils.isBlank(path), getMessage(MessagesConstants.PATH_IS_REQUIRED));
        File file = new File(path);
        Assert.isTrue(file.isFile() && file.canRead(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        return file;
    }

    private String getSeparator(String path) {
        final String fileExtension = FilenameUtils.getExtension(path);
        final String separator = FileFormat.getSeparatorByExtension(fileExtension);
        Assert.notNull(separator, getMessage(MessagesConstants.ERROR_UNSUPPORTED_HEATMAP_FILE_EXTENSION));
        return separator;
    }

    private List<List<Map<?, String>>> getAnnotatedContent(final InputStream heatmapInputStream,
                                                           final InputStream annotationInputStream,
                                                           final HeatmapDataType dataType,
                                                           final String path) throws IOException {
        String separator = getSeparator(path);
        List<List<String>> content = getDataAsList(heatmapInputStream, separator);
        List<List<String>> annotation = annotationInputStream != null ?
                getDataAsList(annotationInputStream, separator) :
                null;
        List<List<Map<?, String>>> annotatedContent = new LinkedList<>();

        for (int i = 1; i < content.size(); i++) {
            final List<String> contentRow = content.get(i);
            final List<String> annotationRow = annotation != null ? annotation.get(i) : null;
            List<Map<?, String>> annotatedContentRow = new LinkedList<>();
            for (int j = 1; j < contentRow.size(); j++) {
                String value = annotationRow == null ? "" :
                        (annotationRow.get(j).equals(".") ? "" : annotationRow.get(j));
                if (dataType == HeatmapDataType.INTEGER) {
                    Map<Integer, String> annotatedCell = new HashMap<>();
                    annotatedCell.put(Integer.parseInt(contentRow.get(j)), value);
                    annotatedContentRow.add(annotatedCell);
                } else if (dataType == HeatmapDataType.DOUBLE) {
                    Map<Double, String> annotatedCell = new HashMap<>();
                    annotatedCell.put(Double.parseDouble(contentRow.get(j)), value);
                    annotatedContentRow.add(annotatedCell);
                } else {
                    Map<String, String> annotatedCell = new HashMap<>();
                    annotatedCell.put(contentRow.get(j), value);
                    annotatedContentRow.add(annotatedCell);
                }
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

    private Map<String, String> getDataAsMap(final InputStream inputStream, final String path) throws IOException {
        final String separator = getSeparator(path);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            Map<String, String> content = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cells = line.split(separator);
                if (!content.containsKey(cells[0].trim())) {
                    content.put(cells[0].trim(), cells[1].trim());
                }
            }
            return content;
        }
    }

    @SneakyThrows
    private void checkLabelAnnotation(final Heatmap heatmap) {
        final String path = heatmap.getLabelAnnotationPath();
        final String separator = getSeparator(path);
        final List<String> rowLabels = heatmap.getRowLabels();
        final List<String> columnLabels = heatmap.getColumnLabels();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            String[] cells;
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(separator);
                Assert.isTrue(cells.length == 2, getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
                Assert.isTrue(rowLabels.contains(cells[0].trim()) || columnLabels.contains(cells[0].trim()),
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
            }
        }
    }

    @SneakyThrows
    private void checkCellAnnotation(final Heatmap heatmap) {
        final String path = heatmap.getCellAnnotationPath();
        final String separator = getSeparator(path);
        final List<String> rowLabels = heatmap.getRowLabels();
        final List<String> columnLabels = heatmap.getColumnLabels();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            String[] cells = line.split(separator);
            for (int i = 1; i < cells.length; i++) {
                Assert.isTrue(columnLabels.get(i - 1).equals(cells[i].trim()),
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
            }
            int rowNum = 0;
            int columnsCount = columnLabels.size() + 1;
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(separator);
                Assert.isTrue(cells.length == columnsCount,
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
                Assert.isTrue(rowLabels.get(rowNum).equals(cells[0]),
                        getMessage(MessagesConstants.ERROR_INCORRECT_FILE_FORMAT));
                rowNum++;
            }
        }
    }

    @SneakyThrows
    private void checkTree(final List<String> labels, String path) {
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
}
