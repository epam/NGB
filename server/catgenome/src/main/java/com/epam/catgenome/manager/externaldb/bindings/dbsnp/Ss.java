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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * <p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Sequence">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Seq5" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                   &lt;element name="Observed" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="Seq3" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="ssId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="handle" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="batchId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="locSnpId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="subSnpClass">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="snp"/>
 *             &lt;enumeration value="in-del"/>
 *             &lt;enumeration value="heterozygous"/>
 *             &lt;enumeration value="microsatellite"/>
 *             &lt;enumeration value="named-locus"/>
 *             &lt;enumeration value="no-variation"/>
 *             &lt;enumeration value="mixed"/>
 *             &lt;enumeration value="multinucleotide-polymorphism"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="orient">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="forward"/>
 *             &lt;enumeration value="reverse"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="strand">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="top"/>
 *             &lt;enumeration value="bottom"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="molType">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="genomic"/>
 *             &lt;enumeration value="cDNA"/>
 *             &lt;enumeration value="mito"/>
 *             &lt;enumeration value="chloro"/>
 *             &lt;enumeration value="unknown"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="buildId" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="methodClass">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="DHPLC"/>
 *             &lt;enumeration value="hybridize"/>
 *             &lt;enumeration value="computed"/>
 *             &lt;enumeration value="SSCP"/>
 *             &lt;enumeration value="other"/>
 *             &lt;enumeration value="unknown"/>
 *             &lt;enumeration value="RFLP"/>
 *             &lt;enumeration value="sequence"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="validated">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="by-submitter"/>
 *             &lt;enumeration value="by-frequency"/>
 *             &lt;enumeration value="by-cluster"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="linkoutUrl" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ssAlias" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="alleleOrigin" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="clinicalSignificance" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "sequence"
    })
@XmlRootElement(name = "Ss")
public class Ss {

    @XmlElement(name = "Sequence", required = true)
    protected Ss.Sequence sequence;
    @XmlAttribute(name = "ssId", required = true)
    protected int ssId;
    @XmlAttribute(name = "handle", required = true)
    protected String handle;
    @XmlAttribute(name = "batchId", required = true)
    protected int batchId;
    @XmlAttribute(name = "locSnpId")
    protected String locSnpId;
    @XmlAttribute(name = "subSnpClass")
    protected String subSnpClass;
    @XmlAttribute(name = "orient")
    protected String orient;
    @XmlAttribute(name = "strand")
    protected String strand;
    @XmlAttribute(name = "molType")
    protected String molType;
    @XmlAttribute(name = "buildId")
    protected Integer buildId;
    @XmlAttribute(name = "methodClass")
    protected String methodClass;
    @XmlAttribute(name = "validated")
    protected String validated;
    @XmlAttribute(name = "linkoutUrl")
    protected String linkoutUrl;
    @XmlAttribute(name = "ssAlias")
    protected String ssAlias;
    @XmlAttribute(name = "alleleOrigin")
    protected BigInteger alleleOrigin;
    @XmlAttribute(name = "clinicalSignificance")
    protected String clinicalSignificance;

    /**
     * Gets the value of the sequence property.
     *
     * @return possible object is
     * {@link Ss.Sequence }
     */
    public Ss.Sequence getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     *
     * @param value allowed object is
     *              {@link Ss.Sequence }
     */
    public void setSequence(Ss.Sequence value) {
        this.sequence = value;
    }

    /**
     * Gets the value of the ssId property.
     */
    public int getSsId() {
        return ssId;
    }

    /**
     * Sets the value of the ssId property.
     */
    public void setSsId(int value) {
        this.ssId = value;
    }

    /**
     * Gets the value of the handle property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getHandle() {
        return handle;
    }

    /**
     * Sets the value of the handle property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setHandle(String value) {
        this.handle = value;
    }

    /**
     * Gets the value of the batchId property.
     */
    public int getBatchId() {
        return batchId;
    }

    /**
     * Sets the value of the batchId property.
     */
    public void setBatchId(int value) {
        this.batchId = value;
    }

