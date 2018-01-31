package com.epam.catgenome.util.feature.reader.index;


import java.util.Map;

/**
 * Some Index implementations can be modified in memory.  Also, properties do not make sense for all index types.
 * Only the relevant index implementations implement this interface.
 */
public interface MutableIndex extends Index {
    void addProperty(String key, String value);

    void addProperties(Map<String, String> properties);
}
