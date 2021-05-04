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

package com.epam.catgenome.util.azure;

import com.epam.catgenome.util.FeatureSeekableStream;
import htsjdk.samtools.util.RuntimeIOException;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
public class AzureBlobSeekableStream extends FeatureSeekableStream {

    private final AzureBlobClient client;

    public AzureBlobSeekableStream(final String azureBlobUri,
                                   final AzureBlobClient client) {
        super(azureBlobUri);
        this.client = client;
        this.contentLength = client.getFileSize(azureBlobUri);
        recreateInnerStream();
    }

    private void recreateInnerStream() {
        if (null != currentDataStream) {
            try {
                currentDataStream.close();
            } catch (IOException e) {
                throw new RuntimeIOException(e.getMessage() + "failed to close the data stream", e);
            }
        }

        this.currentDataStream = new CountingWithSkipInputStream(
                new AzureBlobInputStream(cloudUri, offset, length() - 1, client));
        log.debug("A new data stream was launched on offset = {}", offset);
    }

    /**
     * A method that jumps to a specific position in file.
     *
     * @param targetPosition target position in file.
     */
    @Override
    public void seek(long targetPosition) throws IOException {
        log.debug("Seeking from {} to {}", position(), targetPosition);
        this.offset = targetPosition;
        recreateInnerStream();
    }
}
