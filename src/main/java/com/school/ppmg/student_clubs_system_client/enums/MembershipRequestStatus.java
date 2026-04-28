package com.school.ppmg.student_clubs_system_client.enums;

public enum MembershipRequestStatus implements DisplayText {
    PENDING("Изчаква преглед"),
    APPROVED("Одобрена"),
    REJECTED("Отхвърлена"),
    CANCELLED("Отменена");

    private final String text;

    MembershipRequestStatus(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
