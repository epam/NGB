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

package com.epam.catgenome;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.epam.catgenome.controller.ReferenceControllerTest;
import com.epam.catgenome.dao.ReferenceGenomeDaoTest;

/**
 * Source:      GenomeBrowserTestSuite.java
 * Created:     10/21/15, 3:12 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code GenomeBrowserTestSuite} defines test suite for CATGenome Browser application.
 * <p>
 * Here you should define all tests that makes sense for application.
 *
 * @author Denis Medvedev
 */
@RunWith(Suite.class)
@Suite.SuiteClasses
    (
        {
            /** DAOs */
            ReferenceGenomeDaoTest.class,

            /** Managers */

            /** Controllers */
            ReferenceControllerTest.class
        }
    )
public class GenomeBrowserTestSuite {

}
