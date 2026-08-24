/*
 * MIT License
 *
 * Copyright (c) 2026 EPAM Systems
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

package com.epam.catgenome.entity;

import org.junit.Assert;
import org.junit.Test;

/**
 * Pins the treatment of the format ids MAF used to be persisted under. {@code BIO_DATA_ITEM.FORMAT}
 * holds the numeric id, so 13 and 14 can be present in any database written before MAF was removed.
 */
public class BiologicalDataItemFormatTest {

    @Test
    public void getByIdRejectsTheRemovedMafFormat() {
        try {
            BiologicalDataItemFormat.getById(13L);
            Assert.fail("Format id 13 (MAF) is no longer supported and must be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("The message has to name the format the id belonged to: " + e.getMessage(),
                              e.getMessage().contains("MAF"));
        }
    }

    @Test
    public void getByIdRejectsTheRemovedMafIndexFormat() {
        try {
            BiologicalDataItemFormat.getById(14L);
            Assert.fail("Format id 14 (MAF_INDEX) is no longer supported and must be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("The message has to name the format the id belonged to: " + e.getMessage(),
                              e.getMessage().contains("MAF_INDEX"));
        }
    }

    /**
     * Deliberately not a blanket throw: {@code getById} is also called for the index format of items
     * that have no index, which passes {@code null}, and an unknown id must stay {@code null} too.
     */
    @Test
    public void getByIdReturnsNullForAnUnknownOrAbsentId() {
        Assert.assertNull(BiologicalDataItemFormat.getById(999L));
        Assert.assertNull(BiologicalDataItemFormat.getById(null));
    }

    @Test
    public void supportedFormatsStillResolveByTheirPersistedId() {
        Assert.assertEquals(BiologicalDataItemFormat.SEG_INDEX, BiologicalDataItemFormat.getById(12L));
        Assert.assertEquals(BiologicalDataItemFormat.VG, BiologicalDataItemFormat.getById(15L));
    }
}
