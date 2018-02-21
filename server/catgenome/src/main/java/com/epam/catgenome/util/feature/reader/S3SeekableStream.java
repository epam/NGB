package com.epam.catgenome.util.feature.reader;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.IOException;

public class S3SeekableStream extends SeekableStream {


    public S3SeekableStream(String fileName) {
        this.fileName = fileName;
    }
    
    String fileName = null;

    S3ObjectInputStream s3ObjectInputStream = null;

    @Override
    public long length() {
        return 0;
    }

    @Override
    public long position() throws IOException {
        return 0;
    }

    @Override
    public void seek(long position) throws IOException {

    }

    @Override
    public int read() throws IOException {
        return 0;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean eof() throws IOException {
        return false;
    }

    @Override
    public String getSource() {
        return null;
    }


}
