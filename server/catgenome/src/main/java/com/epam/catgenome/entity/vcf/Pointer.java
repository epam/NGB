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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

/**
 * Helper class to represent a pointer in Lucene search results
 */
public class Pointer {
    /** The score of this document for the query. */
    private float score;

    /** A hit document's number.
     * @see IndexSearcher#doc(int) */
    private int doc;

    /** Only set by {@link TopDocs#merge} */
    private int shardIndex;

    private List<FieldsRef> fields;

    public Pointer() {
        // no op
    }

    public Pointer(float score, int doc, int shardIndex) {
        this.score = score;
        this.doc = doc;
        this.shardIndex = shardIndex;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public int getDoc() {
        return doc;
    }

    public void setDoc(int doc) {
        this.doc = doc;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public void setShardIndex(int shardIndex) {
        this.shardIndex = shardIndex;
    }

    public List<FieldsRef> getFields() {
        return fields;
    }

    public void setFields(List<FieldsRef> fields) {
        this.fields = fields;
    }

    public ScoreDoc toScoreDoc() {
        if (CollectionUtils.isEmpty(fields)) {
            return new ScoreDoc(doc, score, shardIndex);
        } else {
            BytesRef[] docs = fields.stream()
                    .map(FieldsRef::toByteRef)
                    .toArray(BytesRef[]::new);
            return new FieldDoc(doc, score, docs, shardIndex);
        }
    }

    public static Pointer fromScoreDoc(ScoreDoc doc) {
        if (doc == null) {
            return null;
        }
        if (doc instanceof FieldDoc) {
            FieldDoc fieldDoc = (FieldDoc)doc;
            Pointer pointer = new Pointer(doc.score, doc.doc, doc.shardIndex);
            if (fieldDoc.fields.length != 0) {
                List<FieldsRef> refs = new ArrayList<>();
                for (Object field : fieldDoc.fields) {
                    if (field instanceof BytesRef) {
                        BytesRef bytesRef = (BytesRef)field;
                        FieldsRef ref = FieldsRef.fromBytesRef(bytesRef);
                        refs.add(ref);
                    }
                }
                pointer.setFields(refs);
            }
            return pointer;
        } else {
            return new Pointer(doc.score, doc.doc, doc.shardIndex);
        }
    }

    public static class FieldsRef {
        private String bytes;
        private int offset;
        private int length;
        private boolean valid;

        public FieldsRef() {
            //no op
        }

        public String getBytes() {
            return bytes;
        }

        public void setBytes(String bytes) {
            this.bytes = bytes;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public BytesRef toByteRef() {
            return new BytesRef(bytes);
        }

        public static FieldsRef fromBytesRef(BytesRef bytesRef) {
            FieldsRef ref = new FieldsRef();
            ref.setOffset(bytesRef.offset);
            ref.setLength(bytesRef.length);
            ref.setValid(bytesRef.isValid());
            ref.setBytes(bytesRef.utf8ToString());
            return ref;
        }
    }
}
