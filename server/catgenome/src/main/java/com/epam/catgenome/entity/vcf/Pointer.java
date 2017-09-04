package com.epam.catgenome.entity.vcf;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 * Helper class to represent a pointer in Lucene search results
 */
public class Pointer {
    /** The score of this document for the query. */
    public float score;

    /** A hit document's number.
     * @see IndexSearcher#doc(int) */
    public int doc;

    /** Only set by {@link TopDocs#merge} */
    public int shardIndex;

    public Pointer() {
        // no op
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

    public ScoreDoc toScoreDoc() {
        return new ScoreDoc(doc, score, shardIndex);
    }
}
