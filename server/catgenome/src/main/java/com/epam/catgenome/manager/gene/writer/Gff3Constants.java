/*
 * Originated from https://github.com/samtools/htsjdk/blob/master/src/main/java/htsjdk/tribble/gff/Gff3Constants.java
 */
package com.epam.catgenome.manager.gene.writer;

public final class Gff3Constants {
     public static final char FIELD_DELIMITER = '\t';
     public static final char ATTRIBUTE_DELIMITER = ';';
     public static final char KEY_VALUE_SEPARATOR = '=';
     public static final char VALUE_DELIMITER = ',';
     public static final String COMMENT_START = "#";
     public static final String UNDEFINED_FIELD_VALUE = ".";
     public static final char END_OF_LINE_CHARACTER = '\n';
}
