/*
* Originated from https://github.com/samtools/htsjdk/blob/master/src/main/java/htsjdk/tribble/gff/Gff3BaseData.java
* License info https://github.com/samtools/htsjdk/tree/master#licensing-information
*/
package com.epam.catgenome.manager.gene.writer;

import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Gff3BaseData {
    private static final String ID_ATTRIBUTE_KEY = "ID";
    private static final String NAME_ATTRIBUTE_KEY = "Name";
    private static final String ALIAS_ATTRIBUTE_KEY = "Alias";
    private static final int INT_31 = 31;
    private final String contig;
    private final String source;
    private final String type;
    private final int start;
    private final int end;
    private final String score;
    private final StrandSerializable strand;
    private final String phase;
    private final Map<String, List<String>> attributes;
    private final String id;
    private final String name;
    private final List<String> aliases;

    public Gff3BaseData(final String contig, final String source, final String type,
                        final int start, final int end, final String score,
                        final StrandSerializable strand, final String phase,
                        final Map<String, List<String>> attributes) {
        this.contig = contig;
        this.source = source;
        this.type = type;
        this.start = start;
        this.end = end;
        this.score = score;
        this.phase = phase;
        this.strand = strand;
        this.attributes = copyAttributesSafely(attributes);
        this.id = Gff3Codec.extractSingleAttribute(attributes.get(ID_ATTRIBUTE_KEY));
        this.name = Gff3Codec.extractSingleAttribute(attributes.get(NAME_ATTRIBUTE_KEY));
        this.aliases = attributes.getOrDefault(ALIAS_ATTRIBUTE_KEY, Collections.emptyList());
    }

    private static Map<String, List<String>> copyAttributesSafely(final Map<String, List<String>> attributes) {
        final Map<String, List<String>> modifiableDeepMap = new LinkedHashMap<>();

        for (final Map.Entry<String, List<String>> entry : attributes.entrySet()) {
            final List<String> unmodifiableDeepList = Collections.unmodifiableList(new ArrayList<>(entry.getValue()));
            modifiableDeepMap.put(entry.getKey(), unmodifiableDeepList);
        }

        return Collections.unmodifiableMap(modifiableDeepMap);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if(!other.getClass().equals(Gff3BaseData.class)) {
            return false;
        }

        final Gff3BaseData otherBaseData = (Gff3BaseData) other;
        boolean ret = otherBaseData.getContig().equals(getContig()) &&
                otherBaseData.getSource().equals(getSource()) &&
                otherBaseData.getType().equals(getType()) &&
                otherBaseData.getStart() == getStart() &&
                otherBaseData.getEnd() == getEnd() &&
                otherBaseData.getScore().equals(score) &&
                otherBaseData.getPhase().equals(getPhase()) &&
                otherBaseData.getStrand().equals(getStrand()) &&
                otherBaseData.getAttributes().equals(getAttributes());
        if (getId() == null) {
            ret = ret && otherBaseData.getId() == null;
        } else {
            ret = ret && otherBaseData.getId() != null && otherBaseData.getId().equals(getId());
        }

        if (getName() == null) {
            ret = ret && otherBaseData.getName() == null;
        } else {
            ret = ret && otherBaseData.getName() != null && otherBaseData.getName().equals(getName());
        }

        ret = ret && otherBaseData.getAliases().equals(getAliases());

        return ret;
    }

    @Override
    public int hashCode() {
        int hash = getContig().hashCode();
        hash = INT_31 * hash + getSource().hashCode();
        hash = INT_31 * hash + getType().hashCode();
        hash = INT_31 * hash + getStart();
        hash = INT_31 * hash + getEnd();
        hash = INT_31 * hash + getScore().hashCode();
        hash = INT_31 * hash + getPhase().hashCode();
        hash = INT_31 * hash + getStrand().hashCode();
        hash = INT_31 * hash + getAttributes().hashCode();
        if (getId() != null) {
            hash = INT_31 * hash + getId().hashCode();
        }

        if (getName() != null) {
            hash = INT_31 * hash + getName().hashCode();
        }

        hash = INT_31 * hash + aliases.hashCode();

        return hash;
    }

    public List<String> getAttribute(final String key) {
        return attributes.getOrDefault(key, Collections.emptyList());
    }
}
