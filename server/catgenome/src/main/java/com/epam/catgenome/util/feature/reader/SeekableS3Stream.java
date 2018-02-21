package com.epam.catgenome.util.feature.reader;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.HttpUtils;

import java.io.IOException;
import java.io.InputStream;

public class SeekableS3Stream extends SeekableStream {


    public SeekableS3Stream(String fileName) {
        this.fileName = fileName;
    }

    private long position = 0;

    private long contentLength = -1;
    
    String fileName = null;

    protected volatile S3ObjectInputStream in;


    @Override
    public long length() {
        return contentLength;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public void seek(final long position) {
        this.position = position;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return in.read();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public boolean eof() throws IOException {
        return contentLength > 0 && position >= contentLength;
    }

    @Override
    public String getSource() {
        return fileName;
    }


}
