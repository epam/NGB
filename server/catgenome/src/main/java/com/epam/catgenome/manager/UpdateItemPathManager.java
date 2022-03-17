/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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

package com.epam.catgenome.manager;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.heatmap.Heatmap;
import com.epam.catgenome.entity.lineage.LineageTree;
import com.epam.catgenome.entity.notification.NotificationMessageVO;
import com.epam.catgenome.exception.CloudPipelineUnavailableException;
import com.epam.catgenome.manager.cloud.pipeline.CloudPipelineManager;
import com.epam.catgenome.manager.heatmap.HeatmapManager;
import com.epam.catgenome.manager.lineage.LineageTreeManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.epam.catgenome.util.IOHelper.resourceExists;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
public class UpdateItemPathManager {

    private static final String UPDATED_FILES = "Missing file was found '%s'. " +
            "File path was replaced with a new path '%s'.";
    private static final String NOT_UPDATED_FILES = "Missing file was found '%s'. Unable to find a new path, " +
            "please check and fix file location manually.";
    private final List<String> files = new ArrayList<>();

    @Value("${item.path.update.notification.subject}")
    private String subject;

    @Value("${item.path.update.notification.to}")
    private String toUser;

    @Value("${item.path.update.notification.cc}")
    private String ccUser;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private HeatmapManager heatmapManager;

    @Autowired
    private LineageTreeManager lineageTreeManager;

    @Autowired
    private CloudPipelineManager cloudPipelineManager;

    public void updateItemPath(final String currPathPattern, final String newPathPattern) throws IOException {
        final List<BiologicalDataItem> items = biologicalDataItemManager.loadAllItems();
        final List<BiologicalDataItem> itemsToBeUpdated = new ArrayList<>();
        final List<Heatmap> heatmaps = new ArrayList<>();
        final List<LineageTree> lineageTrees = new ArrayList<>();
        String path;
        String newFilePath;
        boolean updateItem = false;
        for (BiologicalDataItem item : items) {
            if (!item.getFormat().isIndex()) {
                if (BiologicalDataItemFormat.HEATMAP.equals(item.getFormat())) {
                    Heatmap heatmap = (Heatmap) item;
                    path = heatmap.getColumnTreePath();
                    newFilePath = getNewPath(path, currPathPattern,  newPathPattern);
                    if (newFilePath != null) {
                        heatmap.setColumnTreePath(newFilePath);
                        updateItem = true;
                    }
                    path = heatmap.getRowTreePath();
                    newFilePath = getNewPath(path, currPathPattern,  newPathPattern);
                    if (newFilePath != null) {
                        heatmap.setRowTreePath(newFilePath);
                        updateItem = true;
                    }
                    path = heatmap.getCellAnnotationPath();
                    newFilePath = getNewPath(path, currPathPattern,  newPathPattern);
                    if (newFilePath != null) {
                        heatmap.setCellAnnotationPath(newFilePath);
                        updateItem = true;
                    }
                    path = heatmap.getLabelAnnotationPath();
                    newFilePath = getNewPath(path, currPathPattern,  newPathPattern);
                    if (newFilePath != null) {
                        heatmap.setLabelAnnotationPath(newFilePath);
                        updateItem = true;
                    }
                    if (updateItem) {
                        heatmaps.add(heatmap);
                        updateItem = false;
                    }
                }
                if (BiologicalDataItemFormat.LINEAGE_TREE.equals(item.getFormat())) {
                    LineageTree lineageTree = (LineageTree) item;
                    path = lineageTree.getEdgesPath();
                    newFilePath = getNewPath(path, currPathPattern,  newPathPattern);
                    if (newFilePath != null) {
                        lineageTree.setEdgesPath(newFilePath);
                        updateItem = true;
                    }
                    path = lineageTree.getNodesPath();
                    newFilePath = getNewPath(path, currPathPattern,  newPathPattern);
                    if (newFilePath != null) {
                        lineageTree.setNodesPath(newFilePath);
                        updateItem = true;
                    }
                    if (updateItem) {
                        lineageTrees.add(lineageTree);
                        updateItem = false;
                    }
                }
                path = item.getPath();
                newFilePath = getNewPath(path, currPathPattern,  newPathPattern);
                if (newFilePath != null) {
                    item.setId(BiologicalDataItem.getBioDataItemId(item));
                    item.setPath(newFilePath);
                    itemsToBeUpdated.add(item);
                }
            }
        }
        heatmapManager.updateHeatmapPaths(heatmaps);
        lineageTreeManager.updateLineageTreePaths(lineageTrees);
        biologicalDataItemManager.updateBiologicalDataItemPath(itemsToBeUpdated);
        sendNotification();
    }

    private String getNewPath(final String path, final String currPathPattern, final String newPathPattern)
            throws IOException {
        if (StringUtils.isNotBlank(path) && !resourceExists(path)) {
            final String newFilePath = modifyPath(path, currPathPattern, newPathPattern);
            if (newFilePath != null && resourceExists(newFilePath)) {
                files.add(String.format(UPDATED_FILES, path, newFilePath));
                return newFilePath;
            }
            files.add(String.format(NOT_UPDATED_FILES, path));
        }
        return null;
    }

    private String modifyPath(final String path, final String currPathPattern, final String newPathPattern) {
        if (path.contains(currPathPattern)) {
            return path.replace(currPathPattern, newPathPattern);
        } else if (path.contains(newPathPattern)) {
            return path.replace(newPathPattern, currPathPattern);
        }
        return null;
    }

    private void sendNotification() {
        final List<String> copyUsers = Arrays.asList(ccUser.split(";"));
        final String subject = join(files, System.lineSeparator());
        final NotificationMessageVO message = NotificationMessageVO.builder()
                .subject(subject)
                .toUser(toUser)
                .copyUsers(copyUsers)
                .subject(subject)
                .build();
        try {
            cloudPipelineManager.sendNotification(message);
        } catch (CloudPipelineUnavailableException e) {
            log.error(e.getMessage());
        }
    }
}
