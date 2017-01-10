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
    "component",
    "snpStat"
    })
@XmlRootElement(name = "Assembly")
public class Assembly {

    @XmlElement(name = "Component")
    protected List<Component> component;
    @XmlElement(name = "SnpStat", required = true)
    protected Assembly.SnpStat snpStat;
    @XmlAttribute(name = "dbSnpBuild", required = true)
    protected int dbSnpBuild;
    @XmlAttribute(name = "genomeBuild", required = true)
    protected String genomeBuild;
    @XmlAttribute(name = "groupLabel")
    protected String groupLabel;
    @XmlAttribute(name = "assemblySource")
    protected String assemblySource;
    @XmlAttribute(name = "current")
    protected Boolean current;
    @XmlAttribute(name = "reference")
    protected Boolean reference;

    /**
     * Gets the value of the component property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the component property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getComponent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Component }
     * 
     * 
     */
    public List<Component> getComponent() {
        if (component == null) {
            component = new ArrayList<>();
        }
        return this.component;
    }

    /**
     * Gets the value of the snpStat property.
     * 
     * @return
     *     possible object is
     *     {@link Assembly.SnpStat }
     *     
     */
    public Assembly.SnpStat getSnpStat() {
        return snpStat;
    }

    /**
     * Sets the value of the snpStat property.
     * 
     * @param value
     *     allowed object is
     *     {@link Assembly.SnpStat }
     *     
     */
    public void setSnpStat(Assembly.SnpStat value) {
        this.snpStat = value;
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
     * Gets the value of the genomeBuild property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGenomeBuild() {
        return genomeBuild;
    }

    /**
     * Sets the value of the genomeBuild property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGenomeBuild(String value) {
        this.genomeBuild = value;
    }

    /**
     * Gets the value of the groupLabel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroupLabel() {
        return groupLabel;
    }

    /**
     * Sets the value of the groupLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroupLabel(String value) {
        this.groupLabel = value;
    }

    /**
     * Gets the value of the assemblySource property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAssemblySource() {
        return assemblySource;
    }

    /**
     * Sets the value of the assemblySource property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAssemblySource(String value) {
        this.assemblySource = value;
    }

    /**
     * Gets the value of the current property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCurrent() {
        return current;
    }

    /**
     * Sets the value of the current property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCurrent(Boolean value) {
        this.current = value;
    }

    /**
     * Gets the value of the reference property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isReference() {
        return reference;
    }

    /**
     * Sets the value of the reference property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setReference(Boolean value) {
        this.reference = value;
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
     *       &lt;attribute name="mapWeight" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *             &lt;enumeration value="unmapped"/>
     *             &lt;enumeration value="unique-in-contig"/>
     *             &lt;enumeration value="two-hits-in-contig"/>
     *             &lt;enumeration value="less-10-hits"/>
     *             &lt;enumeration value="multiple-hits"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="chromCount" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="placedContigCount" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="unplacedContigCount" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="seqlocCount" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="hapCount" type="{http://www.w3.org/2001/XMLSchema}int" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class SnpStat {

        @XmlAttribute(name = "mapWeight", required = true)
        protected String mapWeight;
        @XmlAttribute(name = "chromCount")
        protected Integer chromCount;
        @XmlAttribute(name = "placedContigCount")
        protected Integer placedContigCount;
        @XmlAttribute(name = "unplacedContigCount")
        protected Integer unplacedContigCount;
        @XmlAttribute(name = "seqlocCount")
        protected Integer seqlocCount;
        @XmlAttribute(name = "hapCount")
        protected Integer hapCount;

        /**
         * Gets the value of the mapWeight property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMapWeight() {
            return mapWeight;
        }

        /**
         * Sets the value of the mapWeight property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMapWeight(String value) {
            this.mapWeight = value;
        }

        /**
         * Gets the value of the chromCount property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getChromCount() {
            return chromCount;
        }

        /**
         * Sets the value of the chromCount property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setChromCount(Integer value) {
            this.chromCount = value;
        }

        /**
         * Gets the value of the placedContigCount property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getPlacedContigCount() {
            return placedContigCount;
        }

        /**
         * Sets the value of the placedContigCount property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setPlacedContigCount(Integer value) {
            this.placedContigCount = value;
        }

        /**
         * Gets the value of the unplacedContigCount property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getUnplacedContigCount() {
            return unplacedContigCount;
        }

        /**
         * Sets the value of the unplacedContigCount property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setUnplacedContigCount(Integer value) {
            this.unplacedContigCount = value;
        }

        /**
         * Gets the value of the seqlocCount property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getSeqlocCount() {
            return seqlocCount;
        }

        /**
         * Sets the value of the seqlocCount property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setSeqlocCount(Integer value) {
            this.seqlocCount = value;
        }

        /**
         * Gets the value of the hapCount property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getHapCount() {
            return hapCount;
        }

        /**
         * Sets the value of the hapCount property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setHapCount(Integer value) {
            this.hapCount = value;
        }

    }

}
