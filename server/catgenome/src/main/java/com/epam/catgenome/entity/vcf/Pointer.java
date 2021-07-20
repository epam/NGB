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

package com.epam.catgenome.entity.vcf;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

/**
 * Helper class to represent a pointer in Lucene search results
 */
@Data
@NoArgsConstructor
@Slf4j
public class Pointer {
    /** The score of this document for the query. */
    private float score;

    /** A hit document's number.
     * @see IndexSearcher#doc(int) */
    private int doc;

    /** Only set by {@link TopDocs#merge} */
    private int shardIndex;

    private List<FieldRef> fields;

    public Pointer(float score, int doc, int shardIndex) {
        this.score = score;
        this.doc = doc;
        this.shardIndex = shardIndex;
    }

    public ScoreDoc toScoreDoc() {
        if (CollectionUtils.isEmpty(fields)) {
            return new ScoreDoc(doc, score, shardIndex);
        } else {
            Object[] docs = fields.stream()
                    .map(FieldRef::toField)
                    .toArray();
            return new FieldDoc(doc, score, docs, shardIndex);
        }
    }

    public static Pointer fromScoreDoc(final ScoreDoc doc) {
        if (doc == null) {
            return null;
        }
        if (doc instanceof FieldDoc) {
            FieldDoc fieldDoc = (FieldDoc)doc;
            Pointer pointer = new Pointer(doc.score, doc.doc, doc.shardIndex);
            if (fieldDoc.fields.length != 0) {
                pointer.setFields(
                        Arrays.stream(fieldDoc.fields).map(FieldRef::fromField).collect(Collectors.toList()));
            }
            return pointer;
        } else {
            return new Pointer(doc.score, doc.doc, doc.shardIndex);
        }
    }

    @Value
    @Builder
    public static class FieldRef {

        Object ref;
        FieldRefType type;

        public Object toField() {
            switch (type) {
                case BYTES_REF:
                    return new BytesRef((String) ref);
                case INTEGER:
                    return Integer.parseInt(ref.toString());
                case DOUBLE:
                    return Double.parseDouble(ref.toString());
                case FLOAT:
                    return Float.parseFloat(ref.toString());
                case LONG:
                    return Long.parseLong(ref.toString());
                default:
                    log.debug("Cannot determine field type");
                    return ref;
            }
        }

        public static FieldRef fromField(final Object ref) {
            if (ref instanceof BytesRef) {
                return FieldRef.builder().type(FieldRefType.BYTES_REF)
                        .ref(((BytesRef)ref).utf8ToString()).build();

            }
            return FieldRef.builder()
                    .type(getRefType(ref))
                    .ref(ref)
                    .build();
        }

        private static FieldRefType getRefType(final Object ref) {
            if (ref instanceof Integer) {
                return FieldRefType.INTEGER;
            }
            if (ref instanceof Double) {
                return FieldRefType.DOUBLE;
            }
            if (ref instanceof Float) {
                return FieldRefType.FLOAT;
            }
            if (ref instanceof Long) {
                return FieldRefType.LONG;
            }
            log.debug("Failed to determine field type");
            return FieldRefType.PLAIN;
        }

        public enum FieldRefType {
            BYTES_REF, PLAIN, INTEGER, DOUBLE, FLOAT, LONG
        }
    }

}
