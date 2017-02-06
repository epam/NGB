package com.epam.catgenome.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.Assert;

/**
 * Class for extracting data from VCF INFO fields into a table-like structure.
 * It expects a template format as an input String  three symbols:
 *  * 1st = start of format specification
 *  * 2nd = delimiter between fields
 *  * 3rd = end of format specification.
 *  Example template = '|', meaning that format specification can be found between single quotes "'"
 *  and fields will be delimited by pipe "|". Presence of at least one delimiter is required to consider
 *  an INFO filed extensible.
 *  Class supports several templates.
 */
public class InfoFieldParser {
    
    private final List<InfoPattern> patterns;
    private static final String TEMPLATE_DELIMITER = ",";
    private static final List<Pair<String, String>> PARENTHESES= new ArrayList<>();

    static {
        PARENTHESES.add(Pair.of("{", "}"));
        PARENTHESES.add(Pair.of("(", ")"));
        PARENTHESES.add(Pair.of("[", "]"));
    }

    public InfoFieldParser(final String templates) {
        Assert.notNull(templates);
        patterns = Arrays.stream(templates.split(TEMPLATE_DELIMITER))
                .map(InfoPattern::new)
                .collect(Collectors.toList());
    }

    public boolean isExtendedInfoField(String line) {
        return patterns.stream().anyMatch(p -> p.isExtendedInfoField(line));
    }

    public List<String> extractHeaderFromLine(String line) {
        for (InfoPattern pattern : patterns) {
            List<String> data = pattern.extractHeaderFromLine(line);
            if (!data.isEmpty()) {
                return data;
            }
        }
        return Collections.emptyList();
    }

    public List<String> extractDataFromLine(String line) {
        for (InfoPattern pattern : patterns) {
            List<String> data = pattern.extractDataFromLine(line);
            if (!data.isEmpty()) {
                return data;
            }
        }
        return Collections.emptyList();
    }

    public static class InfoPattern {
        private final String start;
        private final String end;
        private final String delimiter;

        private static final int PATTERN_SIZE = 3;
        private static final int START_OFFSET = 0;
        private static final int END_OFFSET = 2;
        private static final int DELIMITER_OFFSET = 1;

        private static final int INDEX_NOT_FOUND = -1;

        public InfoPattern(String pattern) {
            Assert.isTrue(pattern.length() == PATTERN_SIZE,
                    MessageHelper.getMessage(MessagesConstants.ERROR_ILLEGAL_TEMPLATE_FORMAT, PATTERN_SIZE));
            start = String.valueOf(pattern.charAt(START_OFFSET));
            end = String.valueOf(pattern.charAt(END_OFFSET));
            delimiter = String.valueOf(pattern.charAt(DELIMITER_OFFSET));
        }

        /**
         * @param line description of the INFO field
         * @return true, if line matches at least one of supported patterns
         */
        public boolean isExtendedInfoField(String line) {
            int indexStart = line.indexOf(start);
            if (indexStart == INDEX_NOT_FOUND) {
                return false;
            }
            //we require at least one delimiter rto be present
            int indexDelimiter = line.indexOf(delimiter, indexStart + 1);
            if (indexDelimiter == INDEX_NOT_FOUND) {
                return false;
            }
            int indexEnd = line.indexOf(end, indexDelimiter + 1);
            if (indexEnd == INDEX_NOT_FOUND) {
                return false;
            }
            return true;
        }

        /**
         *
         * @param line description of the INFO field
         * @return {@code List} with names of fields described in the INFO line
         */
        public List<String> extractHeaderFromLine(String line) {
            if (!isExtendedInfoField(line)) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<>();
            int indexStart = line.indexOf(start);
            int indexEnd = line.indexOf(end, indexStart + 1);
            if (indexStart != INDEX_NOT_FOUND && indexEnd != INDEX_NOT_FOUND) {
                String pattern = line.substring(indexStart + 1, indexEnd);
                String[] split = pattern.split(Pattern.quote(delimiter));
                result.addAll(Arrays.stream(split).map(String::trim).collect(Collectors.toList()));
            }
            return result;
        }

        /**
         * @param line value of the INFO field
         * @return {@code List} of field's values
         */
        public List<String> extractDataFromLine(String line) {
            //try to remove parentheses
            String lineWithoutParentheses = removeParentheses(line);
            //in case line ends with a delimiter
            String pattern = lineWithoutParentheses + " ";

            String[] split = pattern.split(Pattern.quote(delimiter));
            if (split.length == 1) {
                return Collections.emptyList();
            }
            return Arrays.stream(split).map(String::trim).collect(Collectors.toList());
        }

        private String removeParentheses(String line) {
            for (Pair<String, String> parentheses : PARENTHESES) {
                if (line.startsWith(parentheses.getLeft()) && line.endsWith(parentheses.getRight())) {
                    return line.substring(1, line.length() - 1);
                }
            }
            return line;
        }
    }
}
