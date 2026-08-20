/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.manager.reference.io;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.entity.nucleotid.NibByteFormat.LOW_TO_HIGH_NIB_CODE_SHIFT;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.nucleotid.FormatCoder;
import com.epam.catgenome.entity.nucleotid.NibByteFormat;
import com.epam.catgenome.entity.nucleotid.Signature;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.util.BlockCompressedDataInputStream;

/**
 * {@code NibDataReader} provides service for loading reference data from the registered in the
 * system files in the Nib file format
 */
@Service
public class NibDataReader {

    public NibDataReader() {
        // no=op
    }

    /**
     * Loads sequence string from the input stream in the Nib format
     * @param startPosition {@code int} start position at chromosome
     * @param endPosition   {@code int} end position at chromosome
     * @param nibStream     {@code InputStream} InputStream from nib-format file
     * @return {@code String} return String of nucleotides, at nibStream started at startPosition
     * and length sequenceLength
     */
    public String getStringFromNibFile(final int startPosition, final int endPosition,
            BlockCompressedDataInputStream nibStream,
            DataInputStream indexStream)
            throws IOException {
        final int newStartPosition = startPosition - 1;
        final int sequenceLength = endPosition - newStartPosition;
        Assert.notNull(nibStream, getMessage(MessagesConstants.ERROR_NO_SUCH_FILE));
        Assert.isTrue(sequenceLength >= 0, getMessage(MessagesConstants.ERROR_LENGTH_ABOVE_ZERO));
        Assert.isTrue(newStartPosition >= 0, getMessage(MessagesConstants.ERROR_START_POSITION_ABOVE_ZERO));

        long seekPosition = (long) ((double) newStartPosition / 2);
        final int positionFactor = newStartPosition % 2;
        final int realLengthFactor = sequenceLength % 2;

        // 2 nib-format at byte
        final int realLength = sequenceLength / 2 + Math.max(positionFactor, realLengthFactor);
        byte[] buffer = new byte[realLength];
        buffer = readFromNib(nibStream, indexStream, newStartPosition, sequenceLength, seekPosition, buffer);
        return nibByteArrayToString(positionFactor, sequenceLength, buffer);
    }

    /**
     * Loads a byte array with nucleotides byte values from the input stream in the Nib format
     * @param startPosition {@code int} start position at chromosome
     * @param endPosition   {@code int} end position at chromosome
     * @param nibStream     {@code InputStream} InputStream from nib-format file
     * @return {@code String} byte array with of nucleotides, at nibStream started at startPosition
     * and length sequenceLength
     */
    public byte[] getByteNucleotidesFromNibFile(final int startPosition, final int endPosition,
            BlockCompressedDataInputStream nibStream,
            DataInputStream indexStream)
            throws IOException {
        final int newStartPosition = startPosition - 1;
        final int sequenceLength = endPosition - newStartPosition;
        Assert.notNull(nibStream, getMessage(MessagesConstants.ERROR_NO_SUCH_FILE));
        Assert.isTrue(sequenceLength >= 0, getMessage(MessagesConstants.ERROR_LENGTH_ABOVE_ZERO));
        Assert.isTrue(newStartPosition >= 0, getMessage(MessagesConstants.ERROR_START_POSITION_ABOVE_ZERO));

        long seekPosition = (long) ((double) newStartPosition / 2);
        final int positionFactor = newStartPosition % 2;
        final int realLengthFactor = sequenceLength % 2;

        // 2 nib-format at byte
        final int realLength = sequenceLength / 2 + Math.max(positionFactor, realLengthFactor);
        byte[] buffer = new byte[realLength];
        buffer = readFromNib(nibStream, indexStream, newStartPosition, sequenceLength, seekPosition, buffer);
        return nibByteArrayToNucleotideBytes(positionFactor, sequenceLength, buffer);
    }

