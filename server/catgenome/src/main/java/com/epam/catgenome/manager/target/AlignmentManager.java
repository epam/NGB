/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.target.UrlEntity;
import com.epam.catgenome.entity.target.AlignmentStatus;
import com.epam.catgenome.entity.target.GeneSequences;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.exception.AlignmentException;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIDataManager;
import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlignmentManager {
    public static final String MUSCLE_COMMAND = "%s -align %s -output %s";
    public static final String ALIGNMENT_FILE_NAME = "%s-%s.fa";
    public static final String EMPTY_LINE = "\n\n";

    @Value("${muscle.path}")
    private String musclePath;

    @Value("${targets.alignment.directory}")
    private String alignmentDirectory;

    private final TargetManager targetManager;
    private final LaunchIdentificationManager launchIdentificationManager;
    private final NCBIDataManager ncbiDataManager;

    public void generateAlignment() {
        final List<Target> targets = targetManager.getTargetsForAlignment();
        for (Target target: targets) {
            target.setAlignmentStatus(AlignmentStatus.IN_PROGRESS);
            targetManager.updateAlignmentStatus(target);
            try {
                process(target);
                target.setAlignmentStatus(AlignmentStatus.ALIGNED);
            } catch (IOException | ParseException | InterruptedException | ExternalDbUnavailableException |
                     AlignmentException e) {
                log.debug(e.getMessage());
                target.setAlignmentStatus(AlignmentStatus.ERROR);
            } finally {
                targetManager.updateAlignmentStatus(target);
            }
        }
    }

    public List<ReferenceSequence> getAlignment(final Long targetId,
                                                final String firstSequenceId,
                                                final String secondSequenceId) {
        final Target target = targetManager.getTarget(targetId);
        Assert.isTrue(target.getAlignmentStatus().equals(AlignmentStatus.ALIGNED), "Sequences are not aligned yet.");
        final String targetDirectory = getTargetDirectory(target.getTargetId());
        final String outputPath = getOutputPath(targetDirectory, firstSequenceId, secondSequenceId);
        final File file = new File(outputPath);
        Assert.isTrue(file.exists(), getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND, outputPath));
        return readFasta(file);
    }

    private List<ReferenceSequence> readFasta(final File fasta) {
        try (FastaSequenceFile file = new FastaSequenceFile(fasta, false)) {
            final List<ReferenceSequence> result = new ArrayList<>();
            ReferenceSequence sequence = file.nextSequence();
            while (sequence != null) {
                result.add(sequence);
                sequence = file.nextSequence();
            }
            return result;
        }
    }

    private void process(final Target target) throws ParseException, IOException,
            InterruptedException, ExternalDbUnavailableException, AlignmentException {
        log.debug("Generating sequence alignment for target {}", target.getTargetName());
        final List<String> geneIds = target.getTargetGenes().stream()
                .map(TargetGene::getGeneId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(geneIds)) {
            return;
        }

        final String targetDirectory = getTargetDirectory(target.getTargetId());
        final File dirFile = new File(targetDirectory);
        if (dirFile.exists()) {
            FileUtils.deleteDirectory(dirFile);
        }
        dirFile.mkdirs();

        final List<GeneSequences> sequences = launchIdentificationManager.getGeneSequences(geneIds);
        final List<String> proteinIds = sequences.stream()
                .map(geneSequences -> geneSequences.getProteins().stream()
                        .map(UrlEntity::getId)
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        final String fasta = getProteinsFasta(proteinIds);
        final Map<String, String> proteins = getFastaMap(fasta);
        for (int i = 0; i < sequences.size(); i++) {
            for (int j = i + 1; j < sequences.size(); j++) {
                alignGenes(proteins, targetDirectory, sequences.get(i), sequences.get(j));
            }
        }
    }

    private void alignGenes(final Map<String, String> sequenceMap, final String targetDirectory,
                            final GeneSequences firstSequence, final GeneSequences secondSequence)
            throws IOException, InterruptedException, AlignmentException {
        final List<String> firstSequenceIds = firstSequence.getProteins().stream()
                .map(UrlEntity::getId)
                .collect(Collectors.toList());
        final List<String> secondSequenceIds = secondSequence.getProteins().stream()
                .map(UrlEntity::getId)
                .collect(Collectors.toList());
        for (String firstSequenceId: firstSequenceIds) {
            for (String secondSequenceId: secondSequenceIds) {
                alignSequences(sequenceMap, targetDirectory, firstSequenceId, secondSequenceId);
            }
        }
    }

    private void alignSequences(final Map<String, String> sequenceMap, final String targetDirectory,
                                final String firstSequenceId, final String secondSequenceId)
            throws IOException, InterruptedException, AlignmentException {
        final File fastaFile = File.createTempFile(UUID.randomUUID().toString(), ".fasta");
        final String outputPath = getOutputPath(targetDirectory, firstSequenceId, secondSequenceId);
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fastaFile.getPath()))) {
                writer.write(sequenceMap.get(firstSequenceId));
                writer.write(EMPTY_LINE);
                writer.write(sequenceMap.get(secondSequenceId));
            }
            runMuscle(fastaFile.toString(), outputPath);
        } finally {
            fastaFile.delete();
        }
    }

    private static Pair<String, String> getSequencesPair(final String firstSequenceId, final String secondSequenceId) {
        return firstSequenceId.compareTo(secondSequenceId) > 0 ?
                Pair.of(secondSequenceId, firstSequenceId) :
                Pair.of(firstSequenceId, secondSequenceId);
    }

    private void runMuscle(final String inputPath, final String outputPath) throws IOException, InterruptedException,
            AlignmentException {
        final String command = String.format(MUSCLE_COMMAND, musclePath, inputPath, outputPath);
        final Process process = Runtime.getRuntime().exec(command);

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            final BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            final StringBuilder result = new StringBuilder();
            String s;
            while ((s = stdError.readLine()) != null) {
                result.append(s);
                result.append('\n');
            }
            throw new AlignmentException(result.toString());
        }
    }

    private String getOutputPath(final String targetDirectory, final String firstSequenceId,
                                 final String secondSequenceId) {
        final Pair<String, String> sequences = getSequencesPair(firstSequenceId, secondSequenceId);
        final String fileName = String.format(ALIGNMENT_FILE_NAME, sequences.getLeft(), sequences.getRight());
        return Paths.get(targetDirectory, fileName).toString();
    }

    private Map<String, String> getFastaMap(final String fasta) {
        final Map<String, String> res = new HashMap<>();
        for (String line : fasta.split(EMPTY_LINE)) {
            res.put(extractSequenceId(line), line);
        }
        return res;
    }

    private String getProteinsFasta(final List<String> proteinIds) throws ExternalDbUnavailableException {
        return ncbiDataManager.fetchTextById("protein", join(proteinIds, ","), "fasta");
    }

    private String extractSequenceId(final String fasta) {
        return fasta.split(" ")[0].replace(">", "").toUpperCase();
    }

    private String getTargetDirectory(final Long targetId) {
        return Paths.get(alignmentDirectory, String.valueOf(targetId)).toString();
    }
}
