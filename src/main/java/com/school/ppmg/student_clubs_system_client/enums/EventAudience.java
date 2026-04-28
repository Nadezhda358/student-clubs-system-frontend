package com.school.ppmg.student_clubs_system_client.enums;

public enum EventAudience implements DisplayText {
    ALL_STUDENTS("Всички ученици"),
    MEMBERS_ONLY("Само членове");

    private final String text;

    EventAudience(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
