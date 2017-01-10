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

package com.epam.catgenome.entity.protein;

import java.util.List;

import com.epam.catgenome.entity.track.Block;


/**
 * Created: 3/15/2016
 * Project: CATGenome Browser
 *
 * <p>
 * A value object, representing protein sequence and transcript ID
 * </p>
 */
public class ProteinSequenceInfo extends Block {

    private String transcriptId;
    private List<ProteinSequence> proteinSequences;

    public ProteinSequenceInfo(final Integer startIndex, final Integer endIndex, final String transcriptId,
            final List<ProteinSequence> proteinSequences) {
        super(startIndex, endIndex);
        this.transcriptId = transcriptId;
        this.proteinSequences = proteinSequences;
    }

    public String getTranscriptId() {
        return transcriptId;
    }

    public void setTranscriptId(final String transcriptId) {
        this.transcriptId = transcriptId;
    }

    public List<ProteinSequence> getProteinSequences() {
        return proteinSequences;
    }

    public void setProteinSequences(final List<ProteinSequence> proteinSequences) {
        this.proteinSequences = proteinSequences;
    }
}
