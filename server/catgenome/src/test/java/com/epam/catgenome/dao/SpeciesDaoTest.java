package com.epam.catgenome.dao;

import com.epam.catgenome.dao.reference.SpeciesDao;
import com.epam.catgenome.entity.reference.Species;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class SpeciesDaoTest {

	private final static String TEST_NAME = "TEST_NAME";
	private final static String TEST_VERSION = "TEST_VERSION";

	@Autowired
	private SpeciesDao speciesDao;

	@Test
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void testSaveAndLoad() throws Exception {
		Species testSpecies = new Species();
		testSpecies.setName(TEST_NAME);
		testSpecies.setVersion(TEST_VERSION);

		speciesDao.saveSpecies(testSpecies);

		Species species = speciesDao.loadSpeciesByVersion(TEST_VERSION);
		Assert.assertNotNull(species);
		Assert.assertEquals(species.getName(), TEST_NAME);
		Assert.assertEquals(species.getVersion(), TEST_VERSION);
	}

	@Test
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void testLoadAllSpecies() throws Exception {
		for (int i = 0; i < 3; i++) {
			Species species = new Species();
			species.setName(TEST_NAME);
			species.setVersion(TEST_VERSION + i);
			speciesDao.saveSpecies(species);
		}
		List<Species> speciesList = speciesDao.loadAllSpecies();
		Assert.assertEquals(speciesList.size(), 3);
	}

}