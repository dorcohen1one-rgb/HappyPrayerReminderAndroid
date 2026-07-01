package com.dor.happyprayer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class ReminderScheduler {
    static final String PREFS = "happy_prayer_reminders";
    static final String DEFAULT_MESSAGE = "שכולם יהיו מאושרים ושמחים\nמכל העולם, מכל היצורים";
    static final int SOUND_BELL = 0;
    static final int SOUND_CALM_PAD = 1;
    static final int SOUND_SOFT_CHIMES = 2;
    static final int SOUND_VOICE = 3;
    static final int SOUND_MANTRA = 4;
    static final String[] SOUND_LABELS = {
            "פעמון עדין",
            "מוזיקה רגועה",
            "צלצולים עדינים",
            "קול שמקריא את ההודעה",
            "מנטרה בקול עם מוזיקה"
    };
    private static final int[][] DEFAULT_TIMES = {
            {9, 0},
            {14, 0},
            {20, 0}
    };

    private ReminderScheduler() {
    }

    static void scheduleAll(Context context) {
        for (ReminderSlot slot : getSlots(context)) {
            if (isEnabled(context, slot)) {
                schedule(context, slot);
            } else {
                cancel(context, slot.id);
            }
        }
    }

    static void schedule(Context context, ReminderSlot slot) {
        scheduleAt(context, slot.id, nextTriggerMillis(context, slot));
    }

    static void scheduleInMinutes(Context context, int slotId, int minutes) {
        int safeMinutes = Math.max(1, minutes);
        scheduleAt(context, slotId, System.currentTimeMillis() + safeMinutes * 60_000L);
    }

    static long nextTriggerMillis(Context context, ReminderSlot slot) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getHour(context, slot));
        calendar.set(Calendar.MINUTE, getMinute(context, slot));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }

    private static void scheduleAt(Context context, int slotId, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PrayerReminderReceiver.class);
        intent.putExtra("slot_id", slotId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                slotId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            return;
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
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

    static List<ReminderSlot> getSlots(Context context) {
        SharedPreferences preferences = prefs(context);
        String idsValue = preferences.getString("slot_ids", null);
        if (idsValue == null || idsValue.trim().isEmpty()) {
            initializeDefaultSlots(context);
            idsValue = prefs(context).getString("slot_ids", "");
        }

        List<ReminderSlot> slots = new ArrayList<>();
        String[] parts = idsValue.split(",");
        for (String part : parts) {
            if (part.trim().isEmpty()) continue;
            int id;
            try {
                id = Integer.parseInt(part.trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            slots.add(new ReminderSlot(
                    id,
                    preferences.getString(key(id, "title"), "תזכורת " + id),
                    preferences.getInt(key(id, "hour"), 9),
                    preferences.getInt(key(id, "minute"), 0)
            ));
        }
        return slots;
    }

    static ReminderSlot getSlot(Context context, int slotId) {
        for (ReminderSlot slot : getSlots(context)) {
            if (slot.id == slotId) return slot;
        }
        return null;
    }

    static ReminderSlot addSlot(Context context) {
        SharedPreferences preferences = prefs(context);
        int nextId = preferences.getInt("next_slot_id", 1);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minute = 0;
        int displayNumber = getSlots(context).size() + 1;
        ReminderSlot slot = new ReminderSlot(nextId, "תזכורת " + displayNumber, hour, minute);

        String ids = preferences.getString("slot_ids", "");
        String newIds = ids == null || ids.trim().isEmpty() ? String.valueOf(nextId) : ids + "," + nextId;
        preferences.edit()
                .putString("slot_ids", newIds)
                .putInt("next_slot_id", nextId + 1)
                .putString(key(nextId, "title"), slot.title)
                .putBoolean(key(nextId, "enabled"), true)
                .putInt(key(nextId, "hour"), hour)
                .putInt(key(nextId, "minute"), minute)
                .putString(key(nextId, "message"), DEFAULT_MESSAGE)
                .apply();
        return slot;
    }

    static void deleteSlot(Context context, ReminderSlot slot) {
        cancel(context, slot.id);
        SharedPreferences preferences = prefs(context);
        StringBuilder ids = new StringBuilder();
        for (ReminderSlot existing : getSlots(context)) {
            if (existing.id == slot.id) continue;
            if (ids.length() > 0) ids.append(",");
            ids.append(existing.id);
        }
        preferences.edit()
                .putString("slot_ids", ids.toString())
                .remove(key(slot.id, "title"))
                .remove(key(slot.id, "enabled"))
                .remove(key(slot.id, "hour"))
                .remove(key(slot.id, "minute"))
                .remove(key(slot.id, "message"))
                .apply();
        scheduleAll(context);
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

    static String getMessage(Context context, ReminderSlot slot) {
        return prefs(context).getString(key(slot, "message"), getMessage(context));
    }

    static String getMessage(Context context, int slotId) {
        ReminderSlot slot = getSlot(context, slotId);
        if (slot == null) return getMessage(context);
        return getMessage(context, slot);
    }

    static void saveMessage(Context context, ReminderSlot slot, String message) {
        String trimmed = message == null ? "" : message.trim();
        prefs(context)
                .edit()
                .putString(key(slot, "message"), trimmed.isEmpty() ? DEFAULT_MESSAGE : trimmed)
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

    static int getPlaybackSeconds(Context context) {
        int seconds = getSoundSeconds(context);
        int mode = getSoundMode(context);
        if (mode == SOUND_VOICE || mode == SOUND_MANTRA) {
            return Math.max(seconds, 35);
        }
        return seconds;
    }

    static int getSoundMode(Context context) {
        int mode = prefs(context).getInt("sound_mode", SOUND_BELL);
        if (mode < 0 || mode >= SOUND_LABELS.length) return SOUND_BELL;
        return mode;
    }

    static void saveSoundMode(Context context, int mode) {
        prefs(context).edit().putInt("sound_mode", mode).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(ReminderSlot slot, String name) {
        return key(slot.id, name);
    }

    private static String key(int slotId, String name) {
        return "slot_" + slotId + "_" + name;
    }

    private static void initializeDefaultSlots(Context context) {
        SharedPreferences preferences = prefs(context);
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < DEFAULT_TIMES.length; i++) {
            int id = i + 1;
            if (ids.length() > 0) ids.append(",");
            ids.append(id);
            editor.putString(key(id, "title"), defaultTitle(i));
            editor.putBoolean(key(id, "enabled"), true);
            editor.putInt(key(id, "hour"), preferences.getInt(key(id, "hour"), DEFAULT_TIMES[i][0]));
            editor.putInt(key(id, "minute"), preferences.getInt(key(id, "minute"), DEFAULT_TIMES[i][1]));
            editor.putString(key(id, "message"), preferences.getString(key(id, "message"), DEFAULT_MESSAGE));
        }
        editor.putString("slot_ids", ids.toString());
        editor.putInt("next_slot_id", DEFAULT_TIMES.length + 1);
        editor.apply();
    }

    private static String defaultTitle(int index) {
        if (index == 0) return "בוקר";
        if (index == 1) return "צהריים";
        if (index == 2) return "ערב";
        return String.format(Locale.US, "תזכורת %d", index + 1);
    }
}
