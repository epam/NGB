package com.epam.catgenome.manager.wig.reader;

import com.epam.catgenome.entity.wig.Wig;
import htsjdk.tribble.Feature;

public class BedGraphFeature extends Wig implements Feature {

    public BedGraphFeature(String chr, final Integer startIndex, final Integer endIndex) {
        super(startIndex, endIndex);
        this.chr = chr;
    }

    public BedGraphFeature(String chr, final Integer startIndex, final Integer endIndex, float value) {
        super(startIndex, endIndex, value);
        this.chr = chr;

    }

    private String chr;

    @Override
    public String getChr() {
        return chr;
    }

    @Override
    public String getContig() {
        return chr;
    }

    @Override
    public int getStart() {
        return getStartIndex();
    }

    @Override
    public int getEnd() {
        return getEndIndex();
    }

}
