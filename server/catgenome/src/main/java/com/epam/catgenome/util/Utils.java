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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.util.aws.S3Client;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.QueryParameters;
import com.epam.catgenome.util.db.SortInfo;
import htsjdk.samtools.util.CloseableIterator;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.TribbleException;
import htsjdk.tribble.readers.LineIterator;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epam.catgenome.manager.aws.S3Manager.generateSignedUrl;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * Source:      Utils.java
 * Created:     11/17/15, 5:46 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code Utils} provides miscellaneous util methods.
 * </p>
 */
public final class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private static final double NANO_TO_MILLIS_DENOMINATOR = 1000000.0;
    private static final int RESULT_HASH_SIZE = 6;
    private static final String DELIMITER = "/";
    private static final String GZ_EXTENSION = ".gz";

    private static final int S3_LINK_EXPIRATION = 60;

    private static final String PAGING_INFO_CLAUSE = " limit %s offset %s";
    private static final String WHERE_CLAUSE = " where %s";
    private static final String ORDER_BY_CLAUSE = " order by %s";
    public static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_NUM = 0;
    public static final String NEW_LINE = "\n";

    private Utils() {
        // no operations by default
    }

    /**
     * Parses the original name of a file to remove its extension that matches the given one.
     * <p>
     * It results in StringUtils.trimToNull() applied to <tt>fileName</tt> in the following
     * cases:
     * 1) <tt>fileName</tt> is <tt>null</tt> or consists of whitespaces only;
     * 2) <tt>extension</tt> is <tt>null</tt> or consists of whitespaces only;
     * 3) a call fileName.endsWith(extension) results with <tt>false</tt>; here both
     * <tt>fileName</tt> and <tt>extension</tt> are trimmed before checking.
     * <p>
     * Note:
     * It's expected <tt>extension</tt> starts with a dot that means e.g. '.txt' is correct,
     * but 'txt' is "illegal", because in such case the last dot won't be cut.
     *
     * @param fileName  {@code String} specifies the original filename including extension
     * @param extension {@code String} specifies the extension of a file that should be cut;
     *                  it should start with a dot - '.txt' is correct, 'txt' is illegal
     * @return {@code String}
     */
    public static String removeFileExtension(final String fileName, final String extension) {
        String fn = StringUtils.trimToNull(fileName);
        String ext = StringUtils.trimToNull(extension);
        return fn == null || ext == null || !fn.endsWith(ext)
                ? fn : fn.substring(0, fn.length() - ext.length()).trim();
    }

    /**
     * Makes time for S3 URL access
     * @return a {@link Date} object, representing time for S3 URL access
     */
    public static Date getTimeForS3URL() {
        return DateUtils.addMinutes(new Date(), S3_LINK_EXPIRATION);
    }

    /**
     * @return current system time in milliseconds
     */
    public static Double getSystemTimeMilliseconds() {
        return System.nanoTime() / NANO_TO_MILLIS_DENOMINATOR;
    }

    /**
     * Changes chromosome name, adding "chr" prefix if it wasn't present or removing it if it was
     * @param name chromosome name to change
     * @return a changed chromosome name
     */
    public static String changeChromosomeName(String name) {
        if (name.startsWith(Constants.CHROMOSOME_PREFIX)) {
            return name.substring(Constants.CHROMOSOME_PREFIX.length());
        } else {
            return Constants.CHROMOSOME_PREFIX + name;
        }
    }

    /**
     * Parses a string, representing an array of integers (e.g. [1,2,3]) into an {@link Integer} array
     * @param arrayString an integer array string
     * @return an {@link Integer} array
     */
    public static Integer[] parseIntArray(String arrayString) {
        String[] items = arrayString.replaceAll("\\[", "").replaceAll("\\]", "").split(",");

        Integer[] results = new Integer[items.length];

        for (int i = 0; i < items.length; i++) {
            String numberString = items[i].trim();
            if (NumberUtils.isNumber(numberString)) {
                results[i] = Integer.parseInt(numberString);
            } else {
                return null;
            }
        }

        return results;
    }

    /**
     * Parses a string, representing an array of float numbers (e.g. [1.4,2.12,3.5]) into a {@link Float} array
     * @param arrayString a float array string
     * @return an {@link Float} array
     */
    public static Float[] parseFloatArray(String arrayString) {
        String[] items = arrayString.replaceAll("\\[", "").replaceAll("\\]", "").split(",");

        Float[] results = new Float[items.length];

        for (int i = 0; i < items.length; i++) {
            String numberString = items[i].trim();
            if (NumberUtils.isNumber(numberString)) {
                results[i] = Float.parseFloat(numberString);
            } else {
                return null;
            }
        }

        return results;
    }

    /**
     * Parses a string, representing an array of boolean values (e.g. [true,false]) into a {@link Boolean} array
     * @param arrayString a boolean array string
     * @return an {@link Boolean} array
     */
    public static Boolean[] parseBooleanArray(String arrayString) {
        String[] items = arrayString.replaceAll("\\[", "").replaceAll("\\]", "").split(",");

        Boolean[] results = new Boolean[items.length];

        for (int i = 0; i < items.length; i++) {
            results[i] = Boolean.parseBoolean(items[i].trim());
        }

        return results;
    }

    /**
     * Gets file's extension, including .gz postfix if present
     * @param fileName the name of the file
     * @return file's extension
     */
    public static String getFileExtension(String fileName) {
        String result = fileName;
        boolean compressed = false;
        if (fileName.endsWith(GZ_EXTENSION)) {
            result = fileName.substring(0, fileName.length() - GZ_EXTENSION.length());
            compressed = true;
        }
        result = FilenameUtils.getExtension(result);
        return '.' + (compressed ? (result + GZ_EXTENSION) : result);
    }

    /**
     * Constructs path to a downloaded from it's hash
     * @param hash hash of a downloaded file
     * @return path to file
     */
    public static String getPathFromHash(final String hash) {
        final StringBuilder builder = new StringBuilder(DELIMITER);
        int pos = 0;
        while (pos < hash.length() - 2) {
            builder.append(hash.substring(pos, pos + 2))
                    .append(DELIMITER);
            pos += 2;
        }
        builder.append(hash.substring(pos, hash.length()))
                .append(DELIMITER);
        return builder.toString();
    }

    /**
     * Creates hash for a url of a file to download
     * @param url a url to construct hash
     * @return a url of a file to download
     */
    public static String getHashFromUrlString(final String url) {
        return DigestUtils.md5Hex(url).substring(0, RESULT_HASH_SIZE);
    }

    /**
     * Queries an {@link FeatureReader}, taking into account variations is chromosome naming
     * @param featureReader a reader to query
     * @param chromosomeName a name of a chromosome to query
     * @param start start of the interval to query
     * @param end end of the interval to query
     * @param <T> the type of a {@link FeatureReader}
     * @return {@link CloseableIterator} that represents queried interval
     * @throws IOException if it thrown by reader
     */
    public static <T extends Feature> CloseableIterator<T> query(final FeatureReader<T> featureReader, final String
            chromosomeName, final int start, final int end) throws IOException {
        CloseableIterator<T> iterator = featureReader.query(chromosomeName, start, end);
        if (!iterator.hasNext()) {
            iterator = featureReader.query(Utils.changeChromosomeName(chromosomeName), start, end);
        }

        return iterator;
    }

    /**
     * Queries an {@link AbstractFeatureReader}, taking into account variations is chromosome naming
     * @param featureReader a reader to query
     * @param chromosomeName a name of a chromosome to query
     * @param start start of the interval to query
     * @param end end of the interval to query
     * @param <T> the type of a {@link AbstractFeatureReader}
     * @return {@link CloseableIterator} that represents queried interval
     * @throws IOException if it thrown by reader
     */
    public static <T extends Feature> CloseableIterator<T> query(final AbstractFeatureReader<T, LineIterator>
                     featureReader, final String chromosomeName, final int start, final int end) throws IOException {
        CloseableIterator<T> iterator = featureReader.query(chromosomeName, start, end);
        if (!iterator.hasNext()) {
            iterator = featureReader.query(Utils.changeChromosomeName(chromosomeName), start, end);
        }

        return iterator;
    }

    /**
     * Queries an {@link AbstractFeatureReader}, taking into account variations is chromosome naming
     * @param featureReader a reader to query
     * @param chromosome a chromosome to query
     * @param start start of the interval to query
     * @param end end of the interval to query
     * @param <T> the type of a {@link AbstractFeatureReader}
     * @return {@link CloseableIterator} that represents queried interval
     * @throws IOException if it thrown by reader
     */
    public static <T extends Feature> CloseableIterator<T> query(final AbstractFeatureReader<T, LineIterator>
                    featureReader, final Chromosome chromosome, final int start, final int end) throws IOException {
        return query(featureReader, chromosome.getName(), start, end);
    }

    /**
     * Checks that Block is fully located on Track
     * @param block a block to check
     * @param track a track to check
     * @return true is block is fully located on track
     */
    public static boolean isFullyOnTrack(Block block, Track track) {
        return  !(block.getStartIndex() <= track.getStartIndex() && block.getEndIndex() <= track.getEndIndex()) &&
            !(block.getStartIndex() >= track.getStartIndex() && block.getEndIndex() >= track.getEndIndex());
    }

    /**
     * Helper method to get Chromosome from Map of Chromosome to String chromosome name, taking into account variations
     * in chromosome naming
     * @param chromosomeMap a Map of Chromosome to String chromosome name
     * @param chromosomeName a name of a chromosome
     * @return Chromosome from Map of Chromosome to String chromosome name, taking into account variations
     * in chromosome naming
     */
    public static Chromosome getFromChromosomeMap(Map<String, Chromosome> chromosomeMap, String chromosomeName) {
        return chromosomeMap.containsKey(chromosomeName) ? chromosomeMap.get(chromosomeName) :
               chromosomeMap.get(changeChromosomeName(chromosomeName));
    }

    /**
     * Helper method to check if Map of Chromosome to String chromosome name contains specified String chromosome name,
     * taking into account variations in chromosome naming
     * @param chromosomeMap a Map of Chromosome to String chromosome name
     * @param chromosomeName a name of a chromosome
     * @return true if Chromosome from Map of Chromosome to String chromosome name contains specified chromosome name,
     * taking into account variations in chromosome naming
     */
    public static boolean chromosomeMapContains(Map<String, Chromosome> chromosomeMap, String chromosomeName) {
        return  chromosomeMap.containsKey(chromosomeName) || chromosomeMap.containsKey(
            changeChromosomeName(chromosomeName));
    }

    /**
     * Measure time for a specified task and log it with specified message on debug level
     * @param measuredTask a task to measure time for
     * @param message a message to add to debug information
     */
    public static void debugLogTime(MeasuredTask measuredTask, String message) {
        double time1 = Utils.getSystemTimeMilliseconds();
        measuredTask.doWork();
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug(message + " took {} ms", time2 - time1);
    }

    public static double debugMeasureTime(MeasuredTask measuredTask, String message) {
        double time1 = Utils.getSystemTimeMilliseconds();
        measuredTask.doWork();
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug(message + " took {} ms", time2 - time1);
        return time2 - time1;
    }

    /**
     * Checks if two features of a FeatureFile are sorted
     * @param feature a current feature of a file to check
     * @param lastFeature a previous feature of a file to check
     * @param featureFile a file, thai is being checked
     */
    public static void checkSorted(Feature feature, Feature lastFeature, FeatureFile featureFile) {
        if (feature.getStart() < lastFeature.getStart() && // Check if file is sorted
            lastFeature.getContig().equals(feature.getContig())) {
            throw new TribbleException.MalformedFeatureFile(
                "Input file is not sorted by start position. \n" +
                "We saw a record with a start of " + feature.getContig() + ":" +
                feature.getStart() + " after a record with a start of " +
                lastFeature.getContig() + ":" + lastFeature.getStart(), featureFile.getName());
        }
    }

    /**
     * Creates a temporary {@link FeatureFile} object, that is not registered int the system, but is used for
     * temporary processing, e.g. opening files by URL
     *
     * @param c class of a file to create
     * @param fileUrl a URL of a file
     * @param indexUrl a URL of an index
     * @param chromosome a chromosome, that is being opened
     * @param <T> type of a {@link FeatureFile}
     * @return temporary {@link FeatureFile} object, that is not registered int the system
     * @throws InvocationTargetException
     */
    public static <T extends FeatureFile> T createNonRegisteredFile(Class<T> c, String fileUrl, String indexUrl,
                                                                    Chromosome chromosome)
        throws InvocationTargetException {
        T notRegisteredFile;
        try {
            notRegisteredFile = c.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
            InvocationTargetException e) {
            throw new InvocationTargetException(e, "Cannot instantiate object of class " + c);
        }
        String preprocessedUrl = processUrl(fileUrl);
        notRegisteredFile.setPath(preprocessedUrl);
        notRegisteredFile.setCompressed(false);
        notRegisteredFile.setType(BiologicalDataItemResourceType.getTypeFromPath(preprocessedUrl));
        notRegisteredFile.setReferenceId(chromosome.getReferenceId());

        BiologicalDataItem index = new BiologicalDataItem();
        String preprocessedIndexUrl = processUrl(indexUrl);
        index.setPath(preprocessedIndexUrl);
        index.setType(BiologicalDataItemResourceType.getTypeFromPath(preprocessedIndexUrl));
        notRegisteredFile.setIndex(index);
        return notRegisteredFile;
    }

    public static String processUrl(String inputUrl) {
        if (!S3Client.isS3Source(inputUrl)) {
            return inputUrl;
        }
        return generateSignedUrl(inputUrl);
    }

    public static Map<String, Chromosome> makeChromosomeMap(Reference reference) {
        return reference.getChromosomes().stream().collect(
                Collectors.toMap(BaseEntity::getName, chromosome -> chromosome));
    }

    public static String getUrlWithoutTrailingSlash(String url) {
        return url.endsWith("/") ?
               url.substring(0, url.length() - 1) : url;
    }

    @FunctionalInterface
    public interface MeasuredTask {
        void doWork();
    }

    public static String addParametersToQuery(final String query, final QueryParameters params) {
        return addPagingInfoToQuery(addSortInfoToQuery(addFiltersToQuery(query,
                params.getFilters()), params.getSortInfos()), params.getPagingInfo());
    }

    public static String addFiltersToQuery(final String query, final List<Filter> filters) {
        List<String> filter = new ArrayList<>(Collections.emptyList());
        if (filters != null) {
            for (Filter q : filters) {
                if (StringUtils.isNotBlank(q.getField())
                        && StringUtils.isNotBlank(q.getOperator())
                        && StringUtils.isNotBlank(q.getValue())) {
                    filter.add(q.getField() + " " + q.getOperator() + " " + q.getValue());
                }
            }
        }
        return filter.isEmpty() ? query
                : query + String.format(WHERE_CLAUSE, join(filter, " and "));
    }

    public static String addPagingInfoToQuery(final String query, final PagingInfo pagingInfo) {
        return query + (pagingInfo == null ? "" : String.format(
                PAGING_INFO_CLAUSE,
                pagingInfo.getPageSize() < 1 ? DEFAULT_PAGE_SIZE : pagingInfo.getPageSize(),
                pagingInfo.getPageNum() < 1 ? DEFAULT_PAGE_NUM
                        : (pagingInfo.getPageNum() - 1) * pagingInfo.getPageSize()));
    }

    public static String addSortInfoToQuery(final String query, final List<SortInfo> sortInfos) {
        List<String> orderBy = new LinkedList<>(Collections.emptyList());
        if (sortInfos != null) {
            for (SortInfo sortInfo: sortInfos) {
                if (StringUtils.isNotBlank(sortInfo.getField())) {
                    orderBy.add(sortInfo.getField() + (sortInfo.isAscending() ? " ASC" : " DESC"));
                }
            }
        }
        return orderBy.isEmpty() ? query
                : query + String.format(ORDER_BY_CLAUSE, join(orderBy, ", "));
    }
}
