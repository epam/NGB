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

package com.epam.catgenome.manager.gene.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.epam.catgenome.entity.gene.GeneFile;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.readers.LineIterator;


/**
 * Source:      GffCodec.java
 * Created:     7/12/15, 13:27
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A custom HTSJDK codec to work with GFF/GTF file
 * </p>
 */
public class GffCodec extends AsciiFeatureCodec<GeneFeature> {
    private static final String GFF_GZ_EXTENSION = ".gff.gz";
    private static final String GFF3_GZ_EXTENSION = ".gff3.gz";
    private static final String GTF_GZ_EXTENSION = ".gtf.gz";
    private static final String GTF_EXTENSION = ".gtf";
    private static final String GFF_EXTENSION = ".gff";
    private static final String GFF3_EXTENSION = ".gff3";

    private static final List<String> HEADER_MARKERS = Arrays.asList("#", "track", "browser", ">");

    /**
     * An enum, describing Gene file types, that NGB can handle
     */
    public enum GffType {
        GFF(GFF_EXTENSION, GFF3_EXTENSION),
        GTF(GTF_EXTENSION),
        COMPRESSED_GFF(GFF_GZ_EXTENSION, GFF3_GZ_EXTENSION),
        COMPRESSED_GTF(GTF_GZ_EXTENSION);

        private static Map<String, GffType> typeMap = new HashMap<>(6);

        static {
            typeMap.put(GFF_EXTENSION, GFF);
            typeMap.put(GFF3_EXTENSION, GFF);
            typeMap.put(GTF_EXTENSION, GTF);
            typeMap.put(GFF_GZ_EXTENSION, COMPRESSED_GFF);
            typeMap.put(GFF3_GZ_EXTENSION, COMPRESSED_GFF);
            typeMap.put(GTF_GZ_EXTENSION, COMPRESSED_GTF);
        }

        private String[] extensions;

        GffType(String... s) {
            this.extensions = s;
        }

        /**
         * Constructs a GffType by extension string
         * @param ext a file extension string, starting with "."
         * @return correct GffType for specified extension string
         */
        public static GffType forExt(String ext) {
            if (typeMap.containsKey(ext)) {
                return typeMap.get(ext);
            }

            throw new IllegalArgumentException("Unsupported extension " + ext);
        }

        public String[] getExtensions() {
            return extensions;
        }

        public static GffType forGeneFile(Class<? extends GeneFeature> geneFeatureClass, GeneFile geneFile) {
            if (geneFeatureClass == GtfFeature.class) {
                return geneFile.getCompressed() ? GffCodec.GffType.COMPRESSED_GTF : GffCodec.GffType.GTF;
            } else {
                return geneFile.getCompressed() ? GffCodec.GffType.COMPRESSED_GFF : GffCodec.GffType.GFF;
            }
        }
    }

    private final GffType type;

    public GffCodec(GffType type) {
        super(GeneFeature.class);
        this.type = type;
    }

    /**
     * Decodes line form a Gene file (GFF/GTF) into GeneFeature object
     * @param line a gene file line to decode
     * @return a GeneFeature object, representing that line
     */
    @Override
    public GeneFeature decode(String line) {
        if (StringUtils.isBlank(line)) {
            return null;
        }
        //we don't parse headers
        if (HEADER_MARKERS.stream().anyMatch(line::startsWith) || !line.contains("\t")) {
            return null;
        }

        return createFeature(line);
    }

    private GeneFeature createFeature(String line) {
        switch (type) {
            case GFF:
            case COMPRESSED_GFF:
                return new GffFeature(line);
            case GTF:
            case COMPRESSED_GTF:
                return new GtfFeature(line);
            default:
                throw new InternalError("Unknown GENE type: " + type);
        }
    }

    /**
     * We don't parse GFF/GTF headers
     * @param reader a reader for header
     * @return an object of header
     */
    @Override
    public Object readActualHeader(LineIterator reader) {
        return null;
    }

    @Override
    public boolean canDecode(String path) {
        return path.endsWith(".gff") || path.endsWith(".gtf");
    }

}
