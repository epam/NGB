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

    private final static int FIELDS_COUNT = 21;
    private final static Pattern WHITESPACE = Pattern.compile("\\s+");
    private final static String TABLE_START = "<TT><PRE>";
    private final static String TABLE_END = "</PRE></TT>";
    private final static int NAME_INDEX = 9;
    private final static int CHR_INDEX = 13;
    private final static int START_INDEX = 15;
    private final static int END_INDEX = 16;
    private final static int STRAND_INDEX = 8;
    private final static int MATCH_INDEX = 0;
    private final static int MISMATCH_INDEX = 1;
    private final static int REPMATCH_INDEX = 2;
    private final static int NS_INDEX = 3;
    private final static int Q_GAP_COUNT_INDEX = 4;
    private final static int Q_GAP_BASES_INDEX = 5;
    private final static int T_GAP_COUNT_INDEX = 6;
    private final static int T_GAP_BASES_INDEX = 7;
    private final static int Q_SIZE_INDEX = 10;

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

        double score = (1000.0f * (record.getMatch() + record.getRepMatch() - record.getMisMatch()
                - record.getqGapCount() - record.gettGapCount())) / record.getqSize();
        record.setScore(score);

        return record;
    }
}
