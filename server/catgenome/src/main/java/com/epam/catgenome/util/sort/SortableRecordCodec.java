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

package com.epam.catgenome.util.sort;

import com.epam.catgenome.exception.SortingException;
import htsjdk.samtools.util.SortingCollection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class SortableRecordCodec implements SortingCollection.Codec<SortableRecord> {

    private static final String UTF_8 = "utf-8";

    private static final int TREE_BYTES_OFFSET = 24;
    private static final int TWO_BYTES_OFFSET = 16;
    private static final int ONE_BYTES_OFFSET = 8;

    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = new DataOutputStream(outputStream);
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = new DataInputStream(inputStream);
    }

    @Override
    public void encode(SortableRecord record) {
        try {
            outputStream.writeInt(record.getStart());
            outputStream.writeUTF(record.getChromosome());

            String s = record.getText();
            byte[] textBytes = s.getBytes(UTF_8);
            outputStream.writeInt(textBytes.length);
            outputStream.write(textBytes, 0, textBytes.length);
        } catch (IOException ex) {
            throw new SortingException(ex);
        }
    }

    @Override
    public SortableRecord decode() {
        try {
            int start = readAndCheckInt();

            if (start == -1) {
                return null;
            }

            String chr = inputStream.readUTF();
            int textLen = inputStream.readInt();

            byte[] textBytes = new byte[textLen];
            inputStream.readFully(textBytes);
            String text = new String(textBytes, UTF_8);

            return new SortableRecord(chr, start, text);
        } catch (IOException ex) {
            throw new SortingException(ex);
        }
    }

    private int readAndCheckInt() throws IOException {
        int ch1 = inputStream.read();
        int ch2 = inputStream.read();
        int ch3 = inputStream.read();
        int ch4 = inputStream.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            return -1;
        }
        return (ch1 << TREE_BYTES_OFFSET) + (ch2 << TWO_BYTES_OFFSET) + (ch3 << ONE_BYTES_OFFSET) + ch4;
    }

    @Override
    public SortingCollection.Codec<SortableRecord> clone() {
        try {
            return (SortingCollection.Codec<SortableRecord>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new SortingException(e);
        }
    }
}
