package com.epam.catgenome.manager.blast;

import com.epam.catgenome.manager.blast.dto.BlastTaxonomy;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BlastTaxonomyManagerTest extends TestCase {

    public static final int ORGANISMS_COUNT = 12;
    @Autowired
    private BlastTaxonomyManager blastTaxonomyManager;

    @Autowired
    private ApplicationContext context;

    private String indexDirectory;
    private String fileName;

    @Before
    public void setUp() throws IOException, ParseException {
        Resource resource = context.getResource("classpath:taxonomy");
        this.indexDirectory = resource.getFile().getPath();
        resource = context.getResource("classpath:taxonomy//names.dmp");
        this.fileName = resource.getFile().getPath();
        blastTaxonomyManager.writeLuceneTaxonomyIndex(fileName, indexDirectory);
    }

    @Test
    public void searchOrganismsTest() throws IOException, ParseException {
        List<BlastTaxonomy> organisms =  blastTaxonomyManager.searchOrganisms("Azorhizobium", indexDirectory);
        assertNotNull(organisms);
        assertEquals(2, organisms.size());
    }

    @Test
    public void searchOrganismsAfterReIndexingTest() throws IOException, ParseException {
        blastTaxonomyManager.writeLuceneTaxonomyIndex(fileName, indexDirectory);
        List<BlastTaxonomy> organisms =  blastTaxonomyManager.searchOrganisms("Azorhizobium", indexDirectory);
        assertNotNull(organisms);
        assertEquals(2, organisms.size());
    }

    @Test
    public void searchOrganismsByIdTest() throws IOException, ParseException {
        BlastTaxonomy organism = blastTaxonomyManager.searchOrganismById(6L, indexDirectory);
        assertNotNull(organism);
    }

    @Test
    public void readTaxonomyTest() {
        List<BlastTaxonomy> organisms =  blastTaxonomyManager.readTaxonomy(fileName);
        assertNotNull(organisms);
        assertEquals(ORGANISMS_COUNT, organisms.size());
    }
}