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
    "mapLoc"
    })
@XmlRootElement(name = "PrimarySequence")
public class PrimarySequence {

    @XmlElement(name = "MapLoc", required = true)
    protected List<MapLoc> mapLoc;
    @XmlAttribute(name = "dbSnpBuild", required = true)
    protected int dbSnpBuild;
    @XmlAttribute(name = "gi", required = true)
    protected int gi;
    @XmlAttribute(name = "source")
    protected String source;
    @XmlAttribute(name = "accession")
    protected String accession;

    /**
     * Gets the value of the mapLoc property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mapLoc property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMapLoc().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MapLoc }
     * 
     * 
     */
    public List<MapLoc> getMapLoc() {
        if (mapLoc == null) {
            mapLoc = new ArrayList<>();
        }
        return this.mapLoc;
    }

    /**
     * Gets the value of the dbSnpBuild property.
     * 
     */
    public int getDbSnpBuild() {
        return dbSnpBuild;
    }

    /**
     * Sets the value of the dbSnpBuild property.
     * 
     */
    public void setDbSnpBuild(int value) {
        this.dbSnpBuild = value;
    }

    /**
     * Gets the value of the gi property.
     * 
     */
    public int getGi() {
        return gi;
    }

    /**
     * Sets the value of the gi property.
     * 
     */
    public void setGi(int value) {
        this.gi = value;
    }

    /**
     * Gets the value of the source property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSource(String value) {
        this.source = value;
    }

    /**
     * Gets the value of the accession property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAccession() {
        return accession;
    }

    /**
     * Sets the value of the accession property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAccession(String value) {
        this.accession = value;
    }

}
