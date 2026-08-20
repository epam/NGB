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
        PHE("F", Arrays.asList("UUU", "UUC")),                                          // Phenylalanine
        LEU("L", Arrays.asList("UUA", "UUG", "CUU", "CUC", "CUA", "CUG")),              // Leucine
        ILE("I", Arrays.asList("AUU", "AUC", "AUA")),                                   // Isoleucine
        MET("M", Collections.singletonList("AUG")),                                     // Methionine
        VAL("V", Arrays.asList("GUU", "GUC", "GUA", "GUG")),                            // Valine
        SER("S", Arrays.asList("UCU", "UCC", "UCA", "UCG", "AGU", "AGC")),              // Serine
        PRO("P", Arrays.asList("CCU", "CCC", "CCA", "CCG")),                            // Proline
        THR("T", Arrays.asList("ACU", "ACC", "ACA", "ACG")),                            // Threonine
        ALA("A", Arrays.asList("GCU", "GCC", "GCA", "GCG")),                            // Alanine
        TYR("Y", Arrays.asList("UAU", "UAC")),                                          // Tyrosine
        STOP("Stop", Arrays.asList("UAA", "UAG", "UGA")),                               // Stop codon
        HIS("H", Arrays.asList("CAU", "CAC")),                                          // Histidine
        GLN("Q", Arrays.asList("CAA", "CAG")),                                          // Glutamine
        ASN("N", Arrays.asList("AAU", "AAC")),                                          // Asparagine
        LYS("K", Arrays.asList("AAA", "AAG")),                                          // Lysine
        ASP("D", Arrays.asList("GAU", "GAC")),                                          // Aspartic acid
        GLU("E", Arrays.asList("GAA", "GAG")),                                          // Glutamic acid
        CYS("C", Arrays.asList("UGU", "UGC")),                                          // Cysteine
        TRP("W", Collections.singletonList("UGG")),                                     // Tryptophan
        ARG("R", Arrays.asList("CGU", "CGC", "CGA", "CGG", "AGA", "AGG")),              // Arginine
        GLY("G", Arrays.asList("GGU", "GGC", "GGA", "GGG"));                            // Glycine

        // The table used to carry two more strings per constant, an "extended title" (the
        // capitalised form of the constant name) and a full amino acid name. Nothing ever read
        // either - the errors that accumulated in them show it: PRO's extended title was "Ser" and
        // THR's full name had a trailing space. The full names are kept above as comments, since
        // that is all they ever were; the extended title is derivable from the constant name.
        private final String title;
        private final List<String> correspondedTriples;

        RnaCodonTable(final String title, final List<String> correspondedTriples) {
            this.title = title;
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
