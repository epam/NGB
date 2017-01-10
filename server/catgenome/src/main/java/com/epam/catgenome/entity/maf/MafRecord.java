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

package com.epam.catgenome.entity.maf;

import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.maf.parser.MafFeature;
import com.epam.catgenome.manager.maf.parser.MutationStatus;
import com.epam.catgenome.manager.maf.parser.SequenceSource;
import com.epam.catgenome.manager.maf.parser.ValidationStatus;
import com.epam.catgenome.manager.maf.parser.VariantClassification;
import com.epam.catgenome.manager.maf.parser.VerificationStatus;

/**
 * Represents a record from the MAF file and holds all its fields according to the
 * MAF format specification
 */
public class MafRecord extends Block {
    private String sampleName;
    private String hugoSymbol;
    private int entrezGeneId;
    private String center;
    private String ncbiBuild;
    private String chr;
    private StrandSerializable strand;
    private VariantClassification variantClassification;
    private VariationType variationType;
    private String ref;
    private String tumorSeqAllele1;
    private String tumorSeqAllele2;
    private String dbSnpRS;
    private String dbSnpValStatus;
    private String tumorSampleBarcode;
    private String matchedNormSampleBarcode;
    private String matchNormSeqAllele1;
    private String matchNormSeqAllele2;
    private String tumorValidationAllele1;
    private String tumorValidationAllele2;
    private String matchNormValidationAllele1;
    private String matchNormValidationAllele2;
    private VerificationStatus verificationStatus;
    private ValidationStatus validationStatus;
    private MutationStatus mutationStatus;
    private String sequencingPhase;
    private SequenceSource sequenceSource;
    private String validationMethod;
    private Integer score;
    private String bamFile;
    private String sequencer;
    private String tumorSampleUUID;
    private String matchedNormSampleUUID;

    public MafRecord() {
        // no-op
    }

