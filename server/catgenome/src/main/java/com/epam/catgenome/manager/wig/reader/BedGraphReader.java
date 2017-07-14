package com.epam.catgenome.manager.wig.reader;

import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.FeatureReader;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

public class BedGraphReader implements Closeable {

    private FeatureReader<BedGraphFeature> reader;

    public BedGraphReader(String wigFile, String index) {
        reader = AbstractFeatureReader.getFeatureReader(
                wigFile,
                index,
                new BedGraphCodec(),
                true);
    }

    public Iterator<BedGraphFeature> query(String chromosome, int start, int stop) throws IOException {
        return reader.query(chromosome, start, stop);
    }


    @Override
    public void close() throws IOException {
        reader.close();
    }
}
