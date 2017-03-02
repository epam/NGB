package com.epam.catgenome.manager.reference.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import org.apache.log4j.Logger;


public class FastaSequenceFile {
    private static final Logger LOG = Logger.getLogger(FastaSequenceFile.class);

    private final FastaIndex index;
    private final String path;
    private final long contentLength;

    public FastaSequenceFile(String path, String indexPath) throws IOException {
        this.path = path;
        contentLength = FastaUtils.getContentLength(path);
        index = new FastaIndex(indexPath);
    }

    public Set<String> getChromosomeNames() {
        return index.getSequenceNames();
    }

    public int getSequenceSize(String name) {
        return index.getSequenceSize(name);
    }

    public byte[] getChromosome(String chr) {
        return getSequence(chr, 0, index.getSequenceSize(chr));
    }

    public byte[] getSequence(String chr, int startIndex, int qend) {
        int qstart = startIndex;
        qstart--;
        FastaIndex.FastaSequenceIndexEntry idxEntry = index.getIndexEntry(chr);
        if (idxEntry == null) {
            return new byte[0];
        }
        try {
            final int start = Math.max(0, qstart);    // qstart should never be < 0
            final int end = Math.min((int) idxEntry.getSize(), qend);

            final int bytesPerLine = idxEntry.getBytesPerLine();
            final int basesPerLine = idxEntry.getBasesPerLine();
            int nEndBytes = bytesPerLine - basesPerLine;

            int startLine = start / basesPerLine;
            int endLine = end / basesPerLine;

            int base0 = startLine * basesPerLine;   // Base at beginning of start line

            int offset = start - base0;
            final long position = idxEntry.getPosition();
            long startByte = position + startLine * bytesPerLine + offset;

            int base1 = endLine * basesPerLine;
            int offset1 = end - base1;
            long endByte = Math.min(contentLength, position + endLine * bytesPerLine + offset1);

            if (startByte >= endByte) {
                return new byte[0];
            }

            // Read all the bytes in the range.  This will include endline characters
            byte[] allBytes = readBytes(startByte, endByte);

            // Create the array for the sequence -- this will be "allBytes" without the endline characters.
            ByteArrayOutputStream bos = new ByteArrayOutputStream(end - start);

            int srcPos = 0;
            // Copy first line
            final int allBytesLength = allBytes.length;
            if (offset > 0) {
                int nBases = Math.min(end - start, basesPerLine - offset);
                bos.write(allBytes, srcPos, nBases);
                srcPos += (nBases + nEndBytes);
            }
            while (srcPos < allBytesLength) {
                int nBases = Math.min(basesPerLine, allBytesLength - srcPos);
                bos.write(allBytes, srcPos, nBases);
                srcPos += (nBases + nEndBytes);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return new byte[0];
        }
    }

    /**
     * Read the bytes between file position posStart and posEnd
     */
    private byte[] readBytes(long posStart, long posEnd) throws IOException {

        SeekableStream ss = null;
        try {
            ss = SeekableStreamFactory.getInstance().getStreamFor(path);
            int nBytes = (int) (posEnd - posStart);
            byte[] bytes = new byte[nBytes];
            ss.seek(posStart);
            ss.readFully(bytes);
            return bytes;
        } finally {
            if (ss != null) {
                ss.close();
            }
        }
    }

}
