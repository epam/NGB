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

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.gene.writer.Gff3FeatureImpl;
import com.epam.catgenome.manager.gene.writer.Gff3Writer;
import com.epam.catgenome.manager.reference.io.FastaUtils;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.DBReferenceInfo;
import org.biojava.nbio.core.sequence.features.FeatureInterface;
import org.biojava.nbio.core.sequence.features.Qualifier;
import org.biojava.nbio.core.sequence.io.FastaWriterHelper;
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.biojava.nbio.genome.parsers.gff.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@Slf4j
public class GenbankManager {

    @Autowired private FileManager fileManager;

    private static final String ID_ATTR = "ID";
    private static final String DBXREF_ATTR = "Dbxref";
    private static final String NOTE_ATTR = "Note";
    private static final String NAME_ATTR = "Name";

    private static final String SOURCE = "GenBank";

    private static final String PSEUDO = "pseudo";
    private static final String LOCUS_TAG = "locus_tag";
    private static final String OPERON = "operon";
    private static final String CODON_START = "codon_start";
    private static final String GENE = "gene";
    private static final String PSEUDOGENE = "pseudogene";

    private static final String DB_XREF_SEPARATOR = ":";
    private static final String LOCATION_SEPARATORS_REGEX = "\\..|,";
    private static final String DOT = ".";

    @SneakyThrows
    public Map<String, DNASequence> readGenbankFile(final String genbankFilePath) {
        Assert.notNull(genbankFilePath, getMessage(MessageCode.RESOURCE_NOT_FOUND));
        try (SeekableStream genBankStream = SeekableStreamFactory.getInstance().getStreamFor(genbankFilePath)) {
            return GenbankReaderHelper.readGenbankDNASequence(genBankStream);
        }
    }

    public boolean hasFeatures(final Map<String, DNASequence> dnaSequences) {
        for (Map.Entry<String, DNASequence> sequence : dnaSequences.entrySet()) {
            if (!sequence.getValue().getFeatures().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @SneakyThrows
    public void genbankToGff(final String genebankFilePath, final Path gffFilePath) {
        Map<String, DNASequence> dnaSequences = readGenbankFile(genebankFilePath);
        Assert.isTrue(!dnaSequences.isEmpty(), getMessage(MessageCode.ERROR_GENBANK_FILE_READING));
        try (Gff3Writer gff3Writer = new Gff3Writer(gffFilePath)) {
            for (Map.Entry<String, DNASequence> sequence : dnaSequences.entrySet()) {
                String seqId = sequence.getValue().getAccession().getID();
                Map<String, Integer> ids = new HashMap<>();
                List<String> value;
                int idValue;
                for (FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> f :
                        sequence.getValue().getFeatures()) {
                    Map<String, List<Qualifier>> qualifiers = f.getQualifiers();
                    String type = GeneType.getType(f.getType(), qualifiers.containsKey(PSEUDO));
                    Map<String, List<String>> attributes = new LinkedHashMap<>();
                    if (type.equals(OPERON) && qualifiers.containsKey(OPERON)) {
                        value = getQualifierValue(qualifiers, OPERON);
                    } else if ((type.equals(GENE) || type.equals(PSEUDOGENE)) &&
                            qualifiers.containsKey(LOCUS_TAG)) {
                        value = getQualifierValue(qualifiers, LOCUS_TAG);
                    } else {
                        String idKey = (qualifiers.containsKey(LOCUS_TAG) ?
                                qualifiers.get(LOCUS_TAG).get(0).getValue() :
                                seqId) + DOT + type;
                        idValue = 0;
                        if (ids.containsKey(idKey)) {
                            idValue = ids.get(idKey) + 1;
                            value = Collections.singletonList(idKey + idValue);
                        } else {
                            value = Collections.singletonList(idKey);
                        }
                        ids.put(idKey, idValue);
                    }
                    attributes.put(ID_ATTR, value);
                    if (f.getQualifiers().containsKey(GENE)) {
                        attributes.put(NAME_ATTR, getQualifierValue(qualifiers, GENE));
                    } else if (qualifiers.containsKey(LOCUS_TAG)) {
                        attributes.put(NAME_ATTR, getQualifierValue(qualifiers, LOCUS_TAG));
                    }
                    attributes.putAll(qualifiersToAttr(qualifiers));

                    Gff3FeatureImpl feature = new Gff3FeatureImpl(
                            seqId.isEmpty() ? sequence.getValue().getOriginalHeader() : seqId,
                            SOURCE,
                            type,
                            parseFeatureLocation(f.getSource()).getBegin(),
                            parseFeatureLocation(f.getSource()).getEnd(),
                            DOT,
                            StrandSerializable.forValue(f.getLocations().getStrand().getStringRepresentation()),
                            attributes.containsKey(CODON_START) ?
                                    getQualifierValue(qualifiers, CODON_START).get(0) + 1 : DOT,
                            attributes
                    );
                    try {
                        gff3Writer.addFeature(feature);
                    } catch (IOException e) {
                        log.debug(e.getMessage(), e);
                    }
                }
            }
        }
    }

    @SneakyThrows
    public String genbankToFasta(final Reference reference, final Map<String, DNASequence> dnaSequences) {
        String referenceDir = fileManager.getReferenceDir(reference);
        Assert.notNull(referenceDir, getMessage(MessageCode.RESOURCE_NOT_FOUND));
        File dnaFileFasta = new File(referenceDir, reference.getName() + FastaUtils.DEFAULT_FASTA_EXTENSION);
        FastaWriterHelper.writeNucleotideSequence(dnaFileFasta, dnaSequences.values());
        return dnaFileFasta.getPath();
    }

    private Map<String, List<String>> qualifiersToAttr(final Map<String, List<Qualifier>> qualifiers) {
        final Map<String, List<String>> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, List<Qualifier>> qualifier : qualifiers.entrySet()) {
            List<String> values = qualifier.getValue()
                    .stream()
                    .map(q -> q.getValue().isEmpty() ? Boolean.toString(true) : q.getValue())
                    .collect(Collectors.toList());
            switch (qualifier.getKey()) {
                case "db_xref":
                    attributes.put(DBXREF_ATTR, qualifier.getValue()
                        .stream()
                        .map(q -> ((DBReferenceInfo) q).
                                getDatabase() + DB_XREF_SEPARATOR + ((DBReferenceInfo) q).getId())
                        .collect(Collectors.toList()));
                    break;
                case "note":
                    attributes.put(NOTE_ATTR, values);
                    break;
                default:
                    attributes.put(qualifier.getKey(), values);
                    break;
            }
        }
        return attributes;
    }

    private List<String> getQualifierValue(final Map<String, List<Qualifier>> qualifiers, final String key) {
        Assert.isTrue(qualifiers.containsKey(key), getMessage(MessageCode.ERROR_NO_QUALIFIERS));
        return qualifiers.get(key).stream().map(Qualifier::getValue).collect(Collectors.toList());
    }


    private Location parseFeatureLocation(final String source) {
        final String[] replacements = {"join(", ")", "complement(", "<", ">"};
        final String[] replaceBy = {"", "", "", "", ""};
        final String[] regions = StringUtils.replaceEach(source, replacements, replaceBy).
                split(LOCATION_SEPARATORS_REGEX);
        return new Location(Integer.parseInt(regions[0]), Integer.parseInt(regions[regions.length - 1]));
    }
}
