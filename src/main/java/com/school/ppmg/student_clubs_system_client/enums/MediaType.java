package com.school.ppmg.student_clubs_system_client.enums;

public enum MediaType implements DisplayText {
    IMAGE("Изображение"),
    FILE("Файл"),
    LINK("Връзка");

    private final String text;

    MediaType(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
