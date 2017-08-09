package epam.autotests.page_objects.enums;

public enum VarTableColumns {

    TYPE("Type"),
    CHR("Chr"),
    GENE("Gene"),
    POSITION("Position");

    public final String value;

    VarTableColumns(final String value) {
        this.value = value;
    }
}