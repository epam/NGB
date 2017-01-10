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

import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import htsjdk.tribble.Feature;

/**
 * Represents an instance parsed from MAF file with fields specified by the MAF file format.
 * This class is an entity returned to the client.
 */
public interface MafFeature extends Feature {
    String getSampleName();
    String getHugoSymbol();
    Integer getEntrezGeneId();
    String getCenter();
    String getNcbiBuild();
    StrandSerializable getStrand();
    VariantClassification getVariantClassification();
    VariationType getVariationType();
    String getRef();
    String getTumorSeqAllele1();
    String getTumorSeqAllele2();
    String getDbSnpRS();
    String getDbSnpValStatus();
    String getTumorSampleBarcode();
    String getMatchedNormSampleBarcode();
    String getMatchNormSeqAllele1();
    String getMatchNormSeqAllele2();
    String getTumorValidationAllele1();
    String getTumorValidationAllele2();
    String getMatchNormValidationAllele1();
    String getMatchNormValidationAllele2();
    VerificationStatus getVerificationStatus();
    ValidationStatus getValidationStatus();
    MutationStatus getMutationStatus();
    String getSequencingPhase();
    SequenceSource getSequenceSource();
    String getValidationMethod();
    Integer getScore();
    String getBamFile();
    String getSequencer();
    String getTumorSampleUUID();
    String getMatchedNormSampleUUID();
    String toBigMafString();
}
