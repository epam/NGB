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
package com.epam.catgenome.manager.export;

import com.epam.catgenome.entity.index.GeneIndexEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum GeneField {
    SOURCE("source", "Source", GeneIndexEntry::getSource),
    SCORE("score", "Score", o -> String.valueOf(o.getScore())),
    STRAND("strand", "Strand", GeneIndexEntry::getStrand),
    FRAME("frame", "Frame", o -> String.valueOf(o.getFrame())),
    FEATURE("feature", "Feature", GeneIndexEntry::getFeature),
    START_INDEX("startIndex", "Position From", o -> String.valueOf(o.getStartIndex())),
    END_INDEX("endIndex", "Position To", o -> String.valueOf(o.getEndIndex())),
    FEATURE_ID("featureId", "Id", o -> String.valueOf(o.getFeatureId())),
    CHROMOSOME("chromosome", "Chr", o -> o.getChromosome().getName()),
    FEATURE_TYPE("featureType", "Type", o -> o.getFeatureType().getFileValue()),
    FEATURE_FILE_ID("featureFileId", "File Id", o -> String.valueOf(o.getFeatureFileId())),
    FEATURE_NAME("featureName", "Name", GeneIndexEntry::getFeatureName);

    private final String field;
    private final String name;
    private final Function<GeneIndexEntry, String> getter;
    private static final Map<String, GeneField> MAP = new HashMap<>();

    static {
        MAP.put(SOURCE.field, SOURCE);
        MAP.put(SCORE.field, SCORE);
        MAP.put(STRAND.field, STRAND);
        MAP.put(FRAME.field, FRAME);
        MAP.put(FEATURE.field, FEATURE);
        MAP.put(START_INDEX.field, START_INDEX);
        MAP.put(END_INDEX.field, END_INDEX);
        MAP.put(FEATURE_ID.field, FEATURE_ID);
        MAP.put(CHROMOSOME.field, CHROMOSOME);
        MAP.put(FEATURE_TYPE.field, FEATURE_TYPE);
        MAP.put(FEATURE_FILE_ID.field, FEATURE_FILE_ID);
        MAP.put(FEATURE_NAME.field, FEATURE_NAME);
    }

    GeneField(final String field, final String name, final Function<GeneIndexEntry, String> getter) {
        this.field = field;
        this.name = name;
        this.getter = getter;
    }

    public static GeneField getByField(String field) {
        return MAP.get(field);
    }

    public Function<GeneIndexEntry, String> getGetter() {
        return getter;
    }

    public String getName() {
        return name;
    }
}
