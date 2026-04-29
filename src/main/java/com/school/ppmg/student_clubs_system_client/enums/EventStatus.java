package com.school.ppmg.student_clubs_system_client.enums;

public enum EventStatus implements DisplayText {
    DRAFT("Чернова"),
    PUBLISHED("Публикувано"),
    CANCELLED("Отменено");

    private final String text;

    EventStatus(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
