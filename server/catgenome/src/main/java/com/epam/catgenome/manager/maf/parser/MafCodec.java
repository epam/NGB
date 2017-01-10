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

package com.epam.catgenome.manager.maf.parser;

import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.util.Utils;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.readers.LineIterator;

/**
 * Codec for parsing file with MAF format. Codec decodes {@code MafFeature}
 * from file line.
 */
public class MafCodec extends AsciiFeatureCodec<MafFeature> {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\t|( +)");
    private static final String BIG_MAF_EXTENSION = ".bmaf";
    public static final String MAF_EXTENSION = ".maf";
    private static final String BIG_MAF_COMPRESSED_EXTENSION = ".bmaf.gz";
    public static final String MAF_COMPRESSED_EXTENSION = ".maf.gz";
    private static final int GENE_ID_OFFSET = 1;
    private static final int CENTER_OFFSET = 2;
    private static final int NCBI_BUILD_OFFSET = 3;
    private static final int CHROMOSOME_OFFSET = 4;
    private static final int CHROMOSOME_START = 5;
    private static final int CHROMOSOME_END = 6;
    private static final int FIELDS_OFFSET = 7;

    private boolean isBigMuff = false;
    private String sampleName;
    private int tokensOffset = 0;

    /**
     * Initializes codec for an input file
     * @param path to MAF file
     */
    public MafCodec(String path) {
        super(MafFeature.class);
        if (path.endsWith(BIG_MAF_EXTENSION) || path.endsWith(BIG_MAF_COMPRESSED_EXTENSION)) {
            isBigMuff = true;
            tokensOffset = 1;
        } else {
            if (!(path.endsWith(MAF_EXTENSION) || path.endsWith(MAF_COMPRESSED_EXTENSION))) {
                throw new IllegalArgumentException("Unsupported file type: " + Utils.getFileExtension(path));
            }
            parseFileName(path);
        }
    }

    /**
     * Creates {@code MafFeature} from file line
     * @param line to decode
     * @return parsed {@code MafFeature}
     */
    @Override
    public MafFeature decode(String line) {
        if (line.trim().isEmpty()) {
            return null;
        }
        if (line.startsWith("#") || line.startsWith("Hugo_Symbol")) {
            return null;
        }
        String[] tokens = SPLIT_PATTERN.split(line, -1);
        return decode(tokens);
    }

    /**
     * This implementation doesn't parse header
     * @param reader for a file
     * @return always null
     */
    @Override
    public Object readActualHeader(LineIterator reader) {
        return null;
    }

    /**
     * Codec format support is checked in the constructor
     * @param path to the file
     * @return always true
     */
    @Override
    public boolean canDecode(String path) {
        return true;
    }

    private MafFeature decode(String[] tokens) {
        NggbMafFeature mafFeature = new NggbMafFeature(tokens[CHROMOSOME_OFFSET + tokensOffset],
                Integer.parseInt(tokens[CHROMOSOME_START + tokensOffset]),
                Integer.parseInt(tokens[CHROMOSOME_END + tokensOffset]));
        // if it is a BigMaf file, all columns are moved by tokenOffset == 1
        mafFeature.setHugoSymbol(tokens[tokensOffset]);
        mafFeature.setEntrezGeneId(Integer.parseInt(tokens[GENE_ID_OFFSET + tokensOffset]));
        mafFeature.setCenter(tokens[CENTER_OFFSET + tokensOffset]);
        mafFeature.setNcbiBuild(tokens[NCBI_BUILD_OFFSET + tokensOffset]);
        int offset = FIELDS_OFFSET;
        mafFeature.setStrand(StrandSerializable.forValue(tokens[offset + tokensOffset]));
        offset++;
        mafFeature.setVariantClassification(VariantClassification.forValue(tokens[offset + tokensOffset]));
        offset++;
        mafFeature.setVariationType(VariationType.forMafValue(tokens[offset + tokensOffset]));
        offset++;
        mafFeature.setRef(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setTumorSeqAllele1(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setTumorSeqAllele2(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setDbSnpRS(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setDbSnpValStatus(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setTumorSampleBarcode(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setMatchedNormSampleBarcode(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setMatchNormSeqAllele1(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setMatchNormSeqAllele2(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setTumorValidationAllele1(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setTumorValidationAllele2(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setMatchNormValidationAllele1(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setMatchNormValidationAllele2(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setVerificationStatus(VerificationStatus.forValue(tokens[offset + tokensOffset]));
        offset++;
        mafFeature.setValidationStatus(ValidationStatus.forValue(tokens[offset + tokensOffset]));
        offset++;
        mafFeature.setMutationStatus(MutationStatus.forValue(tokens[offset + tokensOffset]));
        offset++;
        mafFeature.setSequencingPhase(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setSequenceSource(SequenceSource.forValue(tokens[offset + tokensOffset]));
        offset++;
        mafFeature.setValidationMethod(tokens[offset + tokensOffset]);
        offset++;
        offset+=2;
        mafFeature.setSequencer(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setTumorSampleUUID(tokens[offset + tokensOffset]);
        offset++;
        mafFeature.setMatchedNormSampleUUID(tokens[offset + tokensOffset]);
        if (isBigMuff) {
            mafFeature.setSampleName(tokens[0]);
        } else {
            mafFeature.setSampleName(sampleName);
        }
        return mafFeature;
    }
    private void parseFileName(String fileName) {
        sampleName = Utils.removeFileExtension(FilenameUtils.getName(fileName), Utils.getFileExtension(fileName));
    }
}