    /**
     * Loads a {@code List} of reference sequences from the stream (file) in a Nib format
     * @param startPosition {@code int} start position at chromosome
     * @param endPosition   {@code int} end position at chromosome
     * @param nibStream     {@code InputStream} InputStream from nib-format file
     * @return {@code List} return List of nucleotides sequences, at nibStream started at startPosition
     * and length sequenceLength
     */
    public List<Sequence> getNucleotidesFromNibFile(final int startPosition, final int endPosition,
            final BlockCompressedDataInputStream nibStream,
            final DataInputStream indexStream)
            throws IOException {
        //arrays started at zero position, but chromosome started ad first position
        final int newStartPosition = startPosition - 1;
        //sequenceLength it (endPosition) - (old startPosition) + 1 or (endPosition) - (new startPosition)
        final int sequenceLength = endPosition - newStartPosition;
        Assert.notNull(nibStream, getMessage(MessagesConstants.ERROR_NO_SUCH_FILE));
        Assert.isTrue(sequenceLength >= 0, getMessage(MessagesConstants.ERROR_LENGTH_ABOVE_ZERO));
        Assert.isTrue(newStartPosition >= 0, getMessage(MessagesConstants.ERROR_START_POSITION_ABOVE_ZERO));

        long seekPosition = newStartPosition / 2;
        final int positionFactor = newStartPosition % 2;
        final int realLengthFactor = sequenceLength % 2;

        // 2 nib-format at byte
        final int realLength = sequenceLength / 2 + Math.max(positionFactor, realLengthFactor);
        byte[] buffer = new byte[realLength];
        buffer = readFromNib(nibStream, indexStream, newStartPosition, sequenceLength, seekPosition, buffer);
        return NibByteFormat
                .nibByteArrayToNucleotidesList(newStartPosition, positionFactor, sequenceLength, buffer);
    }

    /**
     * Fills a GC-content data from a reference sequence file in the Nib format
     * @param startPosition {@code int} start position at chromosome
     * @param endPosition   {@code int} end position at chromosome
     * @param scaleFactor   track scale in the client
     * @param nibStream     {@code InputStream} InputStream from nib-format file
     * @param indexStream   {@code DataInputStream} InputStream from the reference index file
     * @return {@code List} of sequences filled with GC-content data
     */
    public List<Sequence> fillSequenceOfGCFromNibFile(final int startPosition, final int endPosition,
            final double scaleFactor,
            final BlockCompressedDataInputStream nibStream,
            final DataInputStream indexStream)
            throws IOException {
        Assert.notNull(nibStream, getMessage(MessagesConstants.ERROR_NO_SUCH_FILE));

        List<Sequence> template = createGCList(startPosition, endPosition, scaleFactor);
        //because index in array started at 0
        final int realStartPosition = startPosition;
        final int newStartPosition = startPosition - 1;
        //sequenceLength it (endPosition) - (old startPosition) + 1 or (endPosition) - (new startPosition)
        final int sequenceLength = endPosition - newStartPosition;
        Assert.isTrue(sequenceLength >= 0, getMessage(MessagesConstants.ERROR_LENGTH_ABOVE_ZERO));

        long seekPosition = newStartPosition / 2;
        final int positionFactor = newStartPosition % 2;
        final int reallengthFactor = sequenceLength % 2;

        // 2 nib-format at byte
        final int realLength = sequenceLength / 2 + Math.max(positionFactor, reallengthFactor);
        byte[] buffer = new byte[realLength];
        int nibCode;
        buffer = readFromNib(nibStream, indexStream, newStartPosition, sequenceLength, seekPosition, buffer);

        //index for gcContentArray
        for (Sequence sequence : template) {
            double gcCount = 0;
            for (int j = sequence.getStartIndex(); j <= sequence.getEndIndex(); j++) {
                int index = positionFactor + j - realStartPosition;
                if (index % 2 == 0) {
                    nibCode = buffer[index / 2] & Constants.MASK_HIGH_BYTE;
                    nibCode = nibCode >> LOW_TO_HIGH_NIB_CODE_SHIFT;
                } else {
                    nibCode = buffer[index / 2] & Constants.MASK_LOW_BYTE;
                }
                if (isGCNibCode((byte) nibCode)) {
                    gcCount++;
                }
            }
            sequence.setContentGC((float) (gcCount / (sequence.getEndIndex() - sequence.getStartIndex() + 1)));
        }
        return template;
    }

    /**
     * Fills a GC-content data from a GC file
     * @param startPosition {@code int} start position at chromosome
     * @param endPosition   {@code int} end position at chromosome
     * @param scaleFactor   track scale in the client
     * @param gcContentStream     {@code InputStream} InputStream from GC-format file
     * @param indexStream   {@code DataInputStream} InputStream from the reference index file
     * @return {@code List} of sequences filled with GC-content data
     */