    /**
     * Creates {@code MafRecord} from a {@code MafFeature}, parsed from the original
     * MAF file
     * @param mafFeature to convert into @code MafRecord}
     */
    public MafRecord(final MafFeature mafFeature) {
        this.sampleName = mafFeature.getSampleName();
        this.hugoSymbol = mafFeature.getHugoSymbol();
        this.entrezGeneId = mafFeature.getEntrezGeneId();
        this.center = mafFeature.getCenter();
        this.ncbiBuild = mafFeature.getNcbiBuild();
        this.chr = mafFeature.getContig();
        this.setStartIndex(mafFeature.getStart());
        this.setEndIndex(mafFeature.getEnd());
        this.strand = mafFeature.getStrand();
        this.variantClassification = mafFeature.getVariantClassification();
        this.variationType = mafFeature.getVariationType();
        this.ref = mafFeature.getRef();
        this.tumorSeqAllele1 = mafFeature.getTumorSeqAllele1();
        this.tumorSeqAllele2 = mafFeature.getTumorSeqAllele2();
        this.dbSnpRS = mafFeature.getDbSnpRS();
        this.dbSnpValStatus = mafFeature.getDbSnpValStatus();
        this.tumorSampleBarcode = mafFeature.getTumorSampleBarcode();
        this.matchedNormSampleBarcode = mafFeature.getMatchedNormSampleBarcode();
        this.matchNormSeqAllele1 = mafFeature.getMatchNormSeqAllele1();
        this.matchNormSeqAllele2 = mafFeature.getMatchNormSeqAllele2();
        this.tumorValidationAllele1 = mafFeature.getTumorValidationAllele1();
        this.tumorValidationAllele2 = mafFeature.getTumorValidationAllele2();
        this.matchNormValidationAllele1 = mafFeature.getMatchNormValidationAllele1();
        this.matchNormValidationAllele2 = mafFeature.getMatchNormValidationAllele2();
        this.verificationStatus = mafFeature.getVerificationStatus();
        this.validationStatus = mafFeature.getValidationStatus();
        this.mutationStatus = mafFeature.getMutationStatus();
        this.sequencingPhase = mafFeature.getSequencingPhase();
        this.sequenceSource = mafFeature.getSequenceSource();
        this.validationMethod = mafFeature.getValidationMethod();
        this.score = mafFeature.getScore();
        this.bamFile = mafFeature.getBamFile();
        this.sequencer = mafFeature.getSequencer();
        this.tumorSampleUUID = mafFeature.getTumorSampleUUID();
        this.matchedNormSampleUUID = mafFeature.getMatchedNormSampleUUID();
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getHugoSymbol() {
        return hugoSymbol;
    }

    public void setHugoSymbol(String hugoSymbol) {
        this.hugoSymbol = hugoSymbol;
    }

    public int getEntrezGeneId() {
        return entrezGeneId;
    }

    public void setEntrezGeneId(int entrezGeneId) {
        this.entrezGeneId = entrezGeneId;
    }

    public String getCenter() {
        return center;
    }

    public void setCenter(String center) {
        this.center = center;
    }

    public String getNcbiBuild() {
        return ncbiBuild;
    }

    public void setNcbiBuild(String ncbiBuild) {
        this.ncbiBuild = ncbiBuild;
    }

    public String getChr() {
        return chr;
    }

    public void setChr(String chr) {
        this.chr = chr;
    }

    public StrandSerializable getStrand() {
        return strand;
    }

    public void setStrand(StrandSerializable strand) {
        this.strand = strand;
    }

    public VariantClassification getVariantClassification() {
        return variantClassification;
    }

    public void setVariantClassification(VariantClassification variantClassification) {
        this.variantClassification = variantClassification;
    }

    public VariationType getVariationType() {
        return variationType;
    }

    public void setVariationType(VariationType variationType) {
        this.variationType = variationType;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getTumorSeqAllele1() {
        return tumorSeqAllele1;
    }

    public void setTumorSeqAllele1(String tumorSeqAllele1) {
        this.tumorSeqAllele1 = tumorSeqAllele1;
    }

    public String getTumorSeqAllele2() {
        return tumorSeqAllele2;
    }

    public void setTumorSeqAllele2(String tumorSeqAllele2) {
        this.tumorSeqAllele2 = tumorSeqAllele2;
    }

    public String getDbSnpRS() {
        return dbSnpRS;
    }

    public void setDbSnpRS(String dbSnpRS) {
        this.dbSnpRS = dbSnpRS;
    }

    public String getDbSnpValStatus() {
        return dbSnpValStatus;
    }

    public void setDbSnpValStatus(String dbSnpValStatus) {
        this.dbSnpValStatus = dbSnpValStatus;
    }

    public String getTumorSampleBarcode() {
        return tumorSampleBarcode;
    }

    public void setTumorSampleBarcode(String tumorSampleBarcode) {
        this.tumorSampleBarcode = tumorSampleBarcode;
    }

    public String getMatchedNormSampleBarcode() {
        return matchedNormSampleBarcode;
    }

    public void setMatchedNormSampleBarcode(String matchedNormSampleBarcode) {
        this.matchedNormSampleBarcode = matchedNormSampleBarcode;
    }

    public String getMatchNormSeqAllele1() {
        return matchNormSeqAllele1;
    }

    public void setMatchNormSeqAllele1(String matchNormSeqAllele1) {
        this.matchNormSeqAllele1 = matchNormSeqAllele1;
    }

    public String getMatchNormSeqAllele2() {
        return matchNormSeqAllele2;
    }

    public void setMatchNormSeqAllele2(String matchNormSeqAllele2) {
        this.matchNormSeqAllele2 = matchNormSeqAllele2;
    }

    public String getTumorValidationAllele1() {
        return tumorValidationAllele1;
    }

    public void setTumorValidationAllele1(String tumorValidationAllele1) {
        this.tumorValidationAllele1 = tumorValidationAllele1;
    }

    public String getTumorValidationAllele2() {
        return tumorValidationAllele2;
    }

    public void setTumorValidationAllele2(String tumorValidationAllele2) {
        this.tumorValidationAllele2 = tumorValidationAllele2;
    }

    public String getMatchNormValidationAllele1() {
        return matchNormValidationAllele1;
    }

    public void setMatchNormValidationAllele1(String matchNormValidationAllele1) {
        this.matchNormValidationAllele1 = matchNormValidationAllele1;
    }

    public String getMatchNormValidationAllele2() {
        return matchNormValidationAllele2;
    }

    public void setMatchNormValidationAllele2(String matchNormValidationAllele2) {
        this.matchNormValidationAllele2 = matchNormValidationAllele2;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public MutationStatus getMutationStatus() {
        return mutationStatus;
    }

    public void setMutationStatus(MutationStatus mutationStatus) {
        this.mutationStatus = mutationStatus;
    }

    public String getSequencingPhase() {
        return sequencingPhase;
    }

    public void setSequencingPhase(String sequencingPhase) {
        this.sequencingPhase = sequencingPhase;
    }

    public SequenceSource getSequenceSource() {
        return sequenceSource;
    }

    public void setSequenceSource(SequenceSource sequenceSource) {
        this.sequenceSource = sequenceSource;
    }

    public String getValidationMethod() {
        return validationMethod;
    }

    public void setValidationMethod(String validationMethod) {
        this.validationMethod = validationMethod;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getBamFile() {
        return bamFile;
    }

    public void setBamFile(String bamFile) {
        this.bamFile = bamFile;
    }

    public String getSequencer() {
        return sequencer;
    }

    public void setSequencer(String sequencer) {
        this.sequencer = sequencer;
    }

    public String getTumorSampleUUID() {
        return tumorSampleUUID;
    }

    public void setTumorSampleUUID(String tumorSampleUUID) {
        this.tumorSampleUUID = tumorSampleUUID;
    }

    public String getMatchedNormSampleUUID() {
        return matchedNormSampleUUID;
    }

    public void setMatchedNormSampleUUID(String matchedNormSampleUUID) {
        this.matchedNormSampleUUID = matchedNormSampleUUID;
    }
}
