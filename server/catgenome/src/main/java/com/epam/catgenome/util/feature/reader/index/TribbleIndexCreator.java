package com.epam.catgenome.util.feature.reader.index;

import java.util.LinkedHashMap;

/**
 * Base class for Tribble-specific index creators.
 */
public abstract class TribbleIndexCreator implements IndexCreator {
    protected LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();

    public void addProperty(final String key, final String value) {
        properties.put(key, value);
    }
}
