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

package com.epam.catgenome.controller.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Source:      MultipartFileSender
 * Created:     28.10.16, 13:18
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A class, that performs sending of a multipart file
 * </p>
 * @author Mikhail Miroliubov
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class MultipartFileSender {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int DEFAULT_BUFFER_SIZE = 20480; // ..bytes = 20MB.
    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.
    private static final int CONSTANT_1000 = 1000;
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    private static final String CONTENT_RANGE_HEADER = "Content-Range";

    Path filepath;

    Request request;
    Response response;

    public MultipartFileSender() {
    }

    public static MultipartFileSender fromPath(Path path) {
        return new MultipartFileSender().setFilepath(path);
    }

    public static MultipartFileSender fromFile(File file) {
        return new MultipartFileSender().setFilepath(file.toPath());
    }

    public static MultipartFileSender fromURIString(String uri) {
        return new MultipartFileSender().setFilepath(Paths.get(uri));
    }

    //** internal setter **//
    private MultipartFileSender setFilepath(Path filepath) {
        this.filepath = filepath;
        return this;
    }

    public MultipartFileSender with(Request httpRequest) {
        request = httpRequest;
        return this;
    }

    public MultipartFileSender with(Response httpResponse) {
        response = httpResponse;
        return this;
    }

    public void serveResource() throws IOException {
        if (response == null || request == null) {
            return;
        }

        if (!Files.exists(filepath)) {
            logger.error("File doesn't exist at URI : {}", filepath.toAbsolutePath());
            Response.writeError(request, response, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Long length = Files.size(filepath);
        String fileName = filepath.getFileName().toString();
        FileTime lastModifiedObj = Files.getLastModifiedTime(filepath);

        if (StringUtils.isEmpty(fileName) || lastModifiedObj == null) {
            Response.writeError(request, response, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        long lastModified = LocalDateTime.ofInstant(lastModifiedObj.toInstant(), ZoneId.of(
                ZoneOffset.systemDefault().getId())).toEpochSecond(ZoneOffset.UTC);
        //String contentType = MimeTypeUtils.probeContentType(filepath);
        String contentType = null;

        // Validate request headers for caching ---------------------------------------------------
        if (!validateHeadersCaching(fileName, lastModified)) {
            return;
        }


        // Validate request headers for resume ----------------------------------------------------
        if (!validateHeadersResume(fileName, lastModified)) {
            return;
        }


        // Validate and process range -------------------------------------------------------------
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = processRange(length, fileName, full);
        if (ranges == null) {
            return;
        }

        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set content disposition.
        String disposition = "inline";

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        } else if (!contentType.startsWith("image")) {
            // Else, expect for images, determine content disposition. If content type is supported by
            // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
            String accept = request.getHeaders().get("Accept");
            disposition = accept != null && HttpUtils.accepts(accept, contentType) ? "inline" : "attachment";
        }
        logger.debug("Content-Type : {}", contentType);
        // Initialize response.

        response.reset();
        response.getHeaders().add("Content-Type", contentType);
        response.getHeaders().add("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        logger.debug("Content-Disposition : {}", disposition);
        response.getHeaders().add("Accept-Ranges", "bytes");
        response.getHeaders().add("ETag", fileName);
        response.getHeaders().add("Last-Modified", lastModified);
        response.getHeaders().add("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);

        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.
        try (InputStream input = new BufferedInputStream(Files.newInputStream(filepath))) {

            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                logger.info("Return full file");
                response.getHeaders().add("Content-Type", contentType);
                response.getHeaders().add(CONTENT_RANGE_HEADER, "bytes " + full.start + "-" + full.end + "/" + full.total);
                response.getHeaders().add("Content-Length", String.valueOf(full.length));
                ByteBuffer buffer = Range.copy(input, length, full.start, full.length);
                response.write(true, buffer, new Callback() {});

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                logger.info("Return 1 part of file : from ({}) to ({})", r.start, r.end);
                response.getHeaders().add("Content-Type", contentType);
                response.getHeaders().add(CONTENT_RANGE_HEADER, "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.getHeaders().add("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Copy single part range.
                ByteBuffer buffer = Range.copy(input, length, r.start, r.length);
                response.write(true, buffer, new Callback() {});

            } else {

                // Return multiple parts of file.
                response.getHeaders().add("Content-Type", "multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Copy multi part range.
                for (Range r : ranges) {
                    logger.info("Return multi part of file : from ({}) to ({})", r.start, r.end);
                    // Add multipart boundary and header fields for every range.
                    response.write(false, BufferUtil.toBuffer("\n"), new Callback() {});
                    response.write(false, BufferUtil.toBuffer("--" + MULTIPART_BOUNDARY), new Callback() {});
                    response.write(false, BufferUtil.toBuffer("Content-Type: " + contentType), new Callback() {});
                    response.write(false, BufferUtil.toBuffer("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total), new Callback() {});

                    // Copy single part range of multi part range.
                    ByteBuffer buffer = Range.copy(input, length, r.start, r.length);
                    response.write(false, buffer, new Callback() {});
                }

                // End with multipart boundary.
                response.write(false, BufferUtil.toBuffer("\n"), new Callback() {});
                response.write(true, BufferUtil.toBuffer("--" + MULTIPART_BOUNDARY + "--"), new Callback() {});
            }

        }
    }

    private boolean validateHeadersResume(String fileName, long lastModified) throws IOException {
        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeaders().get("If-Match");
        if (ifMatch != null && !HttpUtils.matches(ifMatch, fileName)) {
            Response.writeError(request, response, null, HttpServletResponse.SC_PRECONDITION_FAILED);
            return false;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getHeaders().getLongField("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + CONSTANT_1000 <= lastModified) {
            Response.writeError(request, response, null, HttpServletResponse.SC_PRECONDITION_FAILED);
            return false;
        }
        return true;
    }

    private boolean validateHeadersCaching(String fileName, long lastModified) throws IOException {
        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeaders().get("If-None-Match");
        if (ifNoneMatch != null && HttpUtils.matches(ifNoneMatch, fileName)) {
            response.getHeaders().add("ETag", fileName); // Required in 304.
            Response.writeError(request, response, null, HttpServletResponse.SC_NOT_MODIFIED);
            return false;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getHeaders().getLongField("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + CONSTANT_1000 > lastModified) {
            response.getHeaders().add("ETag", fileName); // Required in 304.
            Response.writeError(request, response, null, HttpServletResponse.SC_NOT_MODIFIED);
            return false;
        }
        return true;
    }

    private List<Range> processRange(Long length, String fileName, Range full) throws IOException {
        // Prepare some variables. The full Range represents the complete file.
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeaders().get("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.getHeaders().add(CONTENT_RANGE_HEADER, "bytes */" + length); // Required in 416.
                Response.writeError(request, response, null, HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            String ifRange = request.getHeaders().get("If-Range");
            if (ifRange != null && !ifRange.equals(fileName)) {
                try {
                    long ifRangeTime = request.getHeaders().getLongField("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = Range.sublong(part, 0, part.indexOf('-'));
                    long end = Range.sublong(part, part.indexOf('-') + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        response.getHeaders().add(CONTENT_RANGE_HEADER, "bytes */" + length); // Required in 416.
                        Response.writeError(request, response, null, HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return null;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }

        return ranges;
    }

    private static final class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        private Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

        private static long sublong(String value, int beginIndex, int endIndex) {
            String substring = value.substring(beginIndex, endIndex);
            return substring.length() > 0 ? Long.parseLong(substring) : -1;
        }

        private static ByteBuffer copy(InputStream input, long inputSize, long start, long length)
                throws IOException {
            int intExact = Math.toIntExact(length);
            ByteBuffer resultBuffer = BufferUtil.allocate(intExact);
            resultBuffer.limit(intExact);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

            int read;

            if (inputSize == length) {
                // Write full range.
                while ((read = input.read(buffer)) > 0) {
                    resultBuffer.put(buffer, 0, read);
                    //output.write(buffer, 0, read);
                    //output.flush();
                }
            } else {
                long skipped = input.skip(start);
                Assert.isTrue(skipped == start, "");

                long toRead = length;

                while ((read = input.read(buffer)) > 0) {
                    toRead -= read;
                    if (toRead > 0) {
                        resultBuffer.put(buffer, 0, read);
                        //output.write(buffer, 0, read);
                        //output.flush();
                    } else {
                        resultBuffer.put(buffer, 0, (int) toRead + read);
                        //output.write(buffer, 0, (int) toRead + read);
                        //output.flush();
                        break;
                    }
                }
            }
            resultBuffer.flip();
            return resultBuffer;
        }
    }
    private static class HttpUtils {

        /**
         * Returns true if the given accept header accepts the given value.
         * @param acceptHeader The accept header.
         * @param toAccept The value to be accepted.
         * @return True if the given accept header accepts the given value.
         */
        public static boolean accepts(String acceptHeader, String toAccept) {
            String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
            Arrays.sort(acceptValues);

            return Arrays.binarySearch(acceptValues, toAccept) > -1
                    || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                    || Arrays.binarySearch(acceptValues, "*/*") > -1;
        }

        /**
         * Returns true if the given match header matches the given value.
         * @param matchHeader The match header.
         * @param toMatch The value to be matched.
         * @return True if the given match header matches the given value.
         */
        public static boolean matches(String matchHeader, String toMatch) {
            String[] matchValues = matchHeader.split("\\s*,\\s*");
            Arrays.sort(matchValues);
            return Arrays.binarySearch(matchValues, toMatch) > -1
                    || Arrays.binarySearch(matchValues, "*") > -1;
        }
    }
}
