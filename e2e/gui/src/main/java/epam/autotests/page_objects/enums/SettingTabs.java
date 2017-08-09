package epam.autotests.page_objects.enums;

public enum SettingTabs {

    GENERAL("GENERAL"),
    ALIGNMENTS("ALIGNMENTS"),
    GFF_GTF("GFF/GTF"),
    CUSTOMIZE("CUSTOMIZE");

    public final String value;

    SettingTabs(final String value) {
        this.value = value;
    }
}