    /**
     * Gets the value of the locSnpId property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getLocSnpId() {
        return locSnpId;
    }

    /**
     * Sets the value of the locSnpId property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLocSnpId(String value) {
        this.locSnpId = value;
    }

    /**
     * Gets the value of the subSnpClass property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSubSnpClass() {
        return subSnpClass;
    }

    /**
     * Sets the value of the subSnpClass property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSubSnpClass(String value) {
        this.subSnpClass = value;
    }

    /**
     * Gets the value of the orient property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getOrient() {
        return orient;
    }

    /**
     * Sets the value of the orient property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setOrient(String value) {
        this.orient = value;
    }

    /**
     * Gets the value of the strand property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getStrand() {
        return strand;
    }

    /**
     * Sets the value of the strand property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setStrand(String value) {
        this.strand = value;
    }

    /**
     * Gets the value of the molType property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getMolType() {
        return molType;
    }

    /**
     * Sets the value of the molType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMolType(String value) {
        this.molType = value;
    }

    /**
     * Gets the value of the buildId property.
     *
     * @return possible object is
     * {@link Integer }
     */
    public Integer getBuildId() {
        return buildId;
    }

    /**
     * Sets the value of the buildId property.
     *
     * @param value allowed object is
     *              {@link Integer }
     */
    public void setBuildId(Integer value) {
        this.buildId = value;
    }

    /**
     * Gets the value of the methodClass property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getMethodClass() {
        return methodClass;
    }

    /**
     * Sets the value of the methodClass property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMethodClass(String value) {
        this.methodClass = value;
    }

    /**
     * Gets the value of the validated property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getValidated() {
        return validated;
    }

    /**
     * Sets the value of the validated property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setValidated(String value) {
        this.validated = value;
    }

    /**
     * Gets the value of the linkoutUrl property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getLinkoutUrl() {
        return linkoutUrl;
    }

    /**
     * Sets the value of the linkoutUrl property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLinkoutUrl(String value) {
        this.linkoutUrl = value;
    }

    /**
     * Gets the value of the ssAlias property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSsAlias() {
        return ssAlias;
    }

    /**
     * Sets the value of the ssAlias property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSsAlias(String value) {
        this.ssAlias = value;
    }

    /**
     * Gets the value of the alleleOrigin property.
     *
     * @return possible object is
     * {@link BigInteger }
     */
    public BigInteger getAlleleOrigin() {
        return alleleOrigin;
    }

    /**
     * Sets the value of the alleleOrigin property.
     *
     * @param value allowed object is
     *              {@link BigInteger }
     */
    public void setAlleleOrigin(BigInteger value) {
        this.alleleOrigin = value;
    }

    /**
     * Gets the value of the clinicalSignificance property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getClinicalSignificance() {
        return clinicalSignificance;
    }

    /**
     * Sets the value of the clinicalSignificance property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setClinicalSignificance(String value) {
        this.clinicalSignificance = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * <p>
     * <p>The following schema fragment specifies the expected content contained within this class.
     * <p>
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Seq5" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *         &lt;element name="Observed" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *         &lt;element name="Seq3" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
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

        /**
         * Gets the value of the seq5 property.
         *
         * @return possible object is
         * {@link String }
         */
        public String getSeq5() {
            return seq5;
        }

        /**
         * Sets the value of the seq5 property.
         *
         * @param value allowed object is
         *              {@link String }
         */
        public void setSeq5(String value) {
            this.seq5 = value;
        }

        /**
         * Gets the value of the observed property.
         *
         * @return possible object is
         * {@link String }
         */
        public String getObserved() {
            return observed;
        }

        /**
         * Sets the value of the observed property.
         *
         * @param value allowed object is
         *              {@link String }
         */
        public void setObserved(String value) {
            this.observed = value;
        }

        /**
         * Gets the value of the seq3 property.
         *
         * @return possible object is
         * {@link String }
         */
        public String getSeq3() {
            return seq3;
        }

        /**
         * Sets the value of the seq3 property.
         *
         * @param value allowed object is
         *              {@link String }
         */
        public void setSeq3(String value) {
            this.seq3 = value;
        }

    }

}
