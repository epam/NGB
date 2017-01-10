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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sourceDatabase",
    "rs",
    "assay",
    "query",
    "summary",
    "baseURL"
    })
@XmlRootElement(name = "ExchangeSet")
public class ExchangeSet {

    @XmlElement(name = "SourceDatabase")
    protected ExchangeSet.SourceDatabase sourceDatabase;
    @XmlElement(name = "Rs")
    protected List<Rs> rs;
    @XmlElement(name = "Assay")
    protected Assay assay;
    @XmlElement(name = "Query")
    protected ExchangeSet.Query query;
    @XmlElement(name = "Summary")
    protected ExchangeSet.Summary summary;
    @XmlElement(name = "BaseURL")
    protected List<BaseURL> baseURL;
    @XmlAttribute(name = "setType")
    protected String setType;
    @XmlAttribute(name = "setDepth")
    protected String setDepth;
    @XmlAttribute(name = "specVersion")
    protected String specVersion;
    @XmlAttribute(name = "dbSnpBuild")
    protected Integer dbSnpBuild;
    @XmlAttribute(name = "generated")
    protected String generated;

    /**
     * Gets the value of the sourceDatabase property.
     * 
     * @return
     *     possible object is
     *     {@link ExchangeSet.SourceDatabase }
     *     
     */
    public ExchangeSet.SourceDatabase getSourceDatabase() {
        return sourceDatabase;
    }

    /**
     * Sets the value of the sourceDatabase property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExchangeSet.SourceDatabase }
     *     
     */
    public void setSourceDatabase(ExchangeSet.SourceDatabase value) {
        this.sourceDatabase = value;
    }

    /**
     * Gets the value of the rs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Rs }
     * 
     * 
     */
    public List<Rs> getRs() {
        if (rs == null) {
            rs = new ArrayList<>();
        }
        return this.rs;
    }

    /**
     * Gets the value of the assay property.
     * 
     * @return
     *     possible object is
     *     {@link Assay }
     *     
     */
    public Assay getAssay() {
        return assay;
    }

    /**
     * Sets the value of the assay property.
     * 
     * @param value
     *     allowed object is
     *     {@link Assay }
     *     
     */
    public void setAssay(Assay value) {
        this.assay = value;
    }

    /**
     * Gets the value of the query property.
     * 
     * @return
     *     possible object is
     *     {@link ExchangeSet.Query }
     *     
     */
    public ExchangeSet.Query getQuery() {
        return query;
    }

    /**
     * Sets the value of the query property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExchangeSet.Query }
     *     
     */
    public void setQuery(ExchangeSet.Query value) {
        this.query = value;
    }

    /**
     * Gets the value of the summary property.
     * 
     * @return
     *     possible object is
     *     {@link ExchangeSet.Summary }
     *     
     */
    public ExchangeSet.Summary getSummary() {
        return summary;
    }

    /**
     * Sets the value of the summary property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExchangeSet.Summary }
     *     
     */
    public void setSummary(ExchangeSet.Summary value) {
        this.summary = value;
    }

    /**
     * Gets the value of the baseURL property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the baseURL property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBaseURL().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BaseURL }
     * 
     * 
     */
    public List<BaseURL> getBaseURL() {
        if (baseURL == null) {
            baseURL = new ArrayList<>();
        }
        return this.baseURL;
    }

    /**
     * Gets the value of the setVariationType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSetType() {
        return setType;
    }

    /**
     * Sets the value of the setVariationType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSetType(String value) {
        this.setType = value;
    }

    /**
     * Gets the value of the setDepth property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSetDepth() {
        return setDepth;
    }

    /**
     * Sets the value of the setDepth property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSetDepth(String value) {
        this.setDepth = value;
    }

    /**
     * Gets the value of the specVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSpecVersion() {
        return specVersion;
    }

    /**
     * Sets the value of the specVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSpecVersion(String value) {
        this.specVersion = value;
    }

    /**
     * Gets the value of the dbSnpBuild property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getDbSnpBuild() {
        return dbSnpBuild;
    }

    /**
     * Sets the value of the dbSnpBuild property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setDbSnpBuild(Integer value) {
        this.dbSnpBuild = value;
    }

    /**
     * Gets the value of the generated property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGenerated() {
        return generated;
    }

    /**
     * Sets the value of the generated property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGenerated(String value) {
        this.generated = value;
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
     *       &lt;attribute name="date" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="string" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Query {

        @XmlAttribute(name = "date")
        protected String date;
        @XmlAttribute(name = "string")
        protected String string;

        /**
         * Gets the value of the date property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDate() {
            return date;
        }

        /**
         * Sets the value of the date property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDate(String value) {
            this.date = value;
        }

        /**
         * Gets the value of the string property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getString() {
            return string;
        }

        /**
         * Sets the value of the string property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setString(String value) {
            this.string = value;
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
     *       &lt;attribute name="taxId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="organism" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="dbSnpOrgAbbr" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="gpipeOrgAbbr" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class SourceDatabase {

        @XmlAttribute(name = "taxId", required = true)
        protected int taxId;
        @XmlAttribute(name = "organism", required = true)
        protected String organism;
        @XmlAttribute(name = "dbSnpOrgAbbr")
        protected String dbSnpOrgAbbr;
        @XmlAttribute(name = "gpipeOrgAbbr")
        protected String gpipeOrgAbbr;

        /**
         * Gets the value of the taxId property.
         * 
         */
        public int getTaxId() {
            return taxId;
        }

        /**
         * Sets the value of the taxId property.
         * 
         */
        public void setTaxId(int value) {
            this.taxId = value;
        }

        /**
         * Gets the value of the organism property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getOrganism() {
            return organism;
        }

        /**
         * Sets the value of the organism property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setOrganism(String value) {
            this.organism = value;
        }

        /**
         * Gets the value of the dbSnpOrgAbbr property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDbSnpOrgAbbr() {
            return dbSnpOrgAbbr;
        }

        /**
         * Sets the value of the dbSnpOrgAbbr property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDbSnpOrgAbbr(String value) {
            this.dbSnpOrgAbbr = value;
        }

        /**
         * Gets the value of the gpipeOrgAbbr property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getGpipeOrgAbbr() {
            return gpipeOrgAbbr;
        }

        /**
         * Sets the value of the gpipeOrgAbbr property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setGpipeOrgAbbr(String value) {
            this.gpipeOrgAbbr = value;
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
     *       &lt;attribute name="numRsIds" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="totalSeqLength" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="numContigHits" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="numGeneHits" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="numGiHits" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="num3dStructs" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="numAlleleFreqs" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="numStsHits" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *       &lt;attribute name="numUnigeneCids" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Summary {

        @XmlAttribute(name = "numRsIds")
        protected BigInteger numRsIds;
        @XmlAttribute(name = "totalSeqLength")
        protected BigInteger totalSeqLength;
        @XmlAttribute(name = "numContigHits")
        protected BigInteger numContigHits;
        @XmlAttribute(name = "numGeneHits")
        protected BigInteger numGeneHits;
        @XmlAttribute(name = "numGiHits")
        protected BigInteger numGiHits;
        @XmlAttribute(name = "num3dStructs")
        protected BigInteger num3DStructs;
        @XmlAttribute(name = "numAlleleFreqs")
        protected BigInteger numAlleleFreqs;
        @XmlAttribute(name = "numStsHits")
        protected BigInteger numStsHits;
        @XmlAttribute(name = "numUnigeneCids")
        protected BigInteger numUnigeneCids;

        /**
         * Gets the value of the numRsIds property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNumRsIds() {
            return numRsIds;
        }

        /**
         * Sets the value of the numRsIds property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNumRsIds(BigInteger value) {
            this.numRsIds = value;
        }

        /**
         * Gets the value of the totalSeqLength property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getTotalSeqLength() {
            return totalSeqLength;
        }

        /**
         * Sets the value of the totalSeqLength property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setTotalSeqLength(BigInteger value) {
            this.totalSeqLength = value;
        }

        /**
         * Gets the value of the numContigHits property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNumContigHits() {
            return numContigHits;
        }

        /**
         * Sets the value of the numContigHits property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNumContigHits(BigInteger value) {
            this.numContigHits = value;
        }

        /**
         * Gets the value of the numGeneHits property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNumGeneHits() {
            return numGeneHits;
        }

        /**
         * Sets the value of the numGeneHits property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNumGeneHits(BigInteger value) {
            this.numGeneHits = value;
        }

        /**
         * Gets the value of the numGiHits property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNumGiHits() {
            return numGiHits;
        }

        /**
         * Sets the value of the numGiHits property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNumGiHits(BigInteger value) {
            this.numGiHits = value;
        }

        /**
         * Gets the value of the num3DStructs property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNum3DStructs() {
            return num3DStructs;
        }

        /**
         * Sets the value of the num3DStructs property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNum3DStructs(BigInteger value) {
            this.num3DStructs = value;
        }

        /**
         * Gets the value of the numAlleleFreqs property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNumAlleleFreqs() {
            return numAlleleFreqs;
        }

        /**
         * Sets the value of the numAlleleFreqs property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNumAlleleFreqs(BigInteger value) {
            this.numAlleleFreqs = value;
        }

        /**
         * Gets the value of the numStsHits property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNumStsHits() {
            return numStsHits;
        }

        /**
         * Sets the value of the numStsHits property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNumStsHits(BigInteger value) {
            this.numStsHits = value;
        }

        /**
         * Gets the value of the numUnigeneCids property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNumUnigeneCids() {
            return numUnigeneCids;
        }

        /**
         * Sets the value of the numUnigeneCids property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNumUnigeneCids(BigInteger value) {
            this.numUnigeneCids = value;
        }

    }

}
