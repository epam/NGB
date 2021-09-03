/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2021 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.manager.session;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.session.NGBSession;
import com.epam.catgenome.entity.session.NGBSessionFilter;
import com.epam.catgenome.util.NGBRegistrationUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class NGBSessionManagerTest extends AbstractManagerTest {

    private static final String TEST_USER = "TEST_ADMIN";
    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private static final long START = 0L;
    private static final long END = 10000L;
    private static final String CHROMOSOME = "X";
    private static final String TEST_DESC = "test desc";
    private static final String TEST_SESSION = "testSession";
    public static final String SESSION_NAME = "Session";
    public static final String DESC_FILTER = "desc";

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private NGBSessionManager sessionManager;

    @Autowired
    private NGBRegistrationUtils registrationUtils;

    private Reference reference;

    @Before
    public void setup() throws IOException {
        reference = registrationUtils.registerReference(TEST_REF_NAME,
                TEST_REF_NAME + biologicalDataItemDao.createBioItemId(), TEST_USER);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAndLoadSessionTest() {
        NGBSession ngbSession = sessionManager.create(getNgbSession());
        Assert.assertNotNull(sessionManager.load(ngbSession.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createWithOnlyRequiredParamsSessionTest() {
        NGBSession ngbSession = new NGBSession();
        ngbSession.setName(TEST_SESSION);
        ngbSession.setReferenceId(reference.getId());
        ngbSession.setSessionValue("{" +
                "\"name\":\"testSession\"," +
                "\"tracks\":[" +
                "   {\"bioDataItemId\":\"\",\"height\":100,\"projectId\":\"\",\"format\":\"REFERENCE\"} " +
                "]}"
        );
        ngbSession = sessionManager.create(ngbSession);
        Assert.assertNotNull(sessionManager.load(ngbSession.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createWithOutRequiredParamsSessionTest() {
        NGBSession ngbSession = new NGBSession();
        ngbSession.setReferenceId(reference.getId());
        ngbSession.setSessionValue("{" +
                "\"name\":\"testSession\"," +
                "\"tracks\":[" +
                "   {\"bioDataItemId\":\"\",\"height\":100,\"projectId\":\"\",\"format\":\"REFERENCE\"} " +
                "]}"
        );
        ngbSession = sessionManager.create(ngbSession);
        Assert.assertNotNull(sessionManager.load(ngbSession.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteSessionTest() {
        NGBSession ngbSession = sessionManager.create(getNgbSession());
        Assert.assertNotNull(sessionManager.load(ngbSession.getId()));
        sessionManager.delete(ngbSession.getId());
        Assert.assertNull(sessionManager.load(ngbSession.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void filterSessionTest() {
        sessionManager.create(getNgbSession());
        Assert.assertFalse(sessionManager.filter(NGBSessionFilter.builder().name(SESSION_NAME).build()).isEmpty());
        Assert.assertFalse(sessionManager.filter(
                NGBSessionFilter.builder().description(DESC_FILTER).name(SESSION_NAME).build()).isEmpty()
        );
        Assert.assertFalse(sessionManager.filter(NGBSessionFilter.builder()
                .chromosome(CHROMOSOME)
                .description(DESC_FILTER)
                .name(SESSION_NAME).build()).isEmpty());

        Assert.assertFalse(sessionManager.filter(NGBSessionFilter.builder()
                .chromosome(CHROMOSOME)
                .description(DESC_FILTER)
                .start(START)
                .end(END + 1)
                .name(SESSION_NAME).build()).isEmpty());


        Assert.assertTrue(sessionManager.filter(NGBSessionFilter.builder().chromosome("wrong").build()).isEmpty());
        Assert.assertTrue(sessionManager.filter(NGBSessionFilter.builder().name("wrong").build()).isEmpty());

    }

    private NGBSession getNgbSession() {
        NGBSession ngbSession = new NGBSession();
        ngbSession.setName(TEST_SESSION);
        ngbSession.setDescription(TEST_DESC);
        ngbSession.setChromosome(CHROMOSOME);
        ngbSession.setStart(START);
        ngbSession.setEnd(END);
        ngbSession.setReferenceId(reference.getId());
        ngbSession.setSessionValue("{" +
                "\"name\":\"testSession\"," +
                "\"tracks\":[" +
                "   {\"bioDataItemId\":\"\",\"height\":100,\"projectId\":\"\",\"format\":\"REFERENCE\"} " +
                "]}"
        );
        return ngbSession;
    }

}