/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.ngb.cli.entity;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Mariia_Zueva on 11/25/2016.
 */
public class BiologicalDataItemTest {

    private static final long ITEM_ID_1 = 100L;
    private static final long ITEM_ID_2 = 1000L;

    @Test
    public void testFormatting() {
        BiologicalDataItem item1 = new BiologicalDataItem();
        item1.setId(ITEM_ID_1);
        item1.setName("BiologicalDataItem1");
        item1.setType("FILE");
        item1.setPath("/path/to/first/item");
        item1.setFormat(BiologicalDataItemFormat.BAM_INDEX);
        item1.setCreatedDate(new Date());

        BiologicalDataItem item2 = new BiologicalDataItem();
        item2.setId(ITEM_ID_2);
        item2.setName("BiologicalDataItem2WithLongName");
        item2.setType("FILE");
        item2.setPath("/path/file");
        item2.setFormat(BiologicalDataItemFormat.BAM);
        item2.setCreatedDate(new Date());

        List<BiologicalDataItem> items = Arrays.asList(item1, item2);
        String format = item1.getFormatString(items);
        Assert.assertEquals(item1.formatHeader(format).length(), item1.formatItem(format).length());
        Assert.assertEquals(item1.formatItem(format).length(), item2.formatItem(format).length());

    }
}
