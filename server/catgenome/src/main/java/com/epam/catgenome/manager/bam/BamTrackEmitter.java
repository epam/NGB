/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.manager.bam;

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.entity.bam.BamTrack;
import com.epam.catgenome.entity.bam.Read;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;

/**
 * Class wrapper over {@link ResponseBodyEmitter}. Common usage is continuously writing {@link Read}s by calling
 * {@link BamTrackEmitter#writeRecord} method and finishing by calling
 * {@link BamTrackEmitter#writeTrackAndFinish(BamTrack)} passing result {@link BamTrack} ({@link BamTrack#blocks} field
 * will be ignored.
 * In case of any exception during writing to {@link BamTrackEmitter} {@link BamTrackEmitter#finishWithException} should
 * be called.
 * BamTrackEmitter produces buffering.
 */
public class BamTrackEmitter {

    private static final int BUFFER_SIZE = 512 * 1024;

    private JsonMapper jsonMapper;

    private final ResponseBodyEmitter emitter;

    private boolean firstRecordWasWritten = false;
    private boolean finished = false;

    @SuppressWarnings("PMD.AvoidStringBufferField")
    private StringBuilder stringBuffer;

    public BamTrackEmitter(ResponseBodyEmitter emitter) throws IOException {
        this.emitter = emitter;
        this.stringBuffer = new StringBuilder(BUFFER_SIZE);
        this.jsonMapper = new JsonMapper();

        writeHeader();
    }

    /**
     * Write a read to the emitter. Real data transferring could not be happened due to buffering
     * @param read to be written
     * @throws IOException in case of connections troubles
     */
    public void writeRecord(Read read) throws IOException {
        checkFinished();

        if (firstRecordWasWritten) {
            write(",");
        }

        write(jsonMapper.writeValueAsString(read));

        if (!firstRecordWasWritten) {
            firstRecordWasWritten = true;
        }
    }

    /**
     * Must be called after all reads are written by {@link #writeRecord} method. Will write BamTrack to the emitter
     * ignoring {@link BamTrack#blocks} field. This method will complete wrapped emitter.
     * @param bamTrack to be written
     * @throws IOException in case of connections troubles
     */
    public void writeTrackAndFinish(BamTrack<Read> bamTrack) throws IOException {
        checkFinished();
        write("],");

        JsonNode metadata = jsonMapper.convertValue(bamTrack, JsonNode.class);
        ((ObjectNode) metadata).remove("blocks");
        write(jsonMapper.writeValueAsString(metadata).substring(1));
        write(",\"status\":\"OK\"}");

        sendBuffer();
        emitter.complete();
        finished = true;
    }

    /**
     * Should be called in case of any exception during writing to {@link BamTrackEmitter}. Will try to complete
     * wrapped emitter with exception message and ERROR status or at least call
     * {@link ResponseBodyEmitter#completeWithError(Throwable)}
     * @param throwable - exception during data transferring
     */
    public void finishWithException(Throwable throwable) {
        checkFinished();
        try {
            write("]},\"status\":\"ERROR\",\"message\":\"" + throwable.getLocalizedMessage() + "\"}");
            sendBuffer();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        emitter.complete();
    }

    private void writeHeader() throws IOException {
        write("{\"payload\":{\"blocks\":[");
    }

    private void write(String stringData) throws IOException {
        stringBuffer.append(stringData);

        if (stringBuffer.length() > BUFFER_SIZE) {
            sendBuffer();
        }
    }

    private void sendBuffer() throws IOException {
        emitter.send(stringBuffer.toString(), MediaType.TEXT_PLAIN);
        stringBuffer.setLength(0);
    }

    private void checkFinished() {
        if (finished) {
            throw new IllegalStateException("Already finished");
        }
    }

}
