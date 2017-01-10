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

package com.epam.catgenome.manager.reference;

import static com.epam.catgenome.component.MessageCode.RESOURCE_NOT_FOUND;
import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.cytoband.Cytoband;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.reference.io.cytoband.CytobandReader;
import com.epam.catgenome.manager.reference.io.cytoband.CytobandRecord;

/**
 * {@code CytobandManager} represents a service class designed to encapsulate all business
 * logic operations required to manage cytological cards for reference genomes, e.g. to
 * process cytobands uploads, retrieving cytological card for a particular chromosome etc.
 */
@Service
public class CytobandManager {

    /**
     * {@code String} specifies the format for a single line in cytobands file
     */
    private static final String FMT_CYTOBAND_ROW = "%s\t%d\t%d\t%s\t%s";

    @Autowired
    private FileManager fileManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    /**
     * Returns {@code Track} with cytobands if they're available for a chromosome with
     * the given ID, otherwise returns <tt>null</tt>.
     *
     * @param chromosomeId {@code Long} represents Chromosome ID which cytobands should
     *                     be loaded
     * @return {@code Track} describes cytological card if it's available or <tt>null</tt>
     * otherwise
     * @throws IOException will be thrown if any I/O errors occur
     */
    public Track<Cytoband> loadCytobands(final Long chromosomeId) throws IOException {
        // loads chromosome data by the given ID
        final Chromosome chromosome = referenceGenomeManager.loadChromosome(chromosomeId);
        // tries to find a file that provides cytobands for the given chromosome
        final File cytobandFile = fileManager.makeCytobandsFile(chromosome);
        if (!cytobandFile.exists()) {
            return null;
        }
        // loads cytobands from a file, skipping validation, because it was done earlier
        final CytobandReader reader = CytobandReader.getInstance(cytobandFile);
        final CytobandRecord record = reader.getRecord(chromosome.getName());
        if (record == null || CollectionUtils.isEmpty(record.getBands())) {
            return null;
        }
        // creates and fills a new track that describes cytobands
        final Track<Cytoband> track = new Track<>(TrackType.CYTOBAND);
        track.setStartIndex(1);
        track.setScaleFactor(1D);
        track.setId(chromosomeId);
        track.setChromosome(chromosome);
        track.setBlocks(record.getBands());
        track.setName(chromosome.getName());
        track.setEndIndex(chromosome.getSize());
        return track;
    }

    /**
     * Parses, validates and saves cytobands pointing to a particular reference genome. The
     * cytological cards will be separated between chromosomes.
     *
     * @param referenceId {@code Long} specifies genome ID that should be pointed to the given
     *                    cytobands
     * @param file        {@code File} represents a reference on a file that provides cytobands
     * @throws IOException              will be thrown if any I/O errors occur
     * @throws IllegalArgumentException will be thrown in cases when a given file isn't well-formed or
     *                                  there is at least one band that doesn't provide mandatory values
     */
    public void saveCytobands(final Long referenceId, final File file) throws IOException {
        boolean succeeded = false;
        final List<File> cytobandsFiles = new LinkedList<>();
        try {
            // prepares data about corresponded reference genome and parses cytobands, if
            // the given resource is available
            Assert.isTrue(file != null && file.exists(), getMessage(RESOURCE_NOT_FOUND));
            final Reference reference = referenceGenomeManager.loadReferenceGenome(referenceId);
            // collects names of all chromosomes to force cytobands validation
            final HashSet<String> dictionary = reference.getChromosomes().stream().map(Chromosome::getName)
                .collect(Collectors.toCollection(HashSet::new));
            final CytobandReader reader = CytobandReader.getInstance(file, dictionary);

            // saves cytobands in the system relatively to corresponded chromosomes
            for (final Chromosome chromosome : reference.getChromosomes()) {
                final CytobandRecord record = reader.getRecord(chromosome.getName());
                processCytobandRecord(cytobandsFiles, chromosome, record);

            }
            // sets this flag to 'true' that means all activities are performed successfully and no
            // rollback for applied changes are required
            succeeded = true;
        } finally {
            // reverts all changes that have been made in the file system, if something was going wrong
            // and we cannot create a genome in the system)
            if (!succeeded) {
                cytobandsFiles.forEach(FileUtils::deleteQuietly);
            }
            // removes an original temporary file that provides data for cytobands
            FileUtils.deleteQuietly(file);
        }
    }

    private void processCytobandRecord(List<File> cytobandsFiles, Chromosome chromosome,
            CytobandRecord record) throws IOException {
        // skips chromosome if no cytobands are available for it
        if (record != null) {
            final File cytobandsFile = fileManager.makeCytobandsFile(chromosome);
            if (!cytobandsFile.exists()) {
                // if no file was created before, then in case of rollback an empty
                // file should be deleted too
                cytobandsFiles.add(cytobandsFile);
            }
            // writes cytobands to a file,
            // Note:
            // In the system we handle all positions following assumption that the first
            // base has position 1, but cytobands files are usually used zero-based position
            // notation, so here we increase start/end index on 1 to be compatible with
            // other tracks
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(cytobandsFile), Charset.defaultCharset()))) {
                for (final Cytoband band : record.getBands()) {
                    writer.write(String.format(FMT_CYTOBAND_ROW,
                        band.getChromosome(),
                        band.getStartIndex() + 1,
                        band.getEndIndex() + 1,
                        band.getName(),
                        band.getGiemsaStain().value()
                    ));
                    writer.newLine();
                }
            }
        }
    }

}
