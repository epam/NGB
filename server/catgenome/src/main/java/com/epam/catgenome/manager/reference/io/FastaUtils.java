package com.epam.catgenome.manager.reference.io;

import static htsjdk.samtools.util.HttpUtils.getHeaderField;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.epam.catgenome.util.NgbFileUtils;
import htsjdk.tribble.readers.AsciiLineReader;
import org.apache.log4j.Logger;

public final class FastaUtils {

    private static final Logger LOG = Logger.getLogger(FastaUtils.class);
    protected static final Set<String> FASTA_EXTENSIONS = new HashSet<>();
    static {
        FASTA_EXTENSIONS.add(".fasta");
        FASTA_EXTENSIONS.add(".fa");
        FASTA_EXTENSIONS.add(".fna");
    }
    private static final int TIMEOUT = 3000;
    public static final String FASTA_INDEX = ".fai";
    private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");
    public static final Pattern SEQUENCE_NAME_SPLITTER = Pattern.compile("\\s+");

    private FastaUtils() {
        //utility class
    }

    public static BufferedReader openBufferedReader(String pathOrUrl) throws IOException {
        InputStream inputStream;
        if (NgbFileUtils.isRemotePath(pathOrUrl)) {
            URL url = new URL(pathOrUrl);
            inputStream = openConnection(url).getInputStream();
        } else {
            inputStream = new FileInputStream(pathOrUrl);
        }
        return new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
    }

    private static URLConnection openConnection(final URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setReadTimeout(TIMEOUT);
        conn.setDefaultUseCaches(false);
        conn.setUseCaches(false);
        return conn;
    }

    public static Set<String> getFastaExtensions() {
        return FASTA_EXTENSIONS;
    }

    public static boolean isFasta(String path) {
        return FASTA_EXTENSIONS.stream().anyMatch(path::endsWith);
    }


    public static long getContentLength(String path) {
        try {
            long contentLength;
            if (NgbFileUtils.isRemotePath(path)) {
                URL url = new URL(path);
                contentLength = getContentLength(url);
            } else {
                contentLength = (new File(path)).length();
            }
            return contentLength;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return -1;
        }
    }

    private static long getContentLength(URL url) throws IOException {
        String contentLengthString = getHeaderField(url, "Content-Length");
        if (contentLengthString == null) {
            return -1;
        } else {
            return Long.parseLong(contentLengthString);
        }
    }

    /**
     * Creates fasta index near original reference
     * @param fasta
     */
    public static void indexFasta(File fasta) {
        indexFasta(fasta, null);
    }
    /**
     * Helper method for handling an issue with fasta index for difference OS
     * @param fasta file to index
     */
    public static void indexFasta(File fasta, File index) {
        File indexFile = index;
        if (indexFile == null) {
            indexFile = new File(fasta.getAbsolutePath() + ".fai");
        }
        try (AsciiLineReader reader = new AsciiLineReader(new FileInputStream(fasta));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(indexFile), Charset.defaultCharset()))){
            String line;
            String curContig = null;
            Set<String> allContigs = new HashSet<>();
            int basesPerLine = -1;
            int bytesPerLine = -1;
            long location = 0;
            long size = 0;
            long lastPosition = 0;

            int basesThisLine;
            int bytesThisLine;
            int numInconsistentLines = -1;
            //Number of blank lines in the current contig.
            //-1 for not set
            int numBlanks = -1;
            //We loop through, generating a new FastaSequenceIndexEntry
            //every time we see a new header line, or when the file ends.
            while (true) {
                line = reader.readLine();

                if (line == null || line.startsWith(">")) {
                    //The last line can have a different number of bases/bytes
                    if (numInconsistentLines >= 2) {
                        throw new IllegalArgumentException("Input file must have lines of the "
                                + "equal length.");
                    }
                    //Done with old contig
                    if (curContig != null) {
                        writeLine(writer, curContig, size, location, basesPerLine, bytesPerLine);
                    }
                    if (line == null) {
                        break;
                    }
                    //Header line
                    curContig = WHITE_SPACE.split(line)[0];
                    curContig = curContig.substring(1);
                    if (allContigs.contains(curContig)) {
                        throw new IllegalArgumentException(String.format("Input file must have contiguous chromosomes. "
                                + "We saw contig %s twice in the reference file.", curContig));
                    } else {
                        allContigs.add(curContig);
                    }
                    //Should be starting position of next line
                    location = reader.getPosition();
                    size = 0;
                    basesPerLine = -1;
                    bytesPerLine = -1;
                    numInconsistentLines = -1;
                } else {
                    basesThisLine = line.length();
                    bytesThisLine = (int) (reader.getPosition() - lastPosition);
                    //Calculate stats per line if first line, otherwise
                    //check for consistency
                    if (numInconsistentLines < 0) {
                        basesPerLine = basesThisLine;
                        bytesPerLine = bytesThisLine;
                        numInconsistentLines = 0;
                        numBlanks = 0;
                    } else {
                        if ((basesPerLine != basesThisLine || bytesPerLine != bytesThisLine) && basesThisLine > 0) {
                            numInconsistentLines++;
                        }
                    }
                    //Empty line. This is allowed if it's at the end of the contig)
                    if (basesThisLine == 0) {
                        numBlanks++;
                    } else if (numBlanks >= 1) {
                        throw new IllegalArgumentException("Empty lines are not allowed in the contings.");
                    }
                    size += basesThisLine;
                }
                lastPosition = reader.getPosition();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void writeLine(Writer writer, String contig, long size, long location,
            int basesPerLine, int bytesPerLine) throws IOException {
        String delim = "\t";
        String line = contig + delim + size + delim + location + delim + basesPerLine + delim + bytesPerLine;
        writer.write(line);
        //We infer the newline character based on bytesPerLine - basesPerLine
        //Fasta file may not have been created on this platform, want to keep the index and fasta file consistent
        String newline = "\n";
        if (bytesPerLine - basesPerLine == 2) {
            newline = "\r\n";
        }
        writer.write(newline);
    }

    public static boolean hasIndex(String path) {
        File index = new File(path + FASTA_INDEX);
        return index.exists() && index.canRead();
    }
}
