package com.school.ppmg.student_clubs_system_client.enums;

public enum ReportPeriod implements DisplayText {
    DAY("Ден"),
    WEEK("Седмица"),
    MONTH("Месец");

    private final String text;

    ReportPeriod(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
