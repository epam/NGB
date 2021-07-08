/*
* Originated from https://github.com/samtools/htsjdk/blob/master/src/main/java/htsjdk/tribble/gff/Gff3Codec.java
* License info https://github.com/samtools/htsjdk/tree/master#licensing-information
*/
package com.epam.catgenome.manager.gene.writer;

import htsjdk.tribble.TribbleException;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Codec for parsing Gff3 files, as defined in
 * https://github.com/The-Sequence-Ontology/Specifications/blob/31f62ad469b31769b43af42e0903448db1826925/gff3.md
 * Note that while spec states that all feature types must be defined in sequence ontology, this implementation makes
 * no check on feature types, and allows any string as feature type
 *
 * Each feature line in the Gff3 file will be emitted as a separate feature.  Features linked together through the
 * "Parent" attribute will be linked through {@link Gff3Feature#getParents()}, {@link Gff3Feature#getChildren()},
 * {@link Gff3Feature#getAncestors()}, {@link Gff3Feature#getDescendents()}, amd {@link Gff3Feature#flatten()}.
 * This linking is not guaranteed to be comprehensive when the file is read for only features overlapping a particular
 * region, using a tribble index.  In this case, a particular feature will only be linked to the subgroup of features
 * it is linked to in the input file which overlap the given region.
 */

public final class Gff3Codec {

    private Gff3Codec() {
        //utility class
    }

    static String extractSingleAttribute(final List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        if (values.size() != 1) {
            throw new TribbleException("Attribute has multiple values when only one expected");
        }
        return values.get(0);
    }

    /**
     * Enum for parsing directive lines.  If information in directive line needs to be parsed beyond specifying
     * directive type, decode method should be overriden
     */
    public enum Gff3Directive {

        VERSION3_DIRECTIVE("##gff-version\\s+3(?:\\.\\d*)*$") {
            @Override
             protected Object decode(final String line) throws IOException {
                final String[] splitLine = line.split("\\s+");
                return splitLine[1];
            }

            @Override
            String encode(final Object object) {
                if (object == null) {
                    throw new TribbleException("Cannot encode null in VERSION3_DIRECTIVE");
                }
                if (!(object instanceof String)) {
                    throw new TribbleException("Cannot encode object of type " +
                            object.getClass() + " in VERSION3_DIRECTIVE");
                }

                final String versionLine = "##gff-version " + (String)object;
                if (!regexPattern.matcher(versionLine).matches()) {
                    throw new TribbleException("Version " + (String)object + " is not a valid version");
                }

                return versionLine;
            }
        };

        protected final Pattern regexPattern;

        Gff3Directive(String regex) {
            this.regexPattern = Pattern.compile(regex);
        }

        public static Gff3Directive toDirective(final String line) {
            for (final Gff3Directive directive : Gff3Directive.values()) {
                if(directive.regexPattern.matcher(line).matches()) {
                    return directive;
                }
            }
            return null;
        }

        protected Object decode(String line) throws IOException {
            return null;
        }

        abstract String encode(Object object);
    }
}
