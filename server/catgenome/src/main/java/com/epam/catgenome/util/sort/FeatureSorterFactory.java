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

package com.epam.catgenome.util.sort;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.util.NgbFileUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;

import static com.epam.catgenome.component.MessageHelper.getMessage;

public final class FeatureSorterFactory {

    private FeatureSorterFactory() {

    }

    public static AbstractFeatureSorter getSorter(File inputFile, File outputFile, File tmpDir)
            throws IOException {

        BiologicalDataItemFormat format = NgbFileUtils.getFormatByExtension(inputFile.getName());

        if (format == null) {
            throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_FEATURE_FILE_SORT_TYPE,
                    inputFile.getName()));
        }

        switch (format) {
            case BED:
                return new BedSorter(inputFile, outputFile, tmpDir);
            case GENE:
                return new GFFSorter(inputFile, outputFile, tmpDir);
            case VCF:
                return new VCFSorter(inputFile, outputFile, tmpDir);
            default:
                throw new IllegalArgumentException(getMessage(
                        MessagesConstants.ERROR_UNSUPPORTED_FEATURE_FILE_SORT_TYPE, inputFile.getName()));
        }
    }

    public static String prepareSortedFile(String toBeSortedPath) throws IOException {
        final String sortedBedPath = AbstractFeatureSorter.addSortedSuffix(toBeSortedPath);
        final File sortedBedFile = new File(sortedBedPath);
        Assert.isTrue(!sortedBedFile.exists(), getMessage(MessagesConstants.ERROR_FILES_STATUS_ALREADY_EXISTS,
                sortedBedPath));
        Assert.isTrue(sortedBedFile.createNewFile());

        return sortedBedPath;
    }
}