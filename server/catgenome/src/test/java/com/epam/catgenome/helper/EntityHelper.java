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

package com.epam.catgenome.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.util.AuthUtils;

/**
 * Source:      EntityHelper.java
 * Created:     10/21/15, 6:50 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code EntityHelper} provides miscellaneous methods, which are required to, e.g. instantiate
 * necessary entities used to run tests
 *
 * @author Denis Medvedev
 */
public final class EntityHelper {


    private static final Long REF_BASES_COUNT = 30000L;
    private static final String CHROMOSOME_NAME = "A1";
    private static final int CHROMOSOME_LENGTH = 10000;
    private static final String[] HUMAN_CHROMOSOME_NAMES = {
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13",
        "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y", "MT"
    };

    private static final Chromosome[] HG38_CHROMOSOMES = {
        EntityHelper.createNewChromosome("1", 248956422),
        EntityHelper.createNewChromosome("2", 242193529),
        EntityHelper.createNewChromosome("3", 198295559),
        EntityHelper.createNewChromosome("4", 190214555),
        EntityHelper.createNewChromosome("5", 181538259),
        EntityHelper.createNewChromosome("6", 170805979),
        EntityHelper.createNewChromosome("7", 159345973),
        EntityHelper.createNewChromosome("8", 145138636),
        EntityHelper.createNewChromosome("9", 138394717),
        EntityHelper.createNewChromosome("10", 133797422),
        EntityHelper.createNewChromosome("11", 135086622),
        EntityHelper.createNewChromosome("12", 133275309),
        EntityHelper.createNewChromosome("13", 114364328),
        EntityHelper.createNewChromosome("14", 107043718),
        EntityHelper.createNewChromosome("15", 101991189),
        EntityHelper.createNewChromosome("16", 90338345),
        EntityHelper.createNewChromosome("17", 83257441),
        EntityHelper.createNewChromosome("18", 80373285),
        EntityHelper.createNewChromosome("19", 58617616),
        EntityHelper.createNewChromosome("20", 64444167),
        EntityHelper.createNewChromosome("21", 46709983),
        EntityHelper.createNewChromosome("22", 50818468),
        EntityHelper.createNewChromosome("X", 156040895),
        EntityHelper.createNewChromosome("Y", 57227415),
        EntityHelper.createNewChromosome("MT", 16569)
    };

    private static final int HUMAN_CHROMOSOME_LENGTH = 239107476;

    public static Reference createReference() {
        final Reference reference = new Reference();
        reference.setSize(REF_BASES_COUNT);
        reference.setName("Test.Reference.0.0.1");
        reference.setPath("/contents/tests/references/" + reference.getId());
        reference.setCreatedDate(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
        reference.setCreatedBy(AuthUtils.getCurrentUserId());
        reference.setType(BiologicalDataItemResourceType.FILE);

        final String[] dictionary = new String[]{"A1", "A2", "X"};
        for (String name : dictionary) {
            final Chromosome chromosome = new Chromosome(name, CHROMOSOME_LENGTH);
            chromosome.setPath(String.format("/references/%s/chromosomes/%s/sequences.nib",
                                             reference.getId(), name));
            reference.getChromosomes().add(chromosome);
        }
        return reference;
    }

    public static Chromosome createNewChromosome() {
        return createNewChromosome(CHROMOSOME_NAME, CHROMOSOME_LENGTH);

    }

    public static Chromosome createNewChromosome(String chromosomeName) {
        return createNewChromosome(chromosomeName, CHROMOSOME_LENGTH);
    }

    public static Chromosome createNewChromosome(String chromosomeName, int length) {
        Chromosome chromosome = new Chromosome();
        chromosome.setName(chromosomeName);
        chromosome.setPath("");
        chromosome.setHeader("");
        chromosome.setSize(length);
        return chromosome;
    }

    public static Reference createNewReference(final Chromosome chromosome, final Long referenceId) {
        Reference reference = new Reference();
        reference.setId(referenceId);
        reference.setName("testReference");
        reference.setSize(0L);
        reference.setPath("");
        reference.setCreatedDate(new Date());
        reference.setChromosomes(Collections.singletonList(chromosome));
        return reference;
    }

    public static Reference createNewReference(final List<Chromosome> chromosomes, final Long referenceId) {
        Reference reference = new Reference();
        reference.setId(referenceId);
        reference.setName("testReference");
        reference.setSize(0L);
        reference.setPath("");
        reference.setCreatedDate(new Date());
        reference.setChromosomes(chromosomes);
        return reference;
    }

    public static List<ProteinSequenceEntry> createNewProteinSequences(final int numberOfEntitiesToCreate,
            final long geneId) {
        List<ProteinSequenceEntry> testData = new ArrayList<>(numberOfEntitiesToCreate);
        String text = "A";
        for (int i = 0; i < numberOfEntitiesToCreate; i++) {
            ProteinSequenceEntry ps =
                    new ProteinSequenceEntry(text + i, geneId, (long) i, (long) (i + 10), (long) (i + 1),
                                             (long) (i + 1 + 10));

            testData.add(ps);
        }

        return testData;
    }

    public static BiologicalDataItem createIndex(BiologicalDataItemFormat format, BiologicalDataItemResourceType
            type, String path) {
        BiologicalDataItem index = new BiologicalDataItem();
        index.setName("");
        index.setType(type);
        index.setFormat(format);
        index.setPath(path);
        index.setCreatedDate(new Date());
        index.setCreatedBy(AuthUtils.getCurrentUserId());

        return index;
    }

    public static List<Chromosome> createHumanChromosomes() {
        List<Chromosome> chromosomes = new ArrayList<>(HUMAN_CHROMOSOME_NAMES.length);
        for (String chr : HUMAN_CHROMOSOME_NAMES) {
            chromosomes.add(createNewChromosome(chr, HUMAN_CHROMOSOME_LENGTH));
        }

        return chromosomes;
    }

    public static Reference createG38Reference(Long referenceId) {
        return createNewReference(Arrays.asList(HG38_CHROMOSOMES), referenceId);
    }

    private EntityHelper() {
        // no operations are required by default
    }

}
