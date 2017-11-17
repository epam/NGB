package com.epam.ngb.cli.entity;

import com.epam.ngb.cli.manager.printer.Printable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code {@link SpeciesEntity}} is an entity,  representing a species on NGB server.
 * Species support only unique versions.
 */
public class SpeciesEntity implements Printable<SpeciesEntity> {

	/**
	 * Species name.
	 */
	String name;

	/**
	 * Species version.
	 */
	String version;

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

	/**
	 * Calculates a formatting string for a {@code List} of {@code SpeciesEntity} objects
	 * for table output. Method calculates the width of columns for {@code SpeciesEntity}'s fields
	 * from the length of all items' fields in the list. Printed columns are defined by
	 * {@code FieldFormat}, width of each column will be equal to the width of the field with
	 * the longest string value.
	 * @param table {@code List} of {@code SpeciesEntity} for formatting
	 * @return format String to be applied to the {@param table} printing
	 */
	@Override
	public String getFormatString(List<SpeciesEntity> table) {
		Map<FieldFormat, Integer> formatMap = new EnumMap<>(FieldFormat.class);
		for (FieldFormat field : FieldFormat.values()) {
			formatMap.put(field, field.name().length());
		}
		getItemFormat(table, formatMap);
		return formatMap.values().stream().map(v -> "%" + (v+1) + "s").collect(Collectors.joining());
	}

	/**
	 * Creates a String representation of {@code SpeciesEntity} according to the {@param formatString}
	 * @param formatString to apply to {@code SpeciesEntity} formatting
	 * @return a String representation of {@code SpeciesEntity}
	 */
	@Override
	public String formatItem(String formatString) {
		return String.format(formatString, name, version);
	}

	@Override
	public String formatHeader(String formatString) {
		String[] names = Arrays.stream(FieldFormat.values()).map(Enum::name).toArray(String[]::new);
		return String.format(formatString, (Object[]) names);
	}

	private void getItemFormat(List<SpeciesEntity> table, Map<FieldFormat, Integer> formatMap) {
		for (SpeciesEntity species : table) {
			calculateFieldWidth(formatMap, FieldFormat.NAME, species.getName());
			calculateFieldWidth(formatMap, FieldFormat.VERSION, species.getVersion());
		}
	}

	private void calculateFieldWidth(Map<FieldFormat, Integer> formatMap, FieldFormat field, String value) {
		if (value == null) {
			return;
		}
		if (formatMap.get(field) < value.length()) {
			formatMap.put(field, value.length());
		}
	}

	/**
	 * Represent the fields of the {@code Project}, that will be included in the table for printing
	 */
	private enum FieldFormat {
		NAME,
		VERSION
	}
}
