package com.school.ppmg.student_clubs_system_client.enums;

public enum UserRole implements DisplayText {
    STUDENT("Ученик"),
    TEACHER("Учител"),
    ADMIN("Администратор");

    private final String text;

    UserRole(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
