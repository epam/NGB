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
import com.epam.catgenome.entity.pathway.NGBPathway;
import com.epam.catgenome.entity.pathway.PathwayDatabaseSource;
import com.epam.catgenome.entity.pathway.PathwayQueryParams;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.PagingInfo;
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

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class PathwayManagerTest extends TestCase {

    @Autowired
    private PathwayManager pathwayManager;

    @Autowired
    private ApplicationContext context;

    private String sbgnFileName;
    private String jsonFileName;

    @Before
    public void setUp() throws IOException {
        this.sbgnFileName = context.getResource("classpath:pathway//Glycolysis.sbgn").getFile().getPath();
        this.jsonFileName = context.getResource("classpath:pathway//pwycollage.graph.json").getFile().getPath();
    }

    @Test
    public void createSBGNPathwayTest() throws IOException {
        final NGBPathway pathway = registerPathway("createSBGNPathwayTest", sbgnFileName);
        pathwayManager.deletePathway(pathway.getPathwayId());
        assertNotNull(pathway);
        assertEquals("createSBGNPathwayTest", pathway.getName());
        assertEquals("pathway", pathway.getPrettyName());
        assertEquals(BiologicalDataItemResourceType.FILE, pathway.getType());
        assertEquals(sbgnFileName, pathway.getPath());
        assertEquals(sbgnFileName, pathway.getSource());
        assertEquals(PathwayDatabaseSource.CUSTOM, pathway.getDatabaseSource());
    }

    @Test
    public void createCollagePathwayTest() throws IOException {
        final NGBPathway pathway = registerPathway("createCollagePathwayTest", jsonFileName);
        pathwayManager.deletePathway(pathway.getPathwayId());
        assertNotNull(pathway);
        assertEquals("createCollagePathwayTest", pathway.getName());
        assertEquals("pathway", pathway.getPrettyName());
        assertEquals(BiologicalDataItemResourceType.FILE, pathway.getType());
        assertEquals(jsonFileName, pathway.getPath());
        assertEquals(jsonFileName, pathway.getSource());
        assertEquals(PathwayDatabaseSource.COLLAGE, pathway.getDatabaseSource());
    }

    @Test
    public void loadPathway() throws IOException {
        final NGBPathway pathway = registerPathway("loadPathway", sbgnFileName);
        final NGBPathway createdPathway = pathwayManager.loadPathway(pathway.getPathwayId());
        pathwayManager.deletePathway(pathway.getPathwayId());
        assertNotNull(createdPathway);
    }

    @Test
    public void loadPathways() throws IOException, ParseException {
        final NGBPathway pathway = registerPathway("loadPathways", sbgnFileName);
        final NGBPathway pathway1 = registerPathway("loadPathways1", sbgnFileName);
        final NGBPathway pathway2 = registerPathway("loadPathways2", sbgnFileName);
        final PathwayQueryParams parameters = new PathwayQueryParams();
        final PagingInfo pagingInfo = new PagingInfo(2, 1);
        parameters.setPagingInfo(pagingInfo);
        final Page<NGBPathway> pathways = pathwayManager.loadPathways(parameters);
        pathwayManager.deletePathway(pathway.getPathwayId());
        pathwayManager.deletePathway(pathway1.getPathwayId());
        pathwayManager.deletePathway(pathway2.getPathwayId());
        assertEquals(2, pathways.getItems().size());
        assertEquals(3, pathways.getTotalCount());
    }

    @Test
    public void deletePathwayTest() throws IOException {
        final NGBPathway pathway = registerPathway("deletePathwayTest", sbgnFileName);
        NGBPathway createdPathway = pathwayManager.loadPathway(pathway.getPathwayId());
        assertNotNull(createdPathway);
        pathwayManager.deletePathway(createdPathway.getPathwayId());
        createdPathway = pathwayManager.loadPathway(pathway.getPathwayId());
        assertNull(createdPathway);
    }

    @NotNull
    private NGBPathway registerPathway(final String name, final String fileName) throws IOException {
        final PathwayRegistrationRequest request = PathwayRegistrationRequest.builder()
                .name(name)
                .prettyName("pathway")
                .path(fileName)
                .pathwayDesc("description")
                .build();
        return pathwayManager.registerPathway(request);
    }
}
