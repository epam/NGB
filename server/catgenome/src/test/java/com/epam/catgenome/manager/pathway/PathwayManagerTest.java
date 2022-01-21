/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
package com.epam.catgenome.manager.pathway;

import com.epam.catgenome.controller.vo.registration.PathwayRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.pathway.Pathway;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public class PathwayManagerTest extends TestCase {

    @Autowired
    private PathwayManager pathwayManager;

    @Autowired
    private ApplicationContext context;

    private String fileName;

    @Before
    public void setUp() throws IOException {
        this.fileName = context.getResource("classpath:pathway//pathway.sbml").getFile().getPath();
    }

    @Test
    public void createPathwayTest() throws IOException, ParseException {
        final Pathway pathway = registerPathway("createPathwayTest", fileName);
        assertNotNull(pathway);
        assertEquals("createPathwayTest", pathway.getName());
        assertEquals("pathway", pathway.getPrettyName());
        assertEquals(BiologicalDataItemResourceType.FILE, pathway.getType());
    }

    @Test
    public void loadPathway() throws IOException, ParseException {
        final Pathway pathway = registerPathway("loadPathway", fileName);
        final Pathway createdPathway = pathwayManager.loadPathway(pathway.getPathwayId());
        assertNotNull(createdPathway);
    }

    @Test
    public void loadPathways() throws IOException, ParseException {
        registerPathway("loadPathways", fileName);
        registerPathway("loadPathways1", fileName);
        final List<Pathway> pathways = pathwayManager.loadPathways();
        assertEquals(2, pathways.size());
    }

    @Test
    public void deletePathwayTest() throws IOException, ParseException {
        final Pathway pathway = registerPathway("deletePathwayTest", fileName);
        Pathway createdPathway = pathwayManager.loadPathway(pathway.getPathwayId());
        assertNotNull(createdPathway);
        pathwayManager.deletePathway(createdPathway.getPathwayId());
        createdPathway = pathwayManager.loadPathway(pathway.getPathwayId());
        assertNull(createdPathway);
    }

    @NotNull
    private Pathway registerPathway(final String name, final String fileName) throws IOException, ParseException {
        final PathwayRegistrationRequest request = new PathwayRegistrationRequest();
        request.setName(name);
        request.setPrettyName("pathway");
        request.setPath(fileName);
        request.setPathwayDesc("description");
        return pathwayManager.createPathway(request);
    }
}
