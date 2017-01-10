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

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.nucleotid.NibByteFormat;
import com.epam.catgenome.entity.nucleotid.Signature;
import com.epam.catgenome.exception.RegistrationException;
import com.epam.catgenome.util.BlockCompressedDataOutputStream;


/**
 * {@code NibDataWriter} provides a service for writing data into files in the Nib format.
 * It provides methods for writing sequence files and GC data files.
 */
@Service
public class NibDataWriter {

    //for GC-content
    private static final int[] FAST_GC_CONTENT = new int[Constants.GC_CONTENT_STEP + 1];

    //for GC-content
    static {
        FAST_GC_CONTENT[0] = Byte.MIN_VALUE;
        FAST_GC_CONTENT[FAST_GC_CONTENT.length - 1] = Byte.MAX_VALUE;
        double stepContent = (double) (Byte.MAX_VALUE - Byte.MIN_VALUE) / Constants.GC_CONTENT_STEP;
        for (int i = 0; i < Constants.GC_CONTENT_STEP; i++) {
            FAST_GC_CONTENT[i] = (int) Math.round(stepContent * i + Byte.MIN_VALUE);
        }

    }

    /**
     * Writes encoded nucleotide sequence at Nib-format in a created stream
     *
     * @param arrayOfNucleicAcids        {@code byte[]} Array containing the codes of nucleotides(ASCII)
     * @param compressedDataOutputStream {@code BlockCompressedDataOutputStream} Represents a container that
     *                                   provides access to major properties and can be updated by metadata produced as
     *                                   the result of the current call
     * @throws IOException throws in a case if system can't write at this stream
     */
    public void byteArrayToNibFile(final byte[] arrayOfNucleicAcids,
            final BlockCompressedDataOutputStream compressedDataOutputStream)
            throws IOException {
        final int chromosomeSize = arrayOfNucleicAcids.length;
        writeNibHead(compressedDataOutputStream, chromosomeSize);
        //whether there is a remainder of the division by 2? Yes - we need built and write final byte another way
        int i;
        // built each byte and writes
        for (i = 0; i < chromosomeSize - (chromosomeSize % 2); i += 2) {
            try {
                compressedDataOutputStream.writeByte((byte) NibByteFormat
                        .fastaByteFormatToByteNibFormat(arrayOfNucleicAcids[i],
                                arrayOfNucleicAcids[i + 1]));
            } catch (IllegalArgumentException e) {
                throw new RegistrationException(getMessage(MessagesConstants.ERROR_UNKNOWN_FASTA_SYMBOL, i,
                        new String(new byte[]{arrayOfNucleicAcids[i]}, Charset.defaultCharset()),
                        new String(new byte[]{arrayOfNucleicAcids[i + 1]}, Charset.defaultCharset())), e);
            }
        }

        // built final byte and write (if we have the last byte it only one)
        if (i < chromosomeSize) {
            compressedDataOutputStream.writeByte((byte) NibByteFormat.
                    fastaByteFormatToByteNibFormat(arrayOfNucleicAcids[i]));
            i++;
        }
        Assert.isTrue(i >= chromosomeSize, getMessage(MessagesConstants.ERROR_LOGIC_LENGTH));
        //ended and write last data od GC-content
    }

    /**
     * Calculates and writes GC content file for an array of nucleotides
     * @param arrayOfNucleicAcids   {@code byte[]} Array containing the codes of nucleotides(ASCII)
     * @param stream                {@code BlockCompressedDataOutputStream} Represents a container that
     *                               provides access to major properties and can be updated by metadata produced as
     *                               the result of the current call
     * @throws IOException
     */
    public void byteArrayToGCFile(final byte[] arrayOfNucleicAcids,
            BlockCompressedDataOutputStream stream) throws IOException {

        final int chromosomeSize = arrayOfNucleicAcids.length;
        GCContainer containerGC = init4GC(chromosomeSize);
        writeGCHead(chromosomeSize, containerGC, stream);
        for (byte arrayOfNucleicAcid : arrayOfNucleicAcids) {
            addBase2GC(arrayOfNucleicAcid, containerGC, stream);
        }
        //ended and write last data od GC-content
        lastAdd2HeapAndWrite(containerGC, stream);
    }

