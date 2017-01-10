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

/**
 * Implementation of {@code MafFeature} and transitional object between data in MAF file
 * and {@code {@link MafFeature}}
 */
public class NggbMafFeature implements MafFeature {
    private String sampleName;
    private String hugoSymbol;
    private int entrezGeneId;
    private String center;
    private String ncbiBuild;
    private String chr;
    private int start;
    private int end;
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
    private String sequencer;
    private String tumorSampleUUID;
    private String matchedNormSampleUUID;

    /**
     * Creates a class instance with basic required fields
     * @param chr of a feature
     * @param start of a feature
     * @param end of a feature
     */
    public NggbMafFeature(String chr, int start, int end) {
        this.chr = chr;
        this.start = start;
        this.end = end;
    }

    @Override
    public String getChr() {
        return chr;
    }

    @Override
    public String getContig() {
        return chr;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public String getSampleName() {
        return sampleName;
    }

    @Override
    public String getHugoSymbol() {
        return hugoSymbol;
    }

    @Override
    public Integer getEntrezGeneId() {
        return entrezGeneId;
    }

    @Override
    public String getCenter() {
        return center;
    }

    @Override
    public String getNcbiBuild() {
        return ncbiBuild;
    }

    @Override
    public StrandSerializable getStrand() {
        return strand;
    }

    @Override
    public VariantClassification getVariantClassification() {
        return variantClassification;
    }

    @Override
    public VariationType getVariationType() {
        return variationType;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public String getTumorSeqAllele1() {
        return tumorSeqAllele1;
    }

    @Override
    public String getTumorSeqAllele2() {
        return tumorSeqAllele2;
    }

    @Override
    public String getDbSnpRS() {
        return dbSnpRS;
    }

    @Override
    public String getDbSnpValStatus() {
        return dbSnpValStatus;
    }

    @Override
    public String getTumorSampleBarcode() {
        return tumorSampleBarcode;
    }

    @Override
    public String getMatchedNormSampleBarcode() {
        return matchedNormSampleBarcode;
    }

    @Override
    public String getMatchNormSeqAllele1() {
        return matchNormSeqAllele1;
    }

    @Override
    public String getMatchNormSeqAllele2() {
        return matchNormSeqAllele2;
    }

    @Override
    public String getTumorValidationAllele1() {
        return tumorValidationAllele1;
    }

    @Override
    public String getTumorValidationAllele2() {
        return tumorValidationAllele2;
    }

    @Override
    public String getMatchNormValidationAllele1() {
        return matchNormValidationAllele1;
    }

    @Override
    public String getMatchNormValidationAllele2() {
        return matchNormValidationAllele2;
    }

    @Override
    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    @Override
    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    @Override
    public MutationStatus getMutationStatus() {
        return mutationStatus;
    }

    @Override
    public String getSequencingPhase() {
        return sequencingPhase;
    }

    @Override
    public SequenceSource getSequenceSource() {
        return sequenceSource;
    }

    @Override
    public String getValidationMethod() {
        return validationMethod;
    }

    @Override
    public Integer getScore() {
        return null;
    }

    @Override
    public String getBamFile() {
        return null;
    }

    @Override
    public String getSequencer() {
        return sequencer;
    }

    @Override
    public String getTumorSampleUUID() {
        return tumorSampleUUID;
    }

    @Override
    public String getMatchedNormSampleUUID() {
        return matchedNormSampleUUID;
    }

    public void setHugoSymbol(String hugoSymbol) {
        this.hugoSymbol = hugoSymbol;
    }

    public void setEntrezGeneId(int entrezGeneId) {
        this.entrezGeneId = entrezGeneId;
    }

    public void setCenter(String center) {
        this.center = center;
    }

    public void setStrand(StrandSerializable strand) {
        this.strand = strand;
    }

    public void setVariantClassification(VariantClassification variantClassification) {
        this.variantClassification = variantClassification;
    }

    public void setVariationType(VariationType variationType) {
        this.variationType = variationType;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public void setTumorSeqAllele1(String tumorSeqAllele1) {
        this.tumorSeqAllele1 = tumorSeqAllele1;
    }

    public void setTumorSeqAllele2(String tumorSeqAllele2) {
        this.tumorSeqAllele2 = tumorSeqAllele2;
    }

    public void setDbSnpRS(String dbSnpRS) {
        this.dbSnpRS = dbSnpRS;
    }

    public void setDbSnpValStatus(String dbSnpValStatus) {
        this.dbSnpValStatus = dbSnpValStatus;
    }

    public void setTumorSampleBarcode(String tumorSampleBarcode) {
        this.tumorSampleBarcode = tumorSampleBarcode;
    }

    public void setNcbiBuild(String ncbiBuild) {
        this.ncbiBuild = ncbiBuild;
    }

    public void setMatchedNormSampleBarcode(String matchedNormSampleBarcode) {
        this.matchedNormSampleBarcode = matchedNormSampleBarcode;
    }

    public void setMatchNormSeqAllele1(String matchNormSeqAllele1) {
        this.matchNormSeqAllele1 = matchNormSeqAllele1;
    }

    public void setMatchNormSeqAllele2(String matchNormSeqAllele2) {
        this.matchNormSeqAllele2 = matchNormSeqAllele2;
    }

    public void setTumorValidationAllele1(String tumorValidationAllele1) {
        this.tumorValidationAllele1 = tumorValidationAllele1;
    }

    public void setTumorValidationAllele2(String tumorValidationAllele2) {
        this.tumorValidationAllele2 = tumorValidationAllele2;
    }

    public void setMatchNormValidationAllele1(String matchNormValidationAllele1) {
        this.matchNormValidationAllele1 = matchNormValidationAllele1;
    }

    public void setMatchNormValidationAllele2(String matchNormValidationAllele2) {
        this.matchNormValidationAllele2 = matchNormValidationAllele2;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public void setMutationStatus(MutationStatus mutationStatus) {
        this.mutationStatus = mutationStatus;
    }

    public void setSequencingPhase(String sequencingPhase) {
        this.sequencingPhase = sequencingPhase;
    }

    public void setSequenceSource(SequenceSource sequenceSource) {
        this.sequenceSource = sequenceSource;
    }

    public void setValidationMethod(String validationMethod) {
        this.validationMethod = validationMethod;
    }

    public void setSequencer(String sequencer) {
        this.sequencer = sequencer;
    }

    public void setTumorSampleUUID(String tumorSampleUUID) {
        this.tumorSampleUUID = tumorSampleUUID;
    }

    public void setMatchedNormSampleUUID(String matchedNormSampleUUID) {
        this.matchedNormSampleUUID = matchedNormSampleUUID;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    @Override
    public String toBigMafString() {
        return new StringBuilder(sampleName).append("\t")
                .append(hugoSymbol).append("\t")
                .append(entrezGeneId).append("\t")
                .append(center).append("\t")
                .append(ncbiBuild).append("\t")
                .append(chr).append("\t")
                .append(start).append("\t")
                .append(end).append("\t")
                .append(strand != null ? strand.getFileValue() : "").append("\t")
                .append(variantClassification != null ? variantClassification.getFileValue(): "").append("\t")
                .append(variationType.toMafFileValue()).append("\t")
                .append(ref).append("\t")
                .append(tumorSeqAllele1).append("\t")
                .append(tumorSeqAllele2).append("\t")
                .append(dbSnpRS).append("\t")
                .append(dbSnpValStatus).append("\t")
                .append(tumorSampleBarcode).append("\t")
                .append(matchedNormSampleBarcode).append("\t")
                .append(matchNormSeqAllele1).append("\t")
                .append(matchNormSeqAllele2).append("\t")
                .append(tumorValidationAllele1).append("\t")
                .append(tumorValidationAllele2).append("\t")
                .append(matchNormValidationAllele1).append("\t")
                .append(matchNormValidationAllele2).append("\t")
                .append(verificationStatus != null ? verificationStatus.getFileValue() : "").append("\t")
                .append(validationStatus != null ? validationStatus.getFileValue() : "").append("\t")
                .append(mutationStatus != null ? mutationStatus.getFileValue() : "").append("\t")
                .append(sequencingPhase).append("\t")
                .append(sequenceSource != null ? sequenceSource.getFileValue() : "").append("\t")
                .append(validationMethod).append("\t")
                .append("").append("\t")
                .append("").append("\t")
                .append(sequencer).append("\t")
                .append(tumorSampleUUID).append("\t")
                .append(matchedNormSampleUUID)
                .toString();
    }
}
