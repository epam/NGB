package epam.autotests.page_objects.enums;

public enum Views {
    VARIANTS("Variants"),
    FILTER("Filter"),
    BROWSER("Browser"),
    DATASETS("Datasets"),
    TRACK_LIST("Track List"),
    SESSIONS("Sessions"),
    MOLECULAR_VIEWER("Molecular Viewer"),
    RESTORE_DEFAULT("Restore Default Layout");

    public final String value;

    Views(final String value) {
        this.value = value;
    }
}
