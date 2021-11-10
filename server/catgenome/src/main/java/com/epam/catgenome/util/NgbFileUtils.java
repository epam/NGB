package com.epam.catgenome.util;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.constant.MessagesConstants;
import org.apache.commons.io.FilenameUtils;

import com.epam.catgenome.entity.BiologicalDataItemFormat;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * Source:      FileUtils
 * Created:     19.01.17, 17:58
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public final class NgbFileUtils {
    protected static final List<BiologicalDataItemFormat> ALLOWED_BROWSING_FORMATS = Arrays.asList(
        BiologicalDataItemFormat.VCF, BiologicalDataItemFormat.VCF_INDEX, BiologicalDataItemFormat.BAM,
        BiologicalDataItemFormat.BAM_INDEX, BiologicalDataItemFormat.INDEX,
        BiologicalDataItemFormat.BED, BiologicalDataItemFormat.BED_INDEX,
        BiologicalDataItemFormat.GENE, BiologicalDataItemFormat.GENE_INDEX);

    private static final String[] SUPPORTED_EXTENSIONS = {".fasta", ".fasta.gz", ".fa", ".fa.gz", ".fna, " +
                                                          ".fna.gz", ".txt", ".txt.gz",
                                                          ".vcf", ".vcf.gz",
                                                          ".gff.gz", ".gtf.gz", ".gff", ".gtf",
                                                          ".gff3", ".gff3.gz", ".bam", ".bai", ".bed", ".bed.gz",
                                                          ".seg", ".seg.gz", ".maf", ".maf.gz", ".bw", "bdg", "bg",
                                                          "bedGraph", "bdg.gz", "bg.gz", "bedGraph.gz", ".tbi", ".idx"};

    public static final String GZ_EXTENSION = ".gz";
    public static final String TBI_EXTENSION = ".tbi";
    public static final String IDX_EXTENSION = ".idx";

    private static final Map<String, BiologicalDataItemFormat> FORMAT_MAP = new LinkedHashMap<>();
    static {
        FORMAT_MAP.put(".fasta", BiologicalDataItemFormat.REFERENCE);
        FORMAT_MAP.put(".fasta.gz", BiologicalDataItemFormat.REFERENCE);
        FORMAT_MAP.put(".fa", BiologicalDataItemFormat.REFERENCE);
        FORMAT_MAP.put(".fa.gz", BiologicalDataItemFormat.REFERENCE);
        FORMAT_MAP.put(".fna", BiologicalDataItemFormat.REFERENCE);
        FORMAT_MAP.put(".fna.gz", BiologicalDataItemFormat.REFERENCE);
        FORMAT_MAP.put(".txt", BiologicalDataItemFormat.REFERENCE);
        FORMAT_MAP.put(".txt.gz", BiologicalDataItemFormat.REFERENCE);

        FORMAT_MAP.put(".vcf", BiologicalDataItemFormat.VCF);
        FORMAT_MAP.put(".vcf.gz", BiologicalDataItemFormat.VCF);
        FORMAT_MAP.put(".vcf.idx", BiologicalDataItemFormat.VCF_INDEX);
        FORMAT_MAP.put(".vcf.gz.tbi", BiologicalDataItemFormat.VCF_INDEX);

        FORMAT_MAP.put(".gff.gz", BiologicalDataItemFormat.GENE);
        FORMAT_MAP.put(".gtf.gz", BiologicalDataItemFormat.GENE);
        FORMAT_MAP.put(".gff3.gz", BiologicalDataItemFormat.GENE);
        FORMAT_MAP.put(".gff3", BiologicalDataItemFormat.GENE);
        FORMAT_MAP.put(".gtf", BiologicalDataItemFormat.GENE);
        FORMAT_MAP.put(".gff", BiologicalDataItemFormat.GENE);
        FORMAT_MAP.put(".gff.gz.tbi", BiologicalDataItemFormat.GENE_INDEX);
        FORMAT_MAP.put(".gtf.gz.tbi", BiologicalDataItemFormat.GENE_INDEX);
        FORMAT_MAP.put(".gff3.gz.tbi", BiologicalDataItemFormat.GENE_INDEX);
        FORMAT_MAP.put(".gff3.tbi", BiologicalDataItemFormat.GENE_INDEX);
        FORMAT_MAP.put(".gtf.tbi", BiologicalDataItemFormat.GENE_INDEX);
        FORMAT_MAP.put(".gff.tbi", BiologicalDataItemFormat.GENE_INDEX);

        FORMAT_MAP.put(".bam", BiologicalDataItemFormat.BAM);
        FORMAT_MAP.put(".bai", BiologicalDataItemFormat.BAM_INDEX);

        FORMAT_MAP.put(".bed", BiologicalDataItemFormat.BED);
        FORMAT_MAP.put(".bed.gz", BiologicalDataItemFormat.BED);
        FORMAT_MAP.put(".bed.tbi", BiologicalDataItemFormat.BED_INDEX);
        FORMAT_MAP.put(".bed.gz.tbi", BiologicalDataItemFormat.BED_INDEX);

        FORMAT_MAP.put(".seg", BiologicalDataItemFormat.SEG);
        FORMAT_MAP.put(".seg.gz", BiologicalDataItemFormat.SEG);
        FORMAT_MAP.put(".seg.tbi", BiologicalDataItemFormat.SEG_INDEX);
        FORMAT_MAP.put(".seg.gz.tbi", BiologicalDataItemFormat.BED_INDEX);

        FORMAT_MAP.put(".maf", BiologicalDataItemFormat.MAF);
        FORMAT_MAP.put(".maf.gz", BiologicalDataItemFormat.MAF);
        FORMAT_MAP.put(".maf.tbi", BiologicalDataItemFormat.MAF_INDEX);
        FORMAT_MAP.put(".maf.gz.tbi", BiologicalDataItemFormat.MAF_INDEX);

        FORMAT_MAP.put(".bw", BiologicalDataItemFormat.WIG);
        FORMAT_MAP.put(".bdg", BiologicalDataItemFormat.WIG);
        FORMAT_MAP.put(".bg", BiologicalDataItemFormat.WIG);
        FORMAT_MAP.put(".bedGraph", BiologicalDataItemFormat.WIG);
        FORMAT_MAP.put(".bdg.idx", BiologicalDataItemFormat.BED_GRAPH_INDEX);
        FORMAT_MAP.put(".bg.idx", BiologicalDataItemFormat.BED_GRAPH_INDEX);
        FORMAT_MAP.put(".bedGraph.idx", BiologicalDataItemFormat.BED_GRAPH_INDEX);
        FORMAT_MAP.put(".bdg.gz.tbi", BiologicalDataItemFormat.BED_GRAPH_INDEX);
        FORMAT_MAP.put(".bg.gz.tbi", BiologicalDataItemFormat.BED_GRAPH_INDEX);
        FORMAT_MAP.put(".bedGraph.gz.tbi", BiologicalDataItemFormat.BED_GRAPH_INDEX);
        FORMAT_MAP.put(".bdg.tbi", BiologicalDataItemFormat.BED_GRAPH_INDEX);
        FORMAT_MAP.put(".bg.tbi", BiologicalDataItemFormat.BED_GRAPH_INDEX);
        FORMAT_MAP.put(".bedGraph.tbi", BiologicalDataItemFormat.BED_GRAPH_INDEX);


        FORMAT_MAP.put(".tbi", BiologicalDataItemFormat.INDEX);
        FORMAT_MAP.put(".idx", BiologicalDataItemFormat.INDEX);
    }

    private static final Set<String> EMPTY_CELL_VALUES = new HashSet<>();

    static {
        EMPTY_CELL_VALUES.add(".");
        EMPTY_CELL_VALUES.add("-");
    }

    private NgbFileUtils() {
        // no-op
    }

    public static String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    public static boolean isFileSupported(String fileName) {
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    public static BiologicalDataItemFormat getFormatByExtension(String fileName) {
        return FORMAT_MAP.get(getFileExtension(fileName));
    }

    /**
     * Gets file's extension, including .gz, .tbi or .idx postfix if present
     * @param fileName the name of the file
     * @return file's extension
     */
    public static String getFileExtension(String fileName) {
        if (fileName.endsWith(GZ_EXTENSION) || fileName.endsWith(TBI_EXTENSION) || fileName.endsWith(IDX_EXTENSION)) {
            String extension = FilenameUtils.getExtension(fileName);
            return getFileExtension(fileName.substring(0, fileName.length() - extension.length() - 1)) + "." +
                   extension;
        }

        String extension = FilenameUtils.getExtension(fileName);
        return  extension.length() > 0 && FORMAT_MAP.containsKey("." + extension) ? "." + extension : "";
    }

    public static boolean isFileBrowsingAllowed(BiologicalDataItemFormat format) {
        return ALLOWED_BROWSING_FORMATS.contains(format);
    }

    public static boolean isGzCompressed(String fileName) {
        return fileName.endsWith(GZ_EXTENSION);
    }

    public static boolean isRemotePath(String path) {
        return path.startsWith("http:") || path.startsWith("https:") || path.startsWith("ftsp:");
    }

    public static String getCellValue(final String value) {
        return (TextUtils.isBlank(value) || EMPTY_CELL_VALUES.contains(value.trim())) ? null : value.trim();
    }

    @NotNull
    public static File getFile(final String path) {
        Assert.isTrue(!TextUtils.isBlank(path), getMessage(MessagesConstants.PATH_IS_REQUIRED));
        final File file = new File(path);
        Assert.isTrue(file.isFile() && file.canRead(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        return file;
    }

    public static String getBioDataItemName(final String name, final String filePath) {
        return TextUtils.isBlank(name) ? FilenameUtils.getBaseName(filePath) : name;
    }
}
