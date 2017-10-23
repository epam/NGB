package com.epam.catgenome.controller.vo;

import com.epam.catgenome.entity.reference.Species;

/**
 * <p>
 * A View Object for Species entity representation
 * </p>
 */
public class SpeciesVO {

	private Species species;

	public Species getSpecies() {
		return species;
	}

	public void setSpecies(Species species) {
		this.species = species;
	}
}
