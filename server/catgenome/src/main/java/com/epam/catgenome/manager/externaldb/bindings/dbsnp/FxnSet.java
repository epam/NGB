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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "FxnSet")
public class FxnSet {

    @XmlAttribute(name = "geneId")
    protected Integer geneId;
    @XmlAttribute(name = "symbol")
    protected String symbol;
    @XmlAttribute(name = "mrnaAcc")
    protected String mrnaAcc;
    @XmlAttribute(name = "mrnaVer")
    protected Integer mrnaVer;
    @XmlAttribute(name = "protAcc")
    protected String protAcc;
    @XmlAttribute(name = "protVer")
    protected Integer protVer;
    @XmlAttribute(name = "fxnClass")
    protected String fxnClass;
    @XmlAttribute(name = "readingFrame")
    protected Integer readingFrame;
    @XmlAttribute(name = "allele")
    protected String allele;
    @XmlAttribute(name = "residue")
    protected String residue;
    @XmlAttribute(name = "aaPosition")
    protected Integer aaPosition;
    @XmlAttribute(name = "mrnaPosition")
    protected Integer mrnaPosition;
    @XmlAttribute(name = "soTerm")
    protected String soTerm;

    /**
     * Gets the value of the geneId property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getGeneId() {
        return geneId;
    }

    /**
     * Sets the value of the geneId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setGeneId(Integer value) {
        this.geneId = value;
    }

    /**
     * Gets the value of the symbol property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Sets the value of the symbol property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSymbol(String value) {
        this.symbol = value;
    }

    /**
     * Gets the value of the mrnaAcc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMrnaAcc() {
        return mrnaAcc;
    }

    /**
     * Sets the value of the mrnaAcc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMrnaAcc(String value) {
        this.mrnaAcc = value;
    }

    /**
     * Gets the value of the mrnaVer property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMrnaVer() {
        return mrnaVer;
    }

    /**
     * Sets the value of the mrnaVer property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMrnaVer(Integer value) {
        this.mrnaVer = value;
    }

    /**
     * Gets the value of the protAcc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProtAcc() {
        return protAcc;
    }

    /**
     * Sets the value of the protAcc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProtAcc(String value) {
        this.protAcc = value;
    }

    /**
     * Gets the value of the protVer property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getProtVer() {
        return protVer;
    }

    /**
     * Sets the value of the protVer property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setProtVer(Integer value) {
        this.protVer = value;
    }

    /**
     * Gets the value of the fxnClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFxnClass() {
        return fxnClass;
    }

    /**
     * Sets the value of the fxnClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFxnClass(String value) {
        this.fxnClass = value;
    }

    /**
     * Gets the value of the readingFrame property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getReadingFrame() {
        return readingFrame;
    }

    /**
     * Sets the value of the readingFrame property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setReadingFrame(Integer value) {
        this.readingFrame = value;
    }

    /**
     * Gets the value of the allele property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllele() {
        return allele;
    }

    /**
     * Sets the value of the allele property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllele(String value) {
        this.allele = value;
    }

    /**
     * Gets the value of the residue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResidue() {
        return residue;
    }

    /**
     * Sets the value of the residue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResidue(String value) {
        this.residue = value;
    }

    /**
     * Gets the value of the aaPosition property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getAaPosition() {
        return aaPosition;
    }

    /**
     * Sets the value of the aaPosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setAaPosition(Integer value) {
        this.aaPosition = value;
    }

    /**
     * Gets the value of the mrnaPosition property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMrnaPosition() {
        return mrnaPosition;
    }

    /**
     * Sets the value of the mrnaPosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMrnaPosition(Integer value) {
        this.mrnaPosition = value;
    }

    /**
     * Gets the value of the soTerm property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSoTerm() {
        return soTerm;
    }

    /**
     * Sets the value of the soTerm property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSoTerm(String value) {
        this.soTerm = value;
    }

}