    private void writeNibHead(final BlockCompressedDataOutputStream blockCompressedDataOutputStream, final int length)
            throws IOException {
        blockCompressedDataOutputStream.writeInt(Signature.NIB_SIGNATURE.getSignature());
        blockCompressedDataOutputStream.writeInt(length);
    }

    // initialization of variables to GC-content
    private GCContainer init4GC(int chromosomeSize) {
        return GCContainer.getContainer(chromosomeSize);
    }

    private void writeGCHead(final int chromosomeSize, GCContainer containerGC,
            BlockCompressedDataOutputStream outStreamGC) throws IOException {
        outStreamGC.writeInt(Signature.GC_CONTENT_SIGNATURE.getSignature());
        outStreamGC.writeInt(chromosomeSize);
        outStreamGC.writeInt(Constants.GC_CONTENT_STEP);
        //because lvlCount for buffer and ignores zero-lvl GC
        outStreamGC.writeInt(containerGC.lvlCount + 1);
        for (int i = 0; i < containerGC.lvlCount + 1; i++) {
            outStreamGC.writeInt(containerGC.lvlSize[i]);
        }
    }

    // watch at next nucleotide and work with them
    private void addBase2GC(final byte nucleotide, GCContainer containerGC,
            BlockCompressedDataOutputStream outStreamGC) throws IOException {
        containerGC.zeroLvlStep++;
        if (isGCCharCode(nucleotide)) {
            containerGC.zeroLvlCount++;
        }
        if (containerGC.zeroLvlStep == Constants.GC_CONTENT_STEP) {
            containerGC.zeroLvlStep = 0;
            outStreamGC.writeByte(FAST_GC_CONTENT[containerGC.zeroLvlCount]);
            if (containerGC.lvlCount > 0) {
                add2Heap(FAST_GC_CONTENT[containerGC.zeroLvlCount], containerGC);
            }
            containerGC.zeroLvlCount = 0;
        }
    }

    private boolean isGCCharCode(final byte nucleotide) {
        return nucleotide == NibByteFormat.NUCLEOTIDE_UPPERCASE_G.getCharCode()
                || nucleotide == NibByteFormat.NUCLEOTIDE_UPPERCASE_C.getCharCode()
                || nucleotide == NibByteFormat.NUCLEOTIDE_LOWERCASE_G.getCharCode()
                || nucleotide == NibByteFormat.NUCLEOTIDE_LOWERCASE_C.getCharCode();
    }

    //GC-content processing
    private void add2Heap(final int gcContent, GCContainer containerGC) {
        containerGC.stepCountGC[0]++;
        containerGC.countGC[0] += gcContent;
        for (int i = 0; i < containerGC.lvlCount; i++) {
            if (containerGC.stepCountGC[i] == Constants.GC_CONTENT_STEP) {
                putInHeap(i, (byte) Math.round((double) containerGC.countGC[i]
                        / (double) Constants.GC_CONTENT_STEP), containerGC);
                if (i + 1 < containerGC.lvlCount) {
                    containerGC.countGC[i + 1] += containerGC.countGC[i] / Constants.GC_CONTENT_STEP;
                    containerGC.stepCountGC[i + 1]++;
                }
                containerGC.countGC[i] = 0;
                containerGC.stepCountGC[i] = 0;
            } else {
                break;
            }
        }
    }

    private void putInHeap(final int lvlNumber, final byte gcContent, GCContainer containerGC) {
        containerGC.buff4Heap[containerGC.navigatorGC[lvlNumber]] = gcContent;
        containerGC.navigatorGC[lvlNumber]++;
    }

