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
import com.epam.catgenome.dao.heatmap.HeatmapDao;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.heatmap.Heatmap;
import com.epam.catgenome.entity.heatmap.HeatmapDataType;
import com.epam.catgenome.entity.heatmap.HeatmapTree;
import com.epam.catgenome.util.FileFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapManager {

    @Value("${heatmap.values.max.size:100}")
    private int valuesMaxSize;

    private final HeatmapDao heatmapDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public Heatmap createHeatmap(final Heatmap heatmap) throws IOException {
        Assert.isTrue(!TextUtils.isBlank(heatmap.getName()), MessagesConstants.HEATMAP_NAME_IS_REQUIRED);
        String path = heatmap.getPath();
        Assert.isTrue(!TextUtils.isBlank(path), MessagesConstants.PATH_IS_REQUIRED);
        File file = getFile(path);
        readHeatmap(heatmap);
        heatmap.setType(BiologicalDataItemResourceType.FILE);
        byte[] content = FileUtils.readFileToByteArray(file);
        byte[] labelAnnotation = null;
        byte[] cellAnnotation = null;
        byte[] rowTree =  null;
        byte[] columnTree = null;
        path = heatmap.getLabelAnnotationPath();
        if (path != null) {
            file = getFile(path);
            checkLabelAnnotation(path, heatmap.getRowLabels(), heatmap.getColumnLabels());
            labelAnnotation = FileUtils.readFileToByteArray(file);
        }
        path = heatmap.getCellAnnotationPath();
        if (path != null) {
            file = getFile(path);
            checkCellAnnotation(path, heatmap.getRowLabels(), heatmap.getColumnLabels());
            cellAnnotation = FileUtils.readFileToByteArray(file);
        }
        path = heatmap.getRowTreePath();
        if (path != null) {
            file = getFile(path);
            rowTree = FileUtils.readFileToByteArray(file);
        }
        path = heatmap.getColumnTreePath();
        if (path != null) {
            file = getFile(path);
            columnTree = FileUtils.readFileToByteArray(file);
        }
        return heatmapDao.saveHeatmap(heatmap,
                content,
                cellAnnotation,
                labelAnnotation,
                rowTree,
                columnTree);
    }

    @NotNull
    private File getFile(final String path) {
        File file = new File(path);
        Assert.isTrue(file.isFile() && file.canRead(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        return file;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateLabelAnnotation(final long heatmapId, final String path) throws IOException {
        Assert.isTrue(!TextUtils.isBlank(path), MessagesConstants.PATH_IS_REQUIRED);
        File file = getFile(path);
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        Assert.notNull(heatmap, getMessage(MessagesConstants.ERROR_HEATMAP_NOT_FOUND, heatmapId));
        checkLabelAnnotation(path, heatmap.getRowLabels(), heatmap.getColumnLabels());
        heatmapDao.updateLabelAnnotation(heatmapId, FileUtils.readFileToByteArray(file), path);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateCellAnnotation(final long heatmapId, final String path) throws IOException {
        Assert.isTrue(!TextUtils.isBlank(path), MessagesConstants.PATH_IS_REQUIRED);
        File file = getFile(path);
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        Assert.notNull(heatmap, getMessage(MessagesConstants.ERROR_HEATMAP_NOT_FOUND, heatmapId));
        checkCellAnnotation(path, heatmap.getRowLabels(), heatmap.getColumnLabels());
        heatmapDao.updateCellAnnotation(heatmapId, FileUtils.readFileToByteArray(file), path);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteHeatmap(final long heatmapId) {
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        Assert.notNull(heatmap, getMessage(MessagesConstants.ERROR_HEATMAP_NOT_FOUND, heatmapId));
        heatmapDao.deleteHeatmap(heatmapId);
    }

    public Heatmap getHeatmap(final long heatmapId) {
        final Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        Assert.notNull(heatmap, getMessage(MessagesConstants.ERROR_HEATMAP_NOT_FOUND, heatmapId));
        return heatmap;
    }

    public List<List<Map<?, String>>> getContent(long heatmapId) throws IOException {
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        Assert.notNull(heatmap, getMessage(MessagesConstants.ERROR_HEATMAP_NOT_FOUND, heatmapId));
        final String separator = getSeparator(heatmap.getPath());
        InputStream heatmapInputStream = heatmapDao.loadHeatmapContent(heatmapId);
        InputStream annotationInputStream = heatmapDao.loadCellAnnotation(heatmapId);
        return getAnnotatedContent(heatmapInputStream, annotationInputStream, heatmap.getCellValueType(), separator);
    }

    public List<List<String>> getLabelAnnotation(final long heatmapId) throws IOException {
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        Assert.notNull(heatmap, getMessage(MessagesConstants.ERROR_HEATMAP_NOT_FOUND, heatmapId));
        final String separator = getSeparator(heatmap.getLabelAnnotationPath());
        InputStream inputStream = heatmapDao.loadLabelAnnotation(heatmapId);
        return inputStream == null ? null : getData(inputStream, separator);
    }

    public HeatmapTree getTree(long heatmapId) {
//        heatmapDao.loadHeatmapRowTree(heatmapId);
//        heatmapDao.loadHeatmapColumnTree(heatmapId);
        return new HeatmapTree();
    }

    public void readHeatmap(Heatmap heatmap) throws IOException {
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
                cells = line.split(separator);
                Assert.isTrue(cells.length == columnsNum, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
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
                    List<Integer> intValues = values.stream()
                            .map(Integer::parseInt)
                            .sorted()
                            .collect(Collectors.toList());
                    Set<Integer> limitedIntValues = new LinkedHashSet<>();
                    for (int i = 0; i < valuesMaxSize - 1; i++) {
                        limitedIntValues.add(intValues.get(i));
                    }
                    heatmap.setCellValues(limitedIntValues);
                    break;
                case DOUBLE:
                    List<Double> doubleValues = values.stream()
                            .map(Double::parseDouble)
                            .sorted()
                            .collect(Collectors.toList());
                    Set<Double> limitedDoubleValues = new LinkedHashSet<>();
                    for (int i = 0; i < valuesMaxSize - 1; i++) {
                        limitedDoubleValues.add(doubleValues.get(i));
                    }
                    heatmap.setCellValues(limitedDoubleValues);
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

    private String getSeparator(String path) {
        final String fileExtension = FilenameUtils.getExtension(path);
        final String separator = FileFormat.getSeparatorByExtension(fileExtension);
        Assert.notNull(separator, MessagesConstants.ERROR_UNSUPPORTED_HEATMAP_FILE_EXTENSION);
        return separator;
    }

    private List<List<Map<?, String>>> getAnnotatedContent(final InputStream heatmapInputStream,
                                                           final InputStream annotationInputStream,
                                                           final HeatmapDataType dataType,
                                                           final String separator) throws IOException {
        List<List<String>> content = getData(heatmapInputStream, separator);
        List<List<String>> annotation = annotationInputStream != null ?
                getData(annotationInputStream, separator) :
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

    private List<List<String>> getData(final InputStream inputStream, final String separator) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<List<String>> content = new LinkedList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] cells = line.split(separator);
            content.add(Arrays.asList(cells));
        }
        return content;
    }

    private void checkCellAnnotation(final String path,
                                    final List<String> rowLabels,
                                    final List<String> columnLabels) throws IOException {
        final String separator = getSeparator(path);
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            String[] cells = line.split(separator);
            for (int i = 1; i < cells.length; i++) {
                Assert.isTrue(columnLabels.get(i - 1).equals(cells[i].trim()),
                        MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            }
            int rowNum = 0;
            int columnsCount = columnLabels.size() + 1;
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(separator);
                Assert.isTrue(cells.length == columnsCount, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
                Assert.isTrue(rowLabels.get(rowNum).equals(cells[0]), MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
                rowNum++;
            }
        }
    }

    private void checkLabelAnnotation(final String path,
                                    final List<String> rowLabels,
                                    final List<String> columnLabels) throws IOException {
        final String separator = getSeparator(path);
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            String[] cells;
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(separator);
                Assert.isTrue(cells.length == 2, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
                Assert.isTrue(rowLabels.contains(cells[0].trim()) || columnLabels.contains(cells[0].trim()),
                        MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            }
        }
    }
}
