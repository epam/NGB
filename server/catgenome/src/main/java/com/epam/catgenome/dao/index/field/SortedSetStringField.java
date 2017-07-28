package com.epam.catgenome.dao.index.field;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.util.BytesRef;

/**
 * Source:      SortefStringField
 * Created:     12.01.17, 18:06
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public class SortedSetStringField extends Field {

    public static final FieldType STRING_FIELD_STORED_SORTED = new FieldType(StringField.TYPE_STORED);
    static {
        STRING_FIELD_STORED_SORTED.setDocValuesType(DocValuesType.SORTED_SET);
        STRING_FIELD_STORED_SORTED.freeze();
    }

    public SortedSetStringField(String key, String value) {
        super(key, value != null ? new BytesRef(value.toLowerCase()) : new BytesRef(BytesRef.EMPTY_BYTES),
              STRING_FIELD_STORED_SORTED);
    }

    public SortedSetStringField(String key, String value, boolean matchCase) {
        super(key, value != null ? new BytesRef(matchCase ? value : value.toLowerCase()) :
                   new BytesRef(BytesRef.EMPTY_BYTES), STRING_FIELD_STORED_SORTED);
    }
}
