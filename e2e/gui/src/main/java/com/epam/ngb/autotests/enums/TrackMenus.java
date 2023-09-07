package com.epam.ngb.autotests.enums;

public enum TrackMenus {

    GENERAL("General"),
    TRANSCRIPT_VIEW("Transcript View:"),
    FEATURES("Features:"),
    VARIANTS_VIEW("Variants View:");

    public final String value;

    TrackMenus(final String value) {
        this.value = value;
    }
}