    private void lastAdd2HeapAndWrite(GCContainer containerGC,  BlockCompressedDataOutputStream outStreamGC)
            throws IOException {
        if (containerGC.zeroLvlStep != 0) {
            outStreamGC.writeByte(FAST_GC_CONTENT[containerGC.zeroLvlCount]);
            if (containerGC.lvlCount > 0) {
                add2Heap(FAST_GC_CONTENT[containerGC.zeroLvlCount], containerGC);
            }
        }
        if (containerGC.lvlCount > 0) {
            // a small error at the end of the level
            processContainer(containerGC, outStreamGC);
        }
    }

    private void processContainer(GCContainer containerGC, BlockCompressedDataOutputStream outStreamGC)
            throws IOException {
        for (int i = 0; i < containerGC.lvlCount; i++) {
            if (containerGC.stepCountGC[i] != 0) {
                putInHeap(i, (byte) Math.round((double) containerGC.countGC[i]
                        / (double) containerGC.stepCountGC[i]), containerGC);
                if (i + 1 < containerGC.lvlCount) {
                    containerGC.countGC[i + 1] += (byte) Math.round((double) containerGC.countGC[i]
                            / (double) containerGC.stepCountGC[i]);
                    containerGC.stepCountGC[i + 1]++;
                }
            }
        }
        int checkNavigator = 0;
        for (int i = 0; i < containerGC.lvlCount; i++) {
            checkNavigator += containerGC.lvlSize[i + 1];
            if (checkNavigator != containerGC.navigatorGC[i]) {
                Assert.isTrue(checkNavigator == containerGC.navigatorGC[i]);
            }
        }
        outStreamGC.write(containerGC.buff4Heap);
    }

    /**
     * {@code GCContainer} represents a binary block of GC data file
     */
    private static final class GCContainer {

        //Heap of 1,2,3 ect lvl GC
        private byte[] buff4Heap;
        //navigate where to add
        private int[] navigatorGC;
        //number of bases
        private int[] stepCountGC;
        // for sum GC at step's
        private int[] countGC;
        //max number of bases at lvl
        private int[] lvlSize;
        private int lvlCount;
        private int lengthBuff;
        private int zeroLvlCount;
        private int zeroLvlStep;

        private GCContainer() {
            //no-op
        }

        static GCContainer getContainer(int chromosomeSize) {
            GCContainer containerGC = new GCContainer();
            double lengthBuffHelp = chromosomeSize;
            containerGC.lvlSize = new int[Constants.GC_CONTENT_MAX_TREE_LVL_COUNT];
            containerGC.lvlCount = 0;
            containerGC.lengthBuff = 0;
            containerGC.zeroLvlCount = 0;
            containerGC.zeroLvlStep = 0;
            while (lengthBuffHelp >= Constants.GC_CONTENT_MIN_LENGTH) {
                lengthBuffHelp = lengthBuffHelp / Constants.GC_CONTENT_STEP;
                containerGC.lvlSize[containerGC.lvlCount] = (int) (Math.ceil(lengthBuffHelp));
                containerGC.lvlCount++;
            }
            for (int i = 1; i < containerGC.lvlCount; i++) {
                containerGC.lengthBuff += containerGC.lvlSize[i];
            }
            //now it's lvl count for buff4Heap
            containerGC.lvlCount--;
            //arrays for work with buff4Heap
            if (containerGC.lvlCount > 0) {
                containerGC.buff4Heap = new byte[containerGC.lengthBuff];
                containerGC.navigatorGC = new int[containerGC.lvlCount];
                containerGC.countGC = new int[containerGC.lvlCount];
                containerGC.stepCountGC = new int[containerGC.lvlCount];

                for (int i = 1; i < containerGC.lvlCount; i++) {
                    containerGC.navigatorGC[i] = containerGC.navigatorGC[i - 1] + containerGC.lvlSize[i];
                }
            }
            return containerGC;
        }
    }
}
