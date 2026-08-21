/*
 * MIT License
 *
 * Copyright (c) 2026 EPAM Systems
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

package com.epam.catgenome.util.aws;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Reads objects that end where the caller says they end.
 *
 * <p>{@link com.epam.catgenome.util.FeatureInputStream}, which is where all of the chunking lives,
 * used to signal the end of an object with a one-byte buffer holding {@code -1} - and then hand that
 * byte back through {@code & 0xff}, which turned it into 255. A read past the last byte therefore
 * produced 255s forever instead of an end of stream, and it also dropped the last byte whenever the
 * tail of the object was exactly one byte long. {@link #testReadsBgzipObjectRegisteredFromS3()} is
 * the failure that made it visible: htsjdk reads the trailing bgzip block's ISIZE field as four
 * bytes little-endian, four 255s are {@code 0xFFFFFFFF}, and every bgzip'd file in cloud storage was
 * refused at registration time with "invalid uncompressedLength: -1".
 *
 * <p>The mock stands in for S3 at the {@link S3Client#loadFromTo(String, long, long)} boundary, and
 * honours its inclusive-range contract, so the ranges these tests assert on are the ones a real
 * bucket would be asked for.
 */
public class S3ObjectChunkInputStreamTest {

    /** {@code FeatureInputStream.CHUNK_SIZE}, which is protected and in another package. */
    private static final int CHUNK_SIZE = 64 * 1024;
    private static final String URI = "s3://bucket/object";

    @Test
    public void testReadsObjectSpanningSeveralChunksToItsExactEnd() throws IOException {
        final byte[] object = anObjectOf(3 * CHUNK_SIZE + 12345);
        final List<long[]> ranges = new ArrayList<>();

        final byte[] read = readFully(object, ranges);

        assertArrayEquals(object, read);
        // inclusive ranges, contiguous, and the last one stops on the last byte of the object
        assertEquals(4, ranges.size());
        long expectedFrom = 0;
        for (final long[] range : ranges) {
            assertEquals(expectedFrom, range[0]);
            expectedFrom = range[1] + 1;
        }
        assertEquals(object.length, expectedFrom);
    }

    @Test
    public void testReadsObjectWhoseTailIsExactlyOneByte() throws IOException {
        // the first chunk covers [0, CHUNK_SIZE], so an object of CHUNK_SIZE + 2 bytes leaves a
        // single byte behind it - the case the old `position < destination` test discarded
        final byte[] object = anObjectOf(CHUNK_SIZE + 2);
        final List<long[]> ranges = new ArrayList<>();

        assertArrayEquals(object, readFully(object, ranges));
        assertEquals(2, ranges.size());
        assertArrayEquals(new long[]{CHUNK_SIZE + 1, CHUNK_SIZE + 1}, ranges.get(1));
    }

    @Test
    public void testReadsSingleByteObject() throws IOException {
        assertArrayEquals(new byte[]{(byte) 0xff}, readFully(new byte[]{(byte) 0xff}, new ArrayList<>()));
    }

    @Test
    public void testReportsEndOfStreamRepeatedlyAndWithoutFurtherRequests() throws IOException {
        final byte[] object = anObjectOf(10);
        final List<long[]> ranges = new ArrayList<>();
        try (MockedStatic<S3Client> ignored = mockS3(object, ranges);
             InputStream stream = new S3ObjectChunkInputStream(URI, 0, object.length - 1)) {
            IOUtils.toByteArray(stream);
            assertEquals(-1, stream.read());
            assertEquals(-1, stream.read());
            assertEquals(-1, stream.read());
            // one range for the object, one refill that found nothing; EOF is not re-requested
            assertEquals(1, ranges.size());
        }
    }

    @Test
    public void testReadsFromAnOffset() throws IOException {
        final byte[] object = anObjectOf(CHUNK_SIZE * 2);
        final int offset = CHUNK_SIZE + 7;
        final List<long[]> ranges = new ArrayList<>();
        try (MockedStatic<S3Client> ignored = mockS3(object, ranges);
             InputStream stream = new S3ObjectChunkInputStream(URI, offset, object.length - 1)) {
            assertArrayEquals(Arrays.copyOfRange(object, offset, object.length), IOUtils.toByteArray(stream));
        }
        assertEquals(offset, ranges.get(0)[0]);
    }

    @Test
    public void testReadsBgzipObjectRegisteredFromS3() throws IOException {
        final String text = bgzipPayload();
        final byte[] object = bgzip(text);
        // a bgzip'd feature file is small enough to arrive in one chunk; what used to fail was the
        // read of the 28-byte terminator block that follows the payload
        assertTrue(object.length < CHUNK_SIZE);

        try (MockedStatic<S3Client> ignored = mockS3(object, new ArrayList<>());
             BlockCompressedInputStream stream =
                     new BlockCompressedInputStream(new S3ObjectChunkInputStream(URI, 0, object.length - 1))) {
            assertEquals(text, new String(IOUtils.toByteArray(stream), StandardCharsets.UTF_8));
        }
    }

    private byte[] readFully(final byte[] object, final List<long[]> ranges) throws IOException {
        try (MockedStatic<S3Client> ignored = mockS3(object, ranges);
             InputStream stream = new S3ObjectChunkInputStream(URI, 0, object.length - 1)) {
            return IOUtils.toByteArray(stream);
        }
    }

    /**
     * An {@link S3Client} that serves {@code object} out of memory, records every range it is asked
     * for in {@code ranges}, and reads the range end inclusively, as S3 does.
     */
    private MockedStatic<S3Client> mockS3(final byte[] object, final List<long[]> ranges) {
        final S3Client client = Mockito.mock(S3Client.class);
        Mockito.when(client.loadFromTo(Mockito.eq(URI), Mockito.anyLong(), Mockito.anyLong()))
                .thenAnswer(invocation -> {
                    final long from = invocation.getArgument(1);
                    final long to = invocation.getArgument(2);
                    ranges.add(new long[]{from, to});
                    return new ByteArrayInputStream(Arrays.copyOfRange(object, (int) from,
                            (int) Math.min(object.length, to + 1)));
                });
        final MockedStatic<S3Client> mocked = Mockito.mockStatic(S3Client.class);
        mocked.when(S3Client::getInstance).thenReturn(client);
        return mocked;
    }

    private byte[] anObjectOf(final int length) {
        final byte[] object = new byte[length];
        for (int i = 0; i < length; i++) {
            // deliberately produces 0xff bytes, which the old end-of-object sentinel was
            // indistinguishable from
            object[i] = (byte) (i * 31 + 7);
        }
        return object;
    }

    private String bgzipPayload() {
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            text.append("X\t").append(i).append("\t.\tA\tT\t.\tPASS\t.\n");
        }
        return text.toString();
    }

    private byte[] bgzip(final String text) throws IOException {
        final File file = File.createTempFile("s3-chunk-stream", ".gz");
        try {
            try (OutputStream out = new BlockCompressedOutputStream(file)) {
                out.write(text.getBytes(StandardCharsets.UTF_8));
            }
            return Files.readAllBytes(file.toPath());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}
