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

package com.epam.catgenome.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Source:      DaoHelperTest.java
 * Created:     10/29/15, 8:15 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code DaoHelper} is designed to test miscellaneous DAO utilities and calls
 * shared between different DAOs oriented to deal with certain business entity.
 *
 * @author Denis Medvedev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class DaoHelperTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final Long TEMP_LIST_CAPACITY = 3L;

    private static final String JUNIT_SEQUENCE_NAME = "catgenome.s_junit";

    @Autowired
    private DaoHelper daoHelper;

    @Before
    public void setup() throws Exception {
        assertNotNull("DaoHelper isn't provided.", daoHelper);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testCreateId() {
        final Long createdId = daoHelper.createId(JUNIT_SEQUENCE_NAME);
        assertNotNull("The next sequence value isn't generated.", createdId);
        assertTrue("Unexpected next sequence value.", createdId >= 1);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testCreateIds() {
        final List<Long> createdIds = daoHelper.createIds(JUNIT_SEQUENCE_NAME, TEMP_LIST_CAPACITY.intValue());
        assertEquals("Unexpected number of retrieved IDs.", TEMP_LIST_CAPACITY.intValue(), createdIds.size());
    }

}
