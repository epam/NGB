package com.epam.catgenome.entity;

/**
 * Source:
 * Created:     11/10/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 *
 * @author Semen_Dmitriev
 */
public class NucleotideContent {

    private int aCount;
    private int tCount;
    private int gCount;
    private int cCount;
    private int totalNumberOfNucleotides;

    public NucleotideContent() {
        this.aCount = 0;
        this.tCount = 0;
        this.gCount = 0;
        this.cCount = 0;
        this.totalNumberOfNucleotides = 0;
    }

    public void incACount() {
        this.aCount++;
    }

    public void incTCount() {
        this.tCount++;
    }

    public void incGCount() {
        this.gCount++;
    }

    public void incCCount() {
        this.cCount++;
    }

    public void incTotalNumberOfNucleotides(final int n) {
        this.totalNumberOfNucleotides += n;
    }

    public int getACount() {
        return aCount;
    }

    public void setACount(int aCount) {
        this.aCount = aCount;
    }

    public int getTCount() {
        return tCount;
    }

    public void setTCount(int tCount) {
        this.tCount = tCount;
    }

    public int getGCount() {
        return gCount;
    }

    public void setGCount(int gCount) {
        this.gCount = gCount;
    }

    public int getCCount() {
        return cCount;
    }

    public void setCCount(int cCount) {
        this.cCount = cCount;
    }

    public int getTotalNumberOfNucleotides() {
        return totalNumberOfNucleotides;
    }

    public void setTotalNumberOfNucleotides(int totalNumberOfNucleotides) {
        this.totalNumberOfNucleotides = totalNumberOfNucleotides;
    }
}
