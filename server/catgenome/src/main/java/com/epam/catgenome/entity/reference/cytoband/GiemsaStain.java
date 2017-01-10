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

package com.epam.catgenome.entity.reference.cytoband;

/**
 * <p>
 * {@code GiemsaStain} represents enumeration type that describes Giemsa stain results.
 * <p>
 * Giemsa chromosome banding stain is an unique chromosome staining technique used in
 * cytogenetics to identify individual chromosome (G-banding stain), which produces
 * characteristics bands.
 */
public enum GiemsaStain {

    NA("n/a"),
    GNEG("gneg"),
    ACEN("acen"),
    GVAR("gvar"),
    STALK("stalk"),
    GPOS25("gpos25"),
    GPOS50("gpos50"),
    GPOS75("gpos75"),
    GPOS100("gpos100");

    private final String value;

    GiemsaStain(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
