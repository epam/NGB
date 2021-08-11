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

import com.epam.catgenome.manager.gene.GeneUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Source:      GtfFeature.java
 * Created:     7/12/15, 13:27
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A custom feature class, representing GeneFeature from GTF file
 * </p>
 */
public class GtfFeature extends GffFeature {

    public GtfFeature(String line) {
        super(line);
    }

    public String getTransriptId() {
        return attributes.get(TRANSCRIPT_ID_KEY);
    }

    public String getGroupValue(String attributeKey) {
        return attributes.get(attributeKey);
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    protected String parseGroupId(String line) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        String[] groups = line.substring(line.lastIndexOf('\t') + 1).split(";");
        for (String group : groups) {
            String[] keyValue = group.split("\"");
            if (keyValue.length == 2) {
                attributes.put(keyValue[0].trim(), keyValue[1]);
            }
        }
        return getGeneId();
    }

    @Override
    public String toString() {
        StringBuilder builder =  new StringBuilder().append(seqName)
                .append("\t")
                .append(source)
                .append("\t")
                .append(feature)
                .append("\t")
                .append(start)
                .append("\t")
                .append(end)
                .append("\t")
                .append(Float.compare(score, -1F) != 0 ? score : ".")
                .append("\t")
                .append(strand.getFileValue())
                .append("\t")
                .append(frame != -1 ? frame : ".")
                .append("\t");
        for (Map.Entry<String, String> attribute: attributes.entrySet()) {
            builder.append(attribute.getKey())
                    .append(" \"")
                    .append(attribute.getValue())
                    .append("\"; ");
        }

        return builder.toString();
    }

    @Override
    public String getFeatureName() {
        return GeneUtils.getFeatureName(feature, attributes);
    }
}
