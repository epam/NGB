package com.epam.catgenome.util.feature.reader;
import htsjdk.samtools.util.RuntimeIOException;
import htsjdk.tribble.readers.LineReader;

import java.io.IOException;

/**
 * @author Jim Robinson
 * @date 2/11/12
 */
public class TabixIteratorLineReader implements LineReader {

    TabixReader.Iterator iterator;


    public TabixIteratorLineReader(TabixReader.Iterator iterator) {
        this.iterator = iterator;
    }

    public String readLine() {
        try {
            return iterator != null ? iterator.next() : null;
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public void close() {
        // Ignore -
    }
}

