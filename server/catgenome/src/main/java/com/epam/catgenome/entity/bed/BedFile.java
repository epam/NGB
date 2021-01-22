/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.entity.bed;

import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.manager.bed.parser.NggbBedCodec;
import com.epam.catgenome.manager.bed.parser.NggbBedFeature;
import com.epam.catgenome.manager.bed.parser.NggbMultiFormatBedCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import htsjdk.samtools.util.Tuple;
import htsjdk.tribble.AsciiFeatureCodec;
import lombok.SneakyThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a Bed File metadata in the system
 */
public class BedFile extends FeatureFile {

    public static final Map<String, FileExtension> EXTENSION_MAP = readExtensionMap();
    private static final String BED = "bed";

    @SneakyThrows
    private static Map<String, FileExtension> readExtensionMap() {
        List<FileExtension> extensions = new ObjectMapper().readValue(
                BedFile.class.getClassLoader().getResource("conf/catgenome/format/bed/formats.json"),
                new TypeReference<List<FileExtension>>() {
                }
        );
        return extensions.stream().flatMap(e -> e.getExtensions().stream()
                .map(ext -> new Tuple<>(ext, e)))
                .collect(Collectors.toMap(t -> t.a, t -> t.b));
    }

    /**
     * Creates an empty {@code BedFile} record with a required type BED
     */
    public BedFile() {
        setFormat(BiologicalDataItemFormat.BED);
    }

    @Override
    public AclClass getAclClass() {
        return AclClass.BED;
    }

    @Override
    public AsciiFeatureCodec<NggbBedFeature> getCodec() {
        FileExtension extension = EXTENSION_MAP.getOrDefault(getExtension(),
                new FileExtension(Collections.emptyList(), Collections.singletonList(BED)));
        if (extension.getExtensions().contains(BED)) {
            return new NggbBedCodec();
        }
        return new NggbMultiFormatBedCodec(extension.getMapping());
    }

}
