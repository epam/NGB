package com.epam.catgenome.manager.blast;

import com.epam.catgenome.manager.blast.dto.BlastTaxonomy;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BlastTaxonomyManagerTest extends TestCase {

    @Autowired
    private BlastTaxonomyManager blastTaxonomyManager;

    @Test
    @Ignore
    public void writeLuceneIndexForOrganismsTest() throws IOException, ParseException {
        blastTaxonomyManager.writeLuceneTaxonomyIndex();
    }

    @Test
    @Ignore
    public void searchOrganismsTest() throws IOException, ParseException {
        List<BlastTaxonomy> organisms =  blastTaxonomyManager.searchOrganisms("Azorhizobium");
        assertNotNull(organisms);
    }

    @Test
    @Ignore
    public void searchOrganismsByIdTest() throws IOException, ParseException {
        BlastTaxonomy organism =  blastTaxonomyManager.searchOrganismById(6L);
        assertNotNull(organism);
    }

    @Test
    @Ignore
    public void readTaxonomyTest() {
        List<BlastTaxonomy> organisms =  blastTaxonomyManager.readTaxonomy();
        assertNotNull(organisms);
    }
}