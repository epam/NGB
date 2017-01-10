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

package com.epam.catgenome.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.reference.Sequence;

/**
 * Created: 2/3/2016
 * Project: CATGenome Browser
 * <p>
 * Utility class, containing tools for protein sequence reconstruction
 * </p>
 */
public final class ProteinSequenceUtils {
    private static final int NUCLEOTIDES_IN_CODON = 3;


    private ProteinSequenceUtils() {
    }

    /**
     * Represents RNA codon table to convert nucleotides to amino acids.
     */
    private enum RnaCodonTable {
        PHE("F", "Phe", "Phenylalanine", Arrays.asList("UUU", "UUC")),
        LEU("L", "Leu", "Leucine", Arrays.asList("UUA", "UUG", "CUU", "CUC", "CUA", "CUG")),
        ILE("I", "Ile", "Isoleucine", Arrays.asList("AUU", "AUC", "AUA")),
        MET("M", "Met", "Methionine", Collections.singletonList("AUG")),
        VAL("V", "Val", "Valine", Arrays.asList("GUU", "GUC", "GUA", "GUG")),
        SER("S", "Ser", "Serine", Arrays.asList("UCU", "UCC", "UCA", "UCG", "AGU", "AGC")),
        PRO("P", "Ser", "Proline", Arrays.asList("CCU", "CCC", "CCA", "CCG")),
        THR("T", "Thr", "Threonine ", Arrays.asList("ACU", "ACC", "ACA", "ACG")),
        ALA("A", "Ala", "Alanine", Arrays.asList("GCU", "GCC", "GCA", "GCG")),
        TYR("Y", "Tyr", "Tyrosine", Arrays.asList("UAU", "UAC")),
        STOP("Stop", "Stop", "Stop codon", Arrays.asList("UAA", "UAG", "UGA")),
        HIS("H", "His", "Histidine", Arrays.asList("CAU", "CAC")),
        GLN("Q", "Gln", "Glutamine", Arrays.asList("CAA", "CAG")),
        ASN("N", "Asn", "Asparagine", Arrays.asList("AAU", "AAC")),
        LYS("K", "Lys", "Lysine", Arrays.asList("AAA", "AAG")),
        ASP("D", "Asp", "Aspartic acid", Arrays.asList("GAU", "GAC")),
        GLU("E", "Glu", "Glutamic acid", Arrays.asList("GAA", "GAG")),
        CYS("C", "Cys", "Cysteine", Arrays.asList("UGU", "UGC")),
        TRP("W", "Trp", "Tryptophan", Collections.singletonList("UGG")),
        ARG("R", "Arg", "Arginine", Arrays.asList("CGU", "CGC", "CGA", "CGG", "AGA", "AGG")),
        GLY("G", "Gly", "Glycine", Arrays.asList("GGU", "GGC", "GGA", "GGG"));

        private String title;
        private String extendedTitle;
        private String fullName;
        private List<String> correspondedTriples;

        RnaCodonTable(final String title, final String extendedTitle, final String fullName,
                final List<String> correspondedTriples) {
            this.title = title;
            this.extendedTitle = extendedTitle;
            this.fullName = fullName;
            this.correspondedTriples = correspondedTriples;
        }
    }

    /**
     * Provides reverse complement operation on sequence, represented by List of {@link Sequence}.
     *
     * @param sequences nucleotide sequence
     * @return reversed complement nucleotide sequence, represented by a List of {@link Sequence}
     */
    public static List<Sequence> reverseComplement(final List<Sequence> sequences) {
        List<String> complement = sequences.stream().map(Sequence::getText).map(ProteinSequenceUtils::complement)
                                           .collect(Collectors.toList());
        Collections.reverse(complement);

        List<Sequence> result = new ArrayList<>(sequences.size());
        for (int i = 0; i < sequences.size(); i++) {
            Sequence nucleotide = sequences.get(sequences.size() - 1 -i);
            result.add(new Sequence(nucleotide.getStartIndex(), nucleotide.getEndIndex(), complement.get(i)));
        }

        return result;
    }

    /**
     * Provides reverse complement operation on sequence string
     *
     * @param sequence nucleotide sequence
     * @return reversed complement nucleotide sequence string
     */
    public static String reverseComplement(final String sequence) {
        char[] chars = sequence.toCharArray();
        char[] complementChars = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            complementChars[i] = complement(chars[i]);
        }

        return StringUtils.reverse(new String(complementChars));
    }

    /**
     * Breaks a sequence string into a list of one-base sequences for future computations
     * @param sequenceString a source sequence string
     * @param offset offset from beginning of chromosome
     * @param isNegativeStrand is sequence has negative strand
     * @return
     */
    public static List<Sequence> breakSequenceString(String sequenceString, int offset, boolean isNegativeStrand) {
        List<Sequence> result = new ArrayList<>(sequenceString.length());
        for (int i = 0; i < sequenceString.length(); i++) {
            int pos =
                    isNegativeStrand ? ((offset + sequenceString.length()) - i - 1) : (offset + i);
            Sequence nucleotide = new Sequence(pos, pos, new String(new char[] {sequenceString
                    .charAt(i)}));
            result.add(nucleotide);
        }

        return result;
    }

    /**
     * Convert nucleotide triplet to amino acids, using RNA codon table.
     *
     * @param triplet nucleotide triplet
     * @return corresponded amino acid
     */
    public static String tripletToAminoAcid(final String triplet) {
        validateTriplet(triplet);
        String newTriple = triplet.toUpperCase().replace("T", "U");
        for (RnaCodonTable aminoAcid : RnaCodonTable.values()) {
            if (aminoAcid.correspondedTriples.contains(newTriple)) {
                return aminoAcid.title;
            }
        }

        throw new IllegalArgumentException("Triplet " + triplet + " can not be converted to amino acid. ");
    }

    /**
     * Return complement nucleotide to specified one.
     *
     * @param nucleotide nucleotide
     * @return complement nucleotide
     */
    private static String complement(final String nucleotide) {
        if (StringUtils.isBlank(nucleotide)) {
            throw new IllegalArgumentException(
                    "Complement nucleotide sequence can not be compute for empty sequence. ");
        }
        String complement;
        switch (nucleotide.toUpperCase()) {
            case "A":
                complement = "U";
                break;
            case "T":
            case "U":
                complement = "A";
                break;
            case "C":
                complement = "G";
                break;
            case "G":
                complement = "C";
                break;
            default:
                throw new IllegalArgumentException(MessageHelper.getMessage(MessagesConstants.ERROR_INVALID_NUCLEOTIDE,
                                                                            nucleotide));
        }

        return complement;
    }

    private static char complement(final char nucleotide) {
        char complement;
        switch (Character.toUpperCase(nucleotide)) {
            case 'A':
                complement = 'U';
                break;
            case 'T':
            case 'U':
                complement = 'A';
                break;
            case 'C':
                complement = 'G';
                break;
            case 'G':
                complement = 'C';
                break;
            default:
                throw new IllegalArgumentException(MessageHelper.getMessage(MessagesConstants.ERROR_INVALID_NUCLEOTIDE,
                                                                            nucleotide));
        }

        return complement;
    }

    private static void validateTriplet(final String triple) {
        if (triple == null) {
            throw new IllegalArgumentException(
                "Triple must be specified to perform amino acid computing. Actual: null. ");
        }
        if (triple.length() != NUCLEOTIDES_IN_CODON) {
            throw new IllegalArgumentException(
                "Triple must be specified to perform amino acid computing. Actual: " + triple);
        }
    }
}
