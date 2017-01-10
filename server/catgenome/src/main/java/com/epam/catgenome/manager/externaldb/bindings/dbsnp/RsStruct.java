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


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="protAcc" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="protGi" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="protLoc" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="protResidue" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="rsResidue" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="structGi" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="structLoc" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="structResidue" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "RsStruct")
public class RsStruct {

    @XmlAttribute(name = "protAcc")
    protected String protAcc;
    @XmlAttribute(name = "protGi")
    protected Integer protGi;
    @XmlAttribute(name = "protLoc")
    protected Integer protLoc;
    @XmlAttribute(name = "protResidue")
    protected String protResidue;
    @XmlAttribute(name = "rsResidue")
    protected String rsResidue;
    @XmlAttribute(name = "structGi")
    protected Integer structGi;
    @XmlAttribute(name = "structLoc")
    protected Integer structLoc;
    @XmlAttribute(name = "structResidue")
    protected String structResidue;

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
     * Gets the value of the protGi property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getProtGi() {
        return protGi;
    }

    /**
     * Sets the value of the protGi property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setProtGi(Integer value) {
        this.protGi = value;
    }

    /**
     * Gets the value of the protLoc property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getProtLoc() {
        return protLoc;
    }

    /**
     * Sets the value of the protLoc property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setProtLoc(Integer value) {
        this.protLoc = value;
    }

    /**
     * Gets the value of the protResidue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProtResidue() {
        return protResidue;
    }

    /**
     * Sets the value of the protResidue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProtResidue(String value) {
        this.protResidue = value;
    }

    /**
     * Gets the value of the rsResidue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRsResidue() {
        return rsResidue;
    }

    /**
     * Sets the value of the rsResidue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRsResidue(String value) {
        this.rsResidue = value;
    }

    /**
     * Gets the value of the structGi property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getStructGi() {
        return structGi;
    }

    /**
     * Sets the value of the structGi property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setStructGi(Integer value) {
        this.structGi = value;
    }

    /**
     * Gets the value of the structLoc property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getStructLoc() {
        return structLoc;
    }

    /**
     * Sets the value of the structLoc property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setStructLoc(Integer value) {
        this.structLoc = value;
    }

    /**
     * Gets the value of the structResidue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStructResidue() {
        return structResidue;
    }

    /**
     * Sets the value of the structResidue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStructResidue(String value) {
        this.structResidue = value;
    }

}
