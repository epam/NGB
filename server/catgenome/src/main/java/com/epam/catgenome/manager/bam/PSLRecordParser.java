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

import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PSLRecordParser  {

    private static final int FIELDS_COUNT = 21;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String TABLE_START = "<TT><PRE>";
    private static final String TABLE_END = "</PRE></TT>";
    private static final int NAME_INDEX = 9;
    private static final int CHR_INDEX = 13;
    private static final int START_INDEX = 15;
    private static final int END_INDEX = 16;
    private static final int STRAND_INDEX = 8;
    private static final int MATCH_INDEX = 0;
    private static final int MISMATCH_INDEX = 1;
    private static final int REPMATCH_INDEX = 2;
    private static final int NS_INDEX = 3;
    private static final int Q_GAP_COUNT_INDEX = 4;
    private static final int Q_GAP_BASES_INDEX = 5;
    private static final int T_GAP_COUNT_INDEX = 6;
    private static final int T_GAP_BASES_INDEX = 7;
    private static final int Q_SIZE_INDEX = 10;

    private static final float SCORE_CONSTANT = 1000.0f;

    public List<PSLRecord> parse(String result) throws IOException {
        List<PSLRecord> records = new ArrayList<>();

        BufferedReader br = new BufferedReader(new StringReader(result));
        String line;
        int headerLineCount = 0;
        boolean header = false;
        boolean pslSectionFound = false;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith(TABLE_START)) {
                pslSectionFound = true;
                if (line.contains("psLayout") && line.contains("version")) {
                    header = true;
                    headerLineCount++;
                }
            } else if (line.startsWith(TABLE_END)) {
                break;
            }

            if (pslSectionFound) {
                if (header && headerLineCount < 6) {
                    headerLineCount++;
                    continue;
                }

                String[] tokens = WHITESPACE.split(line);
                if (tokens.length == FIELDS_COUNT) {
                    records.add(createRecord(tokens));
                }
            }
        }

        return records;
    }

    private PSLRecord createRecord(String[] tokens) {
        PSLRecord record = new PSLRecord();
        record.setName(tokens[NAME_INDEX]);
        record.setChr(tokens[CHR_INDEX]);
        record.setStartIndex(Integer.parseInt(tokens[START_INDEX]));
        record.setEndIndex(Integer.parseInt(tokens[END_INDEX]));
        record.setStrand(StrandSerializable.forValue(tokens[STRAND_INDEX]));
        record.setMatch(Integer.parseInt(tokens[MATCH_INDEX]));
        record.setMisMatch(Integer.parseInt(tokens[MISMATCH_INDEX]));
        record.setRepMatch(Integer.parseInt(tokens[REPMATCH_INDEX]));
        record.setNs(Integer.parseInt(tokens[NS_INDEX]));
        record.setqGapCount(Integer.parseInt(tokens[Q_GAP_COUNT_INDEX]));
        record.setqGapBases(Integer.parseInt(tokens[Q_GAP_BASES_INDEX]));
        record.settGapCount(Integer.parseInt(tokens[T_GAP_COUNT_INDEX]));
        record.setqGapBases(Integer.parseInt(tokens[T_GAP_BASES_INDEX]));
        record.setqSize(Integer.parseInt(tokens[Q_SIZE_INDEX]));

        double score = (SCORE_CONSTANT * (record.getMatch() + record.getRepMatch() - record.getMisMatch()
                - record.getqGapCount() - record.gettGapCount())) / record.getqSize();
        record.setScore(score);

        return record;
    }
}
