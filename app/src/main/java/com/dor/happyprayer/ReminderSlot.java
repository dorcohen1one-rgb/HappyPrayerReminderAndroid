package com.dor.happyprayer;

final class ReminderSlot {
    final int id;
    final String title;
    final int defaultHour;
    final int defaultMinute;

    ReminderSlot(int id, String title, int defaultHour, int defaultMinute) {
        this.id = id;
        this.title = title;
        this.defaultHour = defaultHour;
        this.defaultMinute = defaultMinute;
    }
}
