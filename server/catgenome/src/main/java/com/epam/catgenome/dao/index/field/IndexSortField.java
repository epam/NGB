package com.epam.catgenome.dao.index.field;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.SortField;

import com.epam.catgenome.dao.index.FeatureIndexDao.FeatureIndexFields;

/**
 * Source:      SortFields
 * Created:     17.01.17, 14:05
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public enum IndexSortField {
    CHROMOSOME_NAME(FeatureIndexFields.CHROMOSOME_NAME, SortField.Type.STRING),
    START_INDEX(FeatureIndexFields.START_INDEX, SortField.Type.INT),
    END_INDEX(FeatureIndexFields.END_INDEX, SortField.Type.INT),
    FILTER(FeatureIndexFields.FAILED_FILTER, SortField.Type.STRING),
    VARIATION_TYPE(FeatureIndexFields.VARIATION_TYPE, SortField.Type.STRING),
    QUALITY(FeatureIndexFields.QUALITY, SortField.Type.FLOAT),
    GENE_ID(FeatureIndexFields.GENE_IDS, SortField.Type.STRING),
    GENE_NAME(FeatureIndexFields.GENE_NAMES, SortField.Type.STRING);

    private static Map<String, IndexSortField> fieldMap = new HashMap<>();
    static {
        fieldMap.put(CHROMOSOME_NAME.name(), CHROMOSOME_NAME);
        fieldMap.put(START_INDEX.name(), START_INDEX);
        fieldMap.put(END_INDEX.name(), END_INDEX);
        fieldMap.put(FILTER.name(), FILTER);
        fieldMap.put(QUALITY.name(), QUALITY);
        fieldMap.put(GENE_ID.name(), GENE_ID);
        fieldMap.put(GENE_NAME.name(), GENE_NAME);
        fieldMap.put(VARIATION_TYPE.name(), VARIATION_TYPE);
    }

    private SortField.Type type;
    private FeatureIndexFields field;

    IndexSortField(FeatureIndexFields field, SortField.Type type) {
        this.type = type;
        this.field = field;
    }

    public SortField.Type getType() {
        return type;
    }

    public FeatureIndexFields getField() {
        return field;
    }

    public static IndexSortField getByName(String name) {
        return fieldMap.get(name);
    }
}
