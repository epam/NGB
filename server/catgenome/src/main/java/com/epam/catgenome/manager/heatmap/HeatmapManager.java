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

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.heatmap.HeatmapDao;
import com.epam.catgenome.entity.heatmap.Heatmap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapManager {

    public static final String MAX_TARGET_SEQS = "max_target_seqs";

    private final HeatmapDao heatmapDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public Heatmap createHeatmap(final Heatmap heatmap) {
        Heatmap exHeatmap = heatmapDao.saveHeatmap(heatmap);
        if (!CollectionUtils.isEmpty(heatmap.getValues())) {
            heatmapDao.saveHeatmapValues(heatmap.getValues(), exHeatmap.getHeatmapId());
        }
        return exHeatmap;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteHeatmap(final long heatmapId) {
        Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        Assert.notNull(heatmap, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, heatmap));
        heatmapDao.deleteHeatmapValues(heatmapId);
        heatmapDao.deleteHeatmap(heatmapId);
    }

    public Heatmap getHeatmap(final long heatmapId) {
        final Heatmap heatmap = heatmapDao.loadHeatmap(heatmapId);
        Assert.notNull(heatmap, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, heatmapId));
        Set<String> heatmapValues = new HashSet<>(heatmapDao.loadHeatmapValues(heatmapId));
        heatmap.setValues(heatmapValues);
        return heatmap;
    }

    public String[][] getContent(long heatmapId) {
        heatmapDao.loadHeatmapContent(heatmapId);
        return null;
    }

    public String[][] getAnnotation(long heatmapId) {
        heatmapDao.loadHeatmapAnnotation(heatmapId);
        return null;
    }

    public String[][] getTree(long heatmapId) {
        heatmapDao.loadHeatmapTree(heatmapId);
        return null;
    }

    public Heatmap readHeatmap(String fileName) throws IOException {
        try (Reader reader = new FileReader(fileName); BufferedReader bufferedReader = new BufferedReader(reader)) {
            Heatmap heatmap = new Heatmap();
            //header
            String line = bufferedReader.readLine();
            String[] cells = line.split("\\t");
            heatmap.setColumnLabels(Arrays.asList(cells));
            //data
            List<String> rowLabels = new ArrayList<>();
            Set<String> values = new HashSet<>();
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split("\\t");
                rowLabels.add(cells[0].trim());
                if (cells[1].matches("\\d+")) {
                    heatmap.setCellValueType("Int");
                }
                rowLabels.addAll(Arrays.asList(cells).subList(1, cells.length));
            }
            heatmap.setValues(values);
            heatmap.setRowLabels(rowLabels);
        }
        return null;
    }
}
