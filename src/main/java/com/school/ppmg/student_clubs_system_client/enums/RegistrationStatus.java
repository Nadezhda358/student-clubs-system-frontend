package com.school.ppmg.student_clubs_system_client.enums;

public enum RegistrationStatus implements DisplayText {
    REGISTERED("Регистриран"),
    CANCELLED("Отменена");

    private final String text;

    RegistrationStatus(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
