package com.epam.catgenome.entity.blast;

public enum BlastDataBaseType {
    PROTEIN(1),
    NUCLEOTIDE(2);

    private Long typeId;

    BlastDataBaseType(final long typeId) {
        this.typeId = typeId;
    }

    public Long getTypeId() {
        return typeId;
    }

    /**
     * @param typeId
     * @return a {@code BlastDataBaseType} instance corresponding to the input ID
     */
    public static BlastDataBaseType getTypeById(final Long typeId) {
        for (BlastDataBaseType type : BlastDataBaseType.values()) {
            if (type.getTypeId().equals(typeId)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid Blast Data Base type ID");
    }
}
