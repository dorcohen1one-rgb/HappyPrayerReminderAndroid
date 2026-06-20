package com.dor.happyprayer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import java.util.Calendar;

final class ReminderScheduler {
    static final String PREFS = "happy_prayer_reminders";
    static final String DEFAULT_MESSAGE = "שכולם יהיו מאושרים ושמחים\nמכל העולם, מכל היצורים";
    static final ReminderSlot[] SLOTS = {
            new ReminderSlot(1, "בוקר", 9, 0),
            new ReminderSlot(2, "לפני צהריים", 11, 30),
            new ReminderSlot(3, "צהריים", 14, 0),
            new ReminderSlot(4, "אחר הצהריים", 17, 0),
            new ReminderSlot(5, "ערב", 20, 0),
            new ReminderSlot(6, "לילה", 22, 30)
    };

    private ReminderScheduler() {
    }

    static void scheduleAll(Context context) {
        for (ReminderSlot slot : SLOTS) {
            if (isEnabled(context, slot)) {
                schedule(context, slot);
            } else {
                cancel(context, slot.id);
            }
        }
    }

    static void schedule(Context context, ReminderSlot slot) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PrayerReminderReceiver.class);
        intent.putExtra("slot_id", slot.id);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                slot.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getHour(context, slot));
        calendar.set(Calendar.MINUTE, getMinute(context, slot));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            return;
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    static void cancel(Context context, int slotId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PrayerReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                slotId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    static Intent exactAlarmSettingsIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        }
        return null;
    }

    static boolean isEnabled(Context context, ReminderSlot slot) {
        return prefs(context).getBoolean(key(slot, "enabled"), true);
    }

    static int getHour(Context context, ReminderSlot slot) {
        return prefs(context).getInt(key(slot, "hour"), slot.defaultHour);
    }

    static int getMinute(Context context, ReminderSlot slot) {
        return prefs(context).getInt(key(slot, "minute"), slot.defaultMinute);
    }

    static void save(Context context, ReminderSlot slot, boolean enabled, int hour, int minute) {
        prefs(context)
                .edit()
                .putBoolean(key(slot, "enabled"), enabled)
                .putInt(key(slot, "hour"), hour)
                .putInt(key(slot, "minute"), minute)
                .apply();
    }

    static String getMessage(Context context) {
        return prefs(context).getString("message", DEFAULT_MESSAGE);
    }

    static void saveMessage(Context context, String message) {
        String trimmed = message == null ? "" : message.trim();
        prefs(context)
                .edit()
                .putString("message", trimmed.isEmpty() ? DEFAULT_MESSAGE : trimmed)
                .apply();
    }

    static boolean isPopupEnabled(Context context) {
        return prefs(context).getBoolean("popup_enabled", true);
    }

    static void savePopupEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("popup_enabled", enabled).apply();
    }

    static int getSoundSeconds(Context context) {
        return prefs(context).getInt("sound_seconds", 20);
    }

    static void saveSoundSeconds(Context context, int seconds) {
        prefs(context).edit().putInt("sound_seconds", seconds).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(ReminderSlot slot, String name) {
        return "slot_" + slot.id + "_" + name;
    }
}
