/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.dao.index.field;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

/**
 A modification of Lucene's {@link org.apache.lucene.document.IntPoint} that allows
 */
public class SortedIntPoint extends Field {
    /** Creates a new IntPoint, indexing the
     *  provided N-dimensional int point.
     *
     *  @param name field name
     *  @param point int[] value
     *  @throws IllegalArgumentException if the field name or value is null.
     */
    public SortedIntPoint(String name, int... point) {
        super(name, pack(point), getType(point.length));
    }

    private static FieldType getType(int numDims) {
        FieldType type = new FieldType();
        type.setDocValuesType(DocValuesType.NUMERIC);
        type.setDimensions(numDims, Integer.BYTES);
        type.freeze();
        return type;
    }

    @Override
    public void setIntValue(int value) {
        setIntValues(value);
    }

    /** Change the values of this field */
    public void setIntValues(int... point) {
        if (type.pointDimensionCount() != point.length) {
            throw new IllegalArgumentException("this field (name=" + name + ") uses " + type.pointDimensionCount() +
                                           " dimensions; cannot change to (incoming) " + point.length + " dimensions");
        }
        fieldsData = pack(point);
    }

    @Override
    public void setBytesValue(BytesRef bytes) {
        throw new IllegalArgumentException("cannot change value type from int to BytesRef");
    }

    @Override
    public Number numericValue() {
        if (type.pointDimensionCount() != 1) {
            throw new IllegalStateException("this field (name=" + name + ") uses " + type.pointDimensionCount() +
                                            " dimensions; cannot convert to a single numeric value");
        }
        BytesRef bytes = (BytesRef) fieldsData;
        assert bytes.length == Integer.BYTES;
        return decodeDimension(bytes.bytes, bytes.offset);
    }

    private static BytesRef pack(int... point) {
        if (point == null) {
            throw new IllegalArgumentException("point cannot be null");
        }
        if (point.length == 0) {
            throw new IllegalArgumentException("point cannot be 0 dimensions");
        }
        byte[] packed = new byte[point.length * Integer.BYTES];

        for (int dim = 0; dim < point.length; dim++) {
            encodeDimension(point[dim], packed, dim * Integer.BYTES);
        }

        return new BytesRef(packed);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder()
            .append(getClass().getSimpleName())
            .append(" <")
            .append(name)
            .append(':');

        BytesRef bytes = (BytesRef) fieldsData;
        for (int dim = 0; dim < type.pointDimensionCount(); dim++) {
            if (dim > 0) {
                result.append(',');
            }
            result.append(decodeDimension(bytes.bytes, bytes.offset + dim * Integer.BYTES));
        }

        result.append('>');
        return result.toString();
    }

    // public helper methods (e.g. for queries)

    /** Encode single integer dimension */
    public static void encodeDimension(int value, byte[] dest, int offset) {
        NumericUtils.intToSortableBytes(value, dest, offset);
    }

    /** Decode single integer dimension */
    public static int decodeDimension(byte[] value, int offset) {
        return NumericUtils.sortableBytesToInt(value, offset);
    }
}
