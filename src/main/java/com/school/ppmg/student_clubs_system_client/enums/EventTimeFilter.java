package com.school.ppmg.student_clubs_system_client.enums;

public enum EventTimeFilter implements DisplayText {
    UPCOMING("Предстоящи"),
    PAST("Минали"),
    ALL("Всички");

    private final String text;

    EventTimeFilter(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
