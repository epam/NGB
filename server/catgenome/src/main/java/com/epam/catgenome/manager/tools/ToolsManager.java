/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.manager.tools;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.tools.FeatureFileSortRequest;
import com.epam.catgenome.exception.SortingException;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.sort.AbstractFeatureSorter;
import com.epam.catgenome.util.sort.FeatureSorterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * Provides service for sorting feature files
 */
@Service
public class ToolsManager {

    private static final Logger LOG = LoggerFactory.getLogger(ToolsManager.class);

    private static final int DEFAULT_MAX_MEMORY = 500;

    @Autowired
    private FileManager fileManager;

    /**
     * Sorts feature file due to request parameters
     *
     * @param request a sorting request
     * @return a string representing path to the sorted file
     */
    public String sortFeatureFile(final FeatureFileSortRequest request) {
        try {
            final File ordinal = new File(request.getOriginalFilePath());
            final File sorted = new File(
                    request.getSortedFilePath() != null && !request.getSortedFilePath().isEmpty() ?
                            request.getSortedFilePath() :
                            FeatureSorterFactory.prepareSortedFile(request.getOriginalFilePath())
            );

            AbstractFeatureSorter sorter = FeatureSorterFactory.getSorter(ordinal, sorted, fileManager.getTempDir());

            LOG.debug("Will sort {} file with {}Mb of memory",
                    request.getOriginalFilePath(),
                    request.getMaxMemory());

            double time1 = Utils.getSystemTimeMilliseconds();
            sorter.run(request.getMaxMemory() > 0 ? request.getMaxMemory() : DEFAULT_MAX_MEMORY);
            double time2 = Utils.getSystemTimeMilliseconds();

            LOG.debug("Sorting feature file took {} ms", time2 - time1);
            LOG.info(getMessage(MessagesConstants.INFO_SORT_SUCCESS, sorted.getAbsolutePath()));

            return sorted.getAbsolutePath();
        } catch (IOException e) {
            throw new SortingException(e.getMessage(), e);
        }
    }
}
