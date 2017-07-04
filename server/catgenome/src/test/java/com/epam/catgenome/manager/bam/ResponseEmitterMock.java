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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;

class ResponseEmitterMock extends ResponseBodyEmitter {

    private static final ObjectMapper MAPPER = new JsonMapper();

    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder buffer = new StringBuilder();

    @Override
    public synchronized void send(Object object, MediaType mediaType) throws IOException {
        if (mediaType != MediaType.TEXT_PLAIN || !(object instanceof String)) {
            throw new IllegalArgumentException("Only text values are available for testing");
        }

        String data = (String) object;
        buffer.append(data);
    }

    public BamTrack<Read> getBamTrack() throws IOException {
        JsonNode resultJson = MAPPER.readTree(buffer.toString());
        JsonNode trackJson = resultJson.findValue("payload");
        return MAPPER.readValue(trackJson.toString(), new TypeReference<BamTrack<Read>>() {});
    }

    public String getResultStatus() throws IOException {
        JsonNode resultJson = MAPPER.readTree(buffer.toString());
        return resultJson.findValue("status").asText();
    }

    public String getMessage() throws IOException {
        JsonNode resultJson = MAPPER.readTree(buffer.toString());
        return resultJson.findValue("message").asText();
    }
}
