package com.epam.catgenome.dao.index.field;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.util.BytesRef;

/**
 A modification of Lucene's {@link FloatPoint}
 */
public class SortedSetFloatPoint extends Field {
    private static FieldType getType(int numDims) {
        FieldType type = new FieldType();
        type.setDimensions(numDims, Float.BYTES);
        type.setDocValuesType(DocValuesType.SORTED_SET);
        type.freeze();
        return type;
    }

    @Override
    public void setFloatValue(float value) {
        setFloatValues(value);
    }

    /** Change the values of this field */
    public void setFloatValues(float... point) {
        if (type.pointDimensionCount() != point.length) {
            throw new IllegalArgumentException("this field (name=" + name + ") uses " + type.pointDimensionCount() +
                                           " dimensions; cannot change to (incoming) " + point.length + " dimensions");
        }
        fieldsData = pack(point);
    }

    @Override
    public void setBytesValue(BytesRef bytes) {
        throw new IllegalArgumentException("cannot change value type from float to BytesRef");
    }

    @Override
    public Number numericValue() {
        if (type.pointDimensionCount() != 1) {
            throw new IllegalStateException("this field (name=" + name + ") uses " + type.pointDimensionCount() +
                                            " dimensions; cannot convert to a single numeric value");
        }
        BytesRef bytes = (BytesRef) fieldsData;
        assert bytes.length == Float.BYTES;
        return FloatPoint.decodeDimension(bytes.bytes, bytes.offset);
    }

    private static BytesRef pack(float... point) {
        if (point == null) {
            throw new IllegalArgumentException("point cannot be null");
        }
        if (point.length == 0) {
            throw new IllegalArgumentException("point cannot be 0 dimensions");
        }
        byte[] packed = new byte[point.length * Float.BYTES];

        for (int dim = 0; dim < point.length; dim++) {
            FloatPoint.encodeDimension(point[dim], packed, dim * Float.BYTES);
        }

        return new BytesRef(packed);
    }

    /** Creates a new SortedFloatPoint, indexing the
     *  provided N-dimensional float point.
     *
     *  @param name field name
     *  @param point float[] value
     *  @throws IllegalArgumentException if the field name or value is null.
     */
    public SortedSetFloatPoint(String name, float... point) {
        super(name, pack(point), getType(point.length));
    }
}
