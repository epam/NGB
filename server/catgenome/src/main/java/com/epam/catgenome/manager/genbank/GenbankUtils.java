/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.manager.genbank;

import org.apache.commons.lang3.StringUtils;
import org.biojava.nbio.core.sequence.AccessionID;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.DNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.io.DNASequenceCreator;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.biojava.nbio.core.sequence.template.Compound;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GenbankUtils {

    public static final String GENBANK_DEFAULT_EXTENSION = ".gbk";
    private static final Set<String> GENBANK_EXTENSIONS = new HashSet<>();
    private static final String TAB_DELIMITER = "[ |\t]+";
    private static final String EMPTY = "";

    static {
        GENBANK_EXTENSIONS.add(".genbank");
        GENBANK_EXTENSIONS.add(".gb");
        GENBANK_EXTENSIONS.add(".gbf");
        GENBANK_EXTENSIONS.add(".gbk");
    }

    private GenbankUtils() {
        //utility class
    }

    public static Set<String> getGenbankExtensions() {
        return GENBANK_EXTENSIONS;
    }

    public static boolean isGenbank(String path) {
        return GENBANK_EXTENSIONS.stream().anyMatch(path::endsWith);
    }

    public static Map<String, DNASequence> readGenbankDNASequence(
            InputStream inStream) throws Exception {
        NGBGenbankReader<DNASequence, NucleotideCompound> genbankReader = new NGBGenbankReader<>(
                inStream,
                new DNASequenceCreator(DNACompoundSet.getDNACompoundSet()));
        return genbankReader.process();
    }

    public static <C extends Compound> String getSequenceId(final AbstractSequence<C> sequence) {
        return Optional.ofNullable(sequence.getAccession())
                .map(AccessionID::getID)
                .filter(StringUtils::isNotBlank)
                .orElseGet(
                        () -> GenbankUtils.parseSequenceNameFromHeader(sequence.getOriginalHeader())
                );
    }

    private static String parseSequenceNameFromHeader(final String header) {
        return StringUtils.isNotBlank(header) ? header.trim().split(TAB_DELIMITER)[0].trim() : EMPTY;
    }
}
