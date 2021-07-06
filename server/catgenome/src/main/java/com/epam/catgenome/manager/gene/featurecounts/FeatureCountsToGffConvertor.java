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

package com.epam.catgenome.manager.gene.featurecounts;

import com.epam.catgenome.manager.gene.writer.Gff3Writer;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FeatureCountsToGffConvertor {

    public static void convert(final String featureCountsFilePath, final String gffFilePath) {
        try (Gff3Writer gff3Writer = new Gff3Writer(Paths.get(gffFilePath));
             Reader reader = getReader(featureCountsFilePath)) {
            final LineProcessor<FeatureCountsReaderStatus> processor = new FeatureCountsLineProcessor(gff3Writer);
            CharStreams.readLines(reader, processor);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read data.");
        }
    }

    private static InputStreamReader getReader(final String featureCountsFilePath) throws IOException {
        return new InputStreamReader(Files.newInputStream(Paths.get(featureCountsFilePath)),
                StandardCharsets.UTF_8);
    }
}
