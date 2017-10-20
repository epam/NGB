package com.epam.catgenome.entity.reference;

/**
 * {@code Species} represents a business entity designed to handle information that
 * describes a reference genome species and version.
 */
public class Species {

	private String name;
	private String version;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Species species = (Species) o;

		if (getName() != null ? !getName().equals(species.getName()) : species.getName() != null) {
			return false;
		}
		return getVersion() != null ? getVersion().equals(species.getVersion()) : species.getVersion() == null;
	}

	@Override
	public int hashCode() {
		int result = getName() != null ? getName().hashCode() : 0;
		result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
		return result;
	}
}
