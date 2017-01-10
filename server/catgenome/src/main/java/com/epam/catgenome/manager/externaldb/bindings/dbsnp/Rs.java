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

package com.epam.catgenome.manager.externaldb.bindings.dbsnp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "het",
    "validation",
    "create",
    "update",
    "sequence",
    "ss",
    "assembly",
    "primarySequence",
    "rsStruct",
    "rsLinkout",
    "mergeHistory",
    "hgvs",
    "alleleOrigin",
    "phenotype",
    "bioSource",
    "frequency"
    })
@XmlRootElement(name = "Rs")
public class Rs {

    @XmlElement(name = "Het")
    protected Rs.Het het;
    @XmlElement(name = "Validation", required = true)
    protected Rs.Validation validation;
    @XmlElement(name = "Create", required = true)
    protected Rs.Create create;
    @XmlElement(name = "Update")
    protected Rs.Update update;
    @XmlElement(name = "Sequence", required = true)
    protected Rs.Sequence sequence;
    @XmlElement(name = "Ss", required = true)
    protected List<Ss> ss;
    @XmlElement(name = "Assembly")
    protected List<Assembly> assembly;
    @XmlElement(name = "PrimarySequence")
    protected List<PrimarySequence> primarySequence;
    @XmlElement(name = "RsStruct")
    protected List<RsStruct> rsStruct;
    @XmlElement(name = "RsLinkout")
    protected List<RsLinkout> rsLinkout;
    @XmlElement(name = "MergeHistory")
    protected List<Rs.MergeHistory> mergeHistory;
    protected List<String> hgvs;
    @XmlElement(name = "AlleleOrigin")
    protected List<Rs.AlleleOrigin> alleleOrigin;
    @XmlElement(name = "Phenotype")
    protected List<Rs.Phenotype> phenotype;
    @XmlElement(name = "BioSource")
    protected List<Rs.BioSource> bioSource;
    @XmlElement(name = "Frequency")
    protected List<Rs.Frequency> frequency;
    @XmlAttribute(name = "rsId", required = true)
    protected int rsId;
    @XmlAttribute(name = "snpClass", required = true)
    protected String snpClass;
    @XmlAttribute(name = "snpType", required = true)
    protected String snpType;
    @XmlAttribute(name = "molType", required = true)
    protected String molType;
    @XmlAttribute(name = "validProbMin")
    protected BigInteger validProbMin;
    @XmlAttribute(name = "validProbMax")
    protected BigInteger validProbMax;
    @XmlAttribute(name = "genotype")
    protected Boolean genotype;
    @XmlAttribute(name = "bitField")
    protected String bitField;
    @XmlAttribute(name = "taxId")
    protected Integer taxId;

    public Rs.Het getHet() {
        return het;
    }

    public void setHet(Rs.Het value) {
        this.het = value;
    }

    public Rs.Validation getValidation() {
        return validation;
    }

    public void setValidation(Rs.Validation value) {
        this.validation = value;
    }

    public Rs.Create getCreate() {
        return create;
    }

    public void setCreate(Rs.Create value) {
        this.create = value;
    }

    public Rs.Update getUpdate() {
        return update;
    }

    public void setUpdate(Rs.Update value) {
        this.update = value;
    }

    public Rs.Sequence getSequence() {
        return sequence;
    }

    public void setSequence(Rs.Sequence value) {
        this.sequence = value;
    }

    public List<Ss> getSs() {
        if (ss == null) {
            ss = new ArrayList<>();
        }
        return this.ss;
    }

    public List<Assembly> getAssembly() {
        if (assembly == null) {
            assembly = new ArrayList<>();
        }
        return this.assembly;
    }

    public List<PrimarySequence> getPrimarySequence() {
        if (primarySequence == null) {
            primarySequence = new ArrayList<>();
        }
        return this.primarySequence;
    }
    public List<RsStruct> getRsStruct() {
        if (rsStruct == null) {
            rsStruct = new ArrayList<>();
        }
        return this.rsStruct;
    }
    public List<RsLinkout> getRsLinkout() {
        if (rsLinkout == null) {
            rsLinkout = new ArrayList<>();
        }
        return this.rsLinkout;
    }
    public List<Rs.MergeHistory> getMergeHistory() {
        if (mergeHistory == null) {
            mergeHistory = new ArrayList<>();
        }
        return this.mergeHistory;
    }

    public List<String> getHgvs() {
        if (hgvs == null) {
            hgvs = new ArrayList<>();
        }
        return this.hgvs;
    }

    public List<Rs.AlleleOrigin> getAlleleOrigin() {
        if (alleleOrigin == null) {
            alleleOrigin = new ArrayList<>();
        }
        return this.alleleOrigin;
    }

    public List<Rs.Phenotype> getPhenotype() {
        if (phenotype == null) {
            phenotype = new ArrayList<>();
        }
        return this.phenotype;
    }

    public List<Rs.BioSource> getBioSource() {
        if (bioSource == null) {
            bioSource = new ArrayList<>();
        }
        return this.bioSource;
    }

    public List<Rs.Frequency> getFrequency() {
        if (frequency == null) {
            frequency = new ArrayList<>();
        }
        return this.frequency;
    }

    public int getRsId() {
        return rsId;
    }

    public void setRsId(int value) {
        this.rsId = value;
    }

    public String getSnpClass() {
        return snpClass;
    }

    public void setSnpClass(String value) {
        this.snpClass = value;
    }

    public String getSnpType() {
        return snpType;
    }

    public void setSnpType(String value) {
        this.snpType = value;
    }

    public String getMolType() {
        return molType;
    }

    public void setMolType(String value) {
        this.molType = value;
    }

    public BigInteger getValidProbMin() {
        return validProbMin;
    }

    public void setValidProbMin(BigInteger value) {
        this.validProbMin = value;
    }

    public BigInteger getValidProbMax() {
        return validProbMax;
    }

    public void setValidProbMax(BigInteger value) {
        this.validProbMax = value;
    }

    public Boolean isGenotype() {
        return genotype;
    }

    public void setGenotype(Boolean value) {
        this.genotype = value;
    }

    public String getBitField() {
        return bitField;
    }


    public void setBitField(String value) {
        this.bitField = value;
    }

    public Integer getTaxId() {
        return taxId;
    }

    public void setTaxId(Integer value) {
        this.taxId = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;simpleContent>
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>integer">
     *       &lt;attribute name="allele" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/extension>
     *   &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
        })
    public static class AlleleOrigin {

        @XmlValue
        protected BigInteger value;
        @XmlAttribute(name = "allele")
        protected String allele;

        public BigInteger getValue() {
            return value;
        }

        public void setValue(BigInteger value) {
            this.value = value;
        }

        public String getAllele() {
            return allele;
        }

        public void setAllele(String value) {
            this.allele = value;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "genome",
        "origin"
        })
    public static class BioSource {

        @XmlElement(name = "Genome")
        protected List<String> genome;
        @XmlElement(name = "Origin")
        protected List<String> origin;

        public List<String> getGenome() {
            if (genome == null) {
                genome = new ArrayList<>();
            }
            return this.genome;
        }

        public List<String> getOrigin() {
            if (origin == null) {
                origin = new ArrayList<>();
            }
            return this.origin;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     *
     * <p>The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="build" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="date" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     *
     *
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Create {

        @XmlAttribute(name = "build")
        protected Integer build;
        @XmlAttribute(name = "date")
        protected String date;

        public Integer getBuild() {
            return build;
        }

        public void setBuild(Integer value) {
            this.build = value;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String value) {
            this.date = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="freq" type="{http://www.w3.org/2001/XMLSchema}double" />
     *       &lt;attribute name="allele" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="popId" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="sampleSize" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Frequency {

        @XmlAttribute(name = "freq")
        protected Double freq;
        @XmlAttribute(name = "allele")
        protected String allele;
        @XmlAttribute(name = "popId")
        protected BigInteger popId;
        @XmlAttribute(name = "sampleSize")
        protected BigInteger sampleSize;

        public Double getFreq() {
            return freq;
        }

        public void setFreq(Double value) {
            this.freq = value;
        }

        public String getAllele() {
            return allele;
        }

        public void setAllele(String value) {
            this.allele = value;
        }

        public BigInteger getPopId() {
            return popId;
        }

        public void setPopId(BigInteger value) {
            this.popId = value;
        }

        public BigInteger getSampleSize() {
            return sampleSize;
        }

        public void setSampleSize(BigInteger value) {
            this.sampleSize = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="type" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *             &lt;enumeration value="est"/>
     *             &lt;enumeration value="obs"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}float" />
     *       &lt;attribute name="stdError" type="{http://www.w3.org/2001/XMLSchema}float" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Het {

        @XmlAttribute(name = "type", required = true)
        protected String type;
        @XmlAttribute(name = "value", required = true)
        protected float value;
        @XmlAttribute(name = "stdError")
        protected Float stdError;

        public String getType() {
            return type;
        }

        public void setType(String value) {
            this.type = value;
        }

        public float getValue() {
            return value;
        }

        public void setValue(float value) {
            this.value = value;
        }

        public Float getStdError() {
            return stdError;
        }

        public void setStdError(Float value) {
            this.stdError = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="rsId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="buildId" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="orientFlip" type="{http://www.w3.org/2001/XMLSchema}boolean" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class MergeHistory {

        @XmlAttribute(name = "rsId", required = true)
        protected int rsId;
        @XmlAttribute(name = "buildId")
        protected Integer buildId;
        @XmlAttribute(name = "orientFlip")
        protected Boolean orientFlip;

        public int getRsId() {
            return rsId;
        }

        public void setRsId(int value) {
            this.rsId = value;
        }

        public Integer getBuildId() {
            return buildId;
        }

        public void setBuildId(Integer value) {
            this.buildId = value;
        }

        public Boolean isOrientFlip() {
            return orientFlip;
        }

        public void setOrientFlip(Boolean value) {
            this.orientFlip = value;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "clinicalSignificance"
        })
    public static class Phenotype {

        @XmlElement(name = "ClinicalSignificance")
        protected List<String> clinicalSignificance;

        public List<String> getClinicalSignificance() {
            if (clinicalSignificance == null) {
                clinicalSignificance = new ArrayList<>();
            }
            return this.clinicalSignificance;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Seq5" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *         &lt;element name="Observed" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *         &lt;element name="Seq3" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *       &lt;/sequence>
     *       &lt;attribute name="exemplarSs" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="ancestralAllele" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "seq5",
        "observed",
        "seq3"
        })
    public static class Sequence {

        @XmlElement(name = "Seq5")
        protected String seq5;
        @XmlElement(name = "Observed", required = true)
        protected String observed;
        @XmlElement(name = "Seq3")
        protected String seq3;
        @XmlAttribute(name = "exemplarSs", required = true)
        protected int exemplarSs;
        @XmlAttribute(name = "ancestralAllele")
        protected String ancestralAllele;

        public String getSeq5() {
            return seq5;
        }

        public void setSeq5(String value) {
            this.seq5 = value;
        }

        public String getObserved() {
            return observed;
        }

        public void setObserved(String value) {
            this.observed = value;
        }

        public String getSeq3() {
            return seq3;
        }

        public void setSeq3(String value) {
            this.seq3 = value;
        }

        public int getExemplarSs() {
            return exemplarSs;
        }

        public void setExemplarSs(int value) {
            this.exemplarSs = value;
        }

        public String getAncestralAllele() {
            return ancestralAllele;
        }

        public void setAncestralAllele(String value) {
            this.ancestralAllele = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="build" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="date" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Update {

        @XmlAttribute(name = "build")
        protected Integer build;
        @XmlAttribute(name = "date")
        protected String date;

        public Integer getBuild() {
            return build;
        }

        public void setBuild(Integer value) {
            this.build = value;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String value) {
            this.date = value;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "otherPopBatchId",
        "twoHit2AlleleBatchId",
        "frequencyClass",
        "hapMapPhase",
        "tgpPhase",
        "suspectEvidence"
        })
    public static class Validation {

        @XmlElement(type = Integer.class)
        protected List<Integer> otherPopBatchId;
        @XmlElement(type = Integer.class)
        protected List<Integer> twoHit2AlleleBatchId;
        @XmlElement(name = "FrequencyClass", type = Integer.class)
        protected List<Integer> frequencyClass;
        @XmlElement(name = "HapMapPhase", type = Integer.class)
        protected List<Integer> hapMapPhase;
        @XmlElement(name = "TGPPhase", type = Integer.class)
        protected List<Integer> tgpPhase;
        @XmlElement(name = "SuspectEvidence")
        protected List<String> suspectEvidence;
        @XmlAttribute(name = "byCluster")
        protected Boolean byCluster;
        @XmlAttribute(name = "byFrequency")
        protected Boolean byFrequency;
        @XmlAttribute(name = "byOtherPop")
        protected Boolean byOtherPop;
        @XmlAttribute(name = "by2Hit2Allele")
        protected Boolean by2Hit2Allele;
        @XmlAttribute(name = "byHapMap")
        protected Boolean byHapMap;
        @XmlAttribute(name = "by1000G")
        protected Boolean by1000G;
        @XmlAttribute(name = "suspect")
        protected Boolean suspect;

        public List<Integer> getOtherPopBatchId() {
            if (otherPopBatchId == null) {
                otherPopBatchId = new ArrayList<>();
            }
            return this.otherPopBatchId;
        }

        public List<Integer> getTwoHit2AlleleBatchId() {
            if (twoHit2AlleleBatchId == null) {
                twoHit2AlleleBatchId = new ArrayList<>();
            }
            return this.twoHit2AlleleBatchId;
        }

        public List<Integer> getFrequencyClass() {
            if (frequencyClass == null) {
                frequencyClass = new ArrayList<>();
            }
            return this.frequencyClass;
        }

        public List<Integer> getHapMapPhase() {
            if (hapMapPhase == null) {
                hapMapPhase = new ArrayList<>();
            }
            return this.hapMapPhase;
        }

        public List<Integer> getTGPPhase() {
            if (tgpPhase == null) {
                tgpPhase = new ArrayList<>();
            }
            return this.tgpPhase;
        }

        public List<String> getSuspectEvidence() {
            if (suspectEvidence == null) {
                suspectEvidence = new ArrayList<>();
            }
            return this.suspectEvidence;
        }

        public Boolean isByCluster() {
            return byCluster;
        }

        public void setByCluster(Boolean value) {
            this.byCluster = value;
        }

        public Boolean isByFrequency() {
            return byFrequency;
        }

        public void setByFrequency(Boolean value) {
            this.byFrequency = value;
        }

        public Boolean isByOtherPop() {
            return byOtherPop;
        }

        public void setByOtherPop(Boolean value) {
            this.byOtherPop = value;
        }

        public Boolean isBy2Hit2Allele() {
            return by2Hit2Allele;
        }

        public void setBy2Hit2Allele(Boolean value) {
            this.by2Hit2Allele = value;
        }

        public Boolean isByHapMap() {
            return byHapMap;
        }

        public void setByHapMap(Boolean value) {
            this.byHapMap = value;
        }

        public Boolean isBy1000G() {
            return by1000G;
        }

        public void setBy1000G(Boolean value) {
            this.by1000G = value;
        }

        public Boolean isSuspect() {
            return suspect;
        }

        public void setSuspect(Boolean value) {
            this.suspect = value;
        }
    }
}