    public List<Sequence> fillSequenceOfGCFromGCFile(final int startPosition, final int endPosition,
            final double scaleFactor,
            final BlockCompressedDataInputStream gcContentStream,
            final DataInputStream indexStream)
            throws IOException {

        //step in file very important
        ReadGC stepGCInArray = new ReadGC();
        stepGCInArray.gcArrayStep = 0;
        final int sequenceLength = endPosition - startPosition;
        Assert.isTrue(scaleFactor <= 1.0 / Constants.GC_CONTENT_STEP,
                getMessage(MessagesConstants.ERROR_INVALID_PARAM));
        Assert.notNull(gcContentStream, getMessage(MessagesConstants.ERROR_NO_SUCH_FILE));
        Assert.isTrue(sequenceLength >= 0, getMessage(MessagesConstants.ERROR_LENGTH_ABOVE_ZERO));
        Assert.isTrue(startPosition >= 1, getMessage(MessagesConstants.ERROR_START_POSITION_ABOVE_ZERO));
        List<Sequence> template = createGCList(startPosition, endPosition, scaleFactor);
        //because index in array started at 0
        final int newStartPosition = startPosition - 1;
        final byte[] gcContentArray = getGCContentArray(newStartPosition, endPosition, scaleFactor, gcContentStream,
                indexStream, stepGCInArray);

        //fill template
        Iterator<Sequence> iterator = template.iterator();

        //index for gcContentArray
        int i = 0;
        int helpStartIndexInArray = (newStartPosition / stepGCInArray.gcArrayStep) * stepGCInArray.gcArrayStep;
        while (iterator.hasNext()) {
            Sequence sequence = iterator.next();

            //because index in array started at 0
            int helpStartPosition = sequence.getStartIndex() - 1;

            if (helpStartIndexInArray + stepGCInArray.gcArrayStep >= helpStartPosition &&
                    helpStartIndexInArray > helpStartPosition) {
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_START_POSITION));
            }

