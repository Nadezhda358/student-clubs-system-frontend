package com.school.ppmg.student_clubs_system_client.enums;

public enum MembershipStatus implements DisplayText {
    ACTIVE("Активно"),
    LEFT("Напуснал"),
    BANNED("Забранен");

    private final String text;

    MembershipStatus(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
