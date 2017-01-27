package com.epam.catgenome.entity.bam;

import java.util.List;
import java.util.stream.Collectors;

import htsjdk.samtools.SAMRecord;

/**
 * An implementation of HTSJDK SAMTagAndValue that is a POJO
 */
public class NgbSamTagAndValue {
    private String tag;
    private Object value;

    public NgbSamTagAndValue() {
        // no-op
    }

    public NgbSamTagAndValue(SAMRecord.SAMTagAndValue tagAndValue) {
        this.tag = tagAndValue.tag;
        this.value = tagAndValue.value;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public static List<NgbSamTagAndValue> fromSAMTags(List<SAMRecord.SAMTagAndValue> tags) {
        return tags.stream().map(NgbSamTagAndValue::new).collect(Collectors.toList());
    }
}