            float gcContent = 0;
            final int helpEndIndex = sequence.getEndIndex() - 1;
            while (helpStartPosition < helpEndIndex && i < gcContentArray.length) {
                //end position in array + 1 (it's not difficult to understand, why we don't subtract 1)
                helpStartIndexInArray += stepGCInArray.gcArrayStep;

                /**
                 * increasing results of gc-content by component
                 *(gcContentArray[i] - Byte.MIN_VALUE) / (Byte.MAX_VALUE - Byte.MIN_VALUE) - encoding
                 * component gc-content at array
                 * (helpStartIndexInArray - helpStartPosition) - number of databases that are included in computing
                 * (helpEndIndex - sequence.getStartIndex() + 1)  - the total number of bases
                 *
                 * helpEndIndex included in the sequence so we need plus 1
                 * helpStartIndexInArray not included in the sequence so we not need plus 1
                 *
                 **/
                final double gcContentInArray = encodingGCContent((float) gcContentArray[i]);
                if (helpStartIndexInArray - 1 > helpEndIndex) {
                    gcContent += (gcContentInArray) * (helpEndIndex - helpStartPosition + 1)
                            / (helpEndIndex - sequence.getStartIndex() + 2);
                    helpStartPosition = helpStartIndexInArray;
                    helpStartIndexInArray -= stepGCInArray.gcArrayStep;
                } else {
                    gcContent += (gcContentInArray) * (helpStartIndexInArray - helpStartPosition)
                            / (helpEndIndex - sequence.getStartIndex() + 2);
                    i++;
                    helpStartPosition = helpStartIndexInArray;
                }
            }
            sequence.setContentGC(gcContent);
        }
        return template;
    }


    public List<Sequence> fillSequenceOfGCFromFasta(final Integer startPosition, final Integer endPosition,
            final Double scaleFactor, final String sequence)
            throws IOException {
        List<Sequence> template = createGCList(startPosition, endPosition, scaleFactor);
        final int sequenceLength = endPosition - startPosition;
        Assert.isTrue(sequenceLength >= 0, getMessage(MessagesConstants.ERROR_LENGTH_ABOVE_ZERO));
        setContentGc(startPosition, endPosition, template, sequence);
        return template;
    }

    private void setContentGc(Integer startPosition, Integer endPosition, List<Sequence> template,
            String sequenceText) {
        int index = 0;
        for (Sequence sequence : template) {
            double gcCount = 0;
            for (int j = sequence.getStartIndex(); j <= sequence.getEndIndex(); j++) {
                if (index < endPosition - startPosition - 1) {
                    index++;
                }
                char ch = sequenceText.charAt(index);
                if (ch == NibByteFormat.NUCLEOTIDE_LOWERCASE_C.getCharCode()
                        || ch == NibByteFormat.NUCLEOTIDE_LOWERCASE_G.getCharCode()
                        || ch == NibByteFormat.NUCLEOTIDE_UPPERCASE_C.getCharCode()
                        || ch == NibByteFormat.NUCLEOTIDE_UPPERCASE_G.getCharCode()) {
                    gcCount++;
                }
            }
            sequence.setContentGC((float) (gcCount / (sequence.getEndIndex() - sequence.getStartIndex() + 1)));
        }
    }


    private byte[] readFromNib(final BlockCompressedDataInputStream nibStream, final DataInputStream indexStream,
            final int startPosition, final int sequenceLength, long seekPosition,
            byte[] buffer) throws IOException {

        int signature = nibStream.readInt();
        int lengthNibSequence = nibStream.readInt();
        Assert.isTrue(signature == Signature.NIB_SIGNATURE.getSignature(),
                getMessage(MessagesConstants.ERROR_WRONG_SIGNATURE));
        Assert.isTrue(lengthNibSequence >= startPosition + sequenceLength,
                getMessage(MessagesConstants.ERROR_LOGIC_LENGTH));
        seekBCDISWithIndexFile(nibStream, indexStream, seekPosition);

        readInBuff(buffer, nibStream);

        IOUtils.closeQuietly(nibStream);
        IOUtils.closeQuietly(indexStream);
        return buffer;
    }

    private void seekBCDISWithIndexFile(final BlockCompressedDataInputStream stream, final DataInputStream index,
            final long positon) throws IOException {
        //correct seek in file
        long seekPosition = positon + stream.getPosition();
        long filePosition = index.readLong();
        long seekPos = index.readLong();
        long helpSeekPosition = seekPosition - seekPos;
        while (helpSeekPosition >= 0) {
            seekPosition = helpSeekPosition;
            filePosition = index.readLong();
            seekPos = index.readLong();
            helpSeekPosition = seekPosition - seekPos;
        }
        stream.seek(filePosition + seekPosition);
    }

    private void readInBuff(final byte[] buffer, final BlockCompressedDataInputStream dataInputStream)
            throws IOException {
        int realReadLength = 0;
        while (dataInputStream.available() > 0 && realReadLength < buffer.length) {
            realReadLength += dataInputStream.read(buffer, realReadLength, buffer.length - realReadLength);
        }
        Assert.isTrue(buffer.length == realReadLength, getMessage(MessagesConstants.ERROR_READ_FILE));
    }

    private boolean isGCNibCode(final byte nibCode) {
        return nibCode == NibByteFormat.NUCLEOTIDE_UPPERCASE_G.getByteCode()
                || nibCode == NibByteFormat.NUCLEOTIDE_UPPERCASE_C.getByteCode()
                || nibCode == NibByteFormat.NUCLEOTIDE_LOWERCASE_G.getByteCode()
                || nibCode == NibByteFormat.NUCLEOTIDE_LOWERCASE_C.getByteCode();
    }

    private String nibByteArrayToString(final int startPosition,
            final int sequenceLength, final byte[] nibArray) {
        Assert.notNull(nibArray, getMessage(MessagesConstants.ERROR_NO_SUCH_FILE));
        Assert.isTrue(sequenceLength <= nibArray.length * 2, getMessage(MessagesConstants.ERROR_ARRAY_SO_SMALL));
        int nibCode;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < sequenceLength; i++) {
            if ((startPosition + i) % 2 == 0) {
                nibCode = nibArray[(startPosition + i) >>> 1] & Constants.MASK_HIGH_BYTE;
                nibCode = nibCode >> LOW_TO_HIGH_NIB_CODE_SHIFT;
            } else {
                nibCode = nibArray[(startPosition + i) >>> 1] & Constants.MASK_LOW_BYTE;
            }
            // i + absolutePosition + 1 - because at chromosome started at first index but ib array started from zero
            result.append(FormatCoder.nibByteToString(nibCode));
        }
        return result.toString();
    }

    private byte[] nibByteArrayToNucleotideBytes(final int startPosition,
            final int sequenceLength, final byte[] nibArray) {
        Assert.notNull(nibArray, getMessage(MessagesConstants.ERROR_NO_SUCH_FILE));
        Assert.isTrue(sequenceLength <= nibArray.length * 2, getMessage(MessagesConstants.ERROR_ARRAY_SO_SMALL));
        int nibCode;
        byte[] nucleotides = new byte[sequenceLength];
        for (int i = 0; i < sequenceLength; i++) {
            if ((startPosition + i) % 2 == 0) {
                nibCode = nibArray[(startPosition + i) >>> 1] & Constants.MASK_HIGH_BYTE;
                nibCode = nibCode >> LOW_TO_HIGH_NIB_CODE_SHIFT;
            } else {
                nibCode = nibArray[(startPosition + i) >>> 1] & Constants.MASK_LOW_BYTE;
            }
            // i + absolutePosition + 1 - because at chromosome started at first index but ib array started from zero
            nucleotides[i] = NibByteFormat.nibCodeToByteNucleotide(nibCode);
        }
        return nucleotides;
    }

    public List<Sequence> createGCList(int startPosition, final int endPosition, final double scaleFactor) {
        List<Sequence> result = new ArrayList<>();
        final double step = 1 / scaleFactor;
        final int sizeTrack = (int) Math.ceil((endPosition - startPosition + 1) / step);
        int startPositionHelper = startPosition;
        //added a block and calculation start position and end position for each
        for (int i = 1; i < sizeTrack; i++) {
            final int help = startPosition + (int) Math.round(step * i) - 1;
            Sequence sequence = new Sequence(startPositionHelper, help, 0F);
            startPositionHelper = help + 1;
            result.add(sequence);
        }
        //last block at sequence
        Sequence sequence = new Sequence(startPositionHelper, endPosition, 0F);
        result.add(sequence);
        return result;
    }

    private byte[] getGCContentArray(final int startPosition, final int endPosition, final double scaleFactor,
            final BlockCompressedDataInputStream gcContentStream,
            final DataInputStream indexStream, ReadGC stepGCInArray)
            throws IOException {
        byte[] gcContentArray;
        final int signatureGC = gcContentStream.readInt();
        Assert.isTrue(signatureGC == Signature.GC_CONTENT_SIGNATURE.getSignature(),
                getMessage(MessagesConstants.ERROR_WRONG_SIGNATURE));
        final int sizeGCContent = gcContentStream.readInt();
        Assert.isTrue(sizeGCContent >= endPosition, getMessage(MessagesConstants.ERROR_LOGIC_LENGTH));
        final int stepInFile = gcContentStream.readInt();
        Assert.isTrue(stepInFile > 0, getMessage(MessagesConstants.ERROR_LOGIC_LENGTH));
        final int gcLvlCount = gcContentStream.readInt();
        Assert.isTrue(gcLvlCount > 0, getMessage(MessagesConstants.ERROR_LOGIC_LENGTH));
        final int[] gcLvlSize = new int[gcLvlCount];
        for (int i = 0; i < gcLvlSize.length; i++) {
            gcLvlSize[i] = gcContentStream.readInt();
        }
        final long stepInQuery = Math.round(1 / scaleFactor);
        stepGCInArray.gcArrayStep = stepInFile * stepInFile;
        Assert.isTrue(stepGCInArray.gcArrayStep >= 1, getMessage(MessagesConstants.ERROR_INVALID_PARAM));
        //shows what lvl to take
        int whatLvlWeNeed = 0;
        while (stepGCInArray.gcArrayStep <= stepInQuery) {
            whatLvlWeNeed++;
            stepGCInArray.gcArrayStep *= stepInFile;
        }
        stepGCInArray.gcArrayStep = stepGCInArray.gcArrayStep / stepInFile;
        if (whatLvlWeNeed >= gcLvlCount) {
            whatLvlWeNeed = gcLvlCount - 1;
            stepGCInArray.gcArrayStep = stepInFile * (int) Math.round(Math.pow(stepInFile, whatLvlWeNeed));
        }
        int seekPosition = startPosition / stepGCInArray.gcArrayStep;
        for (int i = 0; i < whatLvlWeNeed; i++) {
            seekPosition += gcLvlSize[i];
        }
        seekBCDISWithIndexFile(gcContentStream, indexStream, seekPosition);


        final int queryLength = (int) Math.ceil((endPosition - startPosition) /
                (Math.pow((double) stepInFile, (double) whatLvlWeNeed + (double) 1)));
        gcContentArray = new byte[queryLength];
        readInBuff(gcContentArray, gcContentStream);
        IOUtils.closeQuietly(indexStream);
        IOUtils.closeQuietly(gcContentStream);
        return gcContentArray;
    }

    private double encodingGCContent(final double gcCode) {
        return (gcCode - Byte.MIN_VALUE) / (Byte.MAX_VALUE - Byte.MIN_VALUE);
    }

    private static class ReadGC {
        private int gcArrayStep = 0;
    }
}
