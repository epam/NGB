package com.epam.catgenome.manager.wig.reader;

import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.readers.LineIterator;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BedGraphCodec extends AsciiFeatureCodec<BedGraphFeature> {

    private static final List<String> BED_GRAPH_EXTENSIONS = new ArrayList<>();
    private static final String COMMENT_LINE = "#";
    private static final String TRACK_LINE = "track";
    private static final String BROWSER_LINE = "browser";
    public static final String TYPE_LINE = "type";

    static {
        BED_GRAPH_EXTENSIONS.add(".bg");
        BED_GRAPH_EXTENSIONS.add(".bg.gz");
        BED_GRAPH_EXTENSIONS.add(".bdg");
        BED_GRAPH_EXTENSIONS.add(".bdg.gz");
        BED_GRAPH_EXTENSIONS.add(".bedGraph");
        BED_GRAPH_EXTENSIONS.add(".bedGraph.gz");
    }

    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\t|( +)");

    public BedGraphCodec() {
        super(BedGraphFeature.class);
    }

    @Override
    public BedGraphFeature decode(String line) {

        if (line.trim().isEmpty()) {
            return null;
        }

        if (line.startsWith(COMMENT_LINE) || line.startsWith(TRACK_LINE) || line.startsWith(BROWSER_LINE)) {
            return null;
        }

        String[] tokens = SPLIT_PATTERN.split(line, -1);
        Assert.isTrue(tokens.length == 4);
        return new BedGraphFeature(
                tokens[0], Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Float.parseFloat(tokens[3])
        );
    }

    @Override
    public Object readActualHeader(LineIterator reader) {
        while (reader.hasNext() &&  reader.peek().isEmpty() || reader.peek().startsWith(COMMENT_LINE)
                || reader.peek().startsWith(TYPE_LINE)|| reader.peek().startsWith(TRACK_LINE) ||
                reader.peek().startsWith(BROWSER_LINE)) {
            reader.next();
        }
        return null;
    }

    @Override
    public boolean canDecode(String path) {
        return BED_GRAPH_EXTENSIONS.stream().anyMatch(path::endsWith);
    }
}
