package com.dor.happyprayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 42;
    private static final int INK = Color.rgb(17, 24, 39);
    private static final int MUTED = Color.rgb(91, 104, 114);
    private static final int TEAL = Color.rgb(0, 137, 123);
    private static final int ROSE = Color.rgb(216, 27, 96);
    private static final int GOLD = Color.rgb(191, 138, 0);
    private ReminderSoundPlayer previewPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(buildContent());
        requestNotificationPermissionIfNeeded();
        ReminderScheduler.scheduleAll(this);
    }

    @Override
    protected void onDestroy() {
        if (previewPlayer != null) previewPlayer.stop();
        super.onDestroy();
    }

    private void refreshContent() {
        setContentView(buildContent());
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(gradient(Color.rgb(228, 247, 244), Color.rgb(255, 250, 236), Color.rgb(245, 241, 255)));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(16), dp(22), dp(16), dp(28));
        scrollView.addView(root);

        LinearLayout prayerCard = gradientCard(
                new int[]{Color.rgb(255, 255, 255), Color.rgb(239, 251, 248), Color.rgb(255, 247, 232)},
                Color.rgb(188, 224, 218),
                28
        );
        prayerCard.setGravity(Gravity.CENTER_HORIZONTAL);
        prayerCard.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.addView(prayerCard);

        TextView heart = text("♥", 70, ROSE, Typeface.BOLD);
        prayerCard.addView(heart);

        TextView title = text("שכולם יהיו מאושרים ושמחים", 36, INK, Typeface.BOLD);
        title.setMaxLines(3);
        title.setEllipsize(null);
        prayerCard.addView(title);

        TextView subtitle = text("תזכורות אישיות עם מנטרה, קול ומסך נעים", 20, TEAL, Typeface.BOLD);
        prayerCard.addView(subtitle);

        TextView body = text("בחר כמה תזכורות שתרצה, שעה לכל תזכורת, והודעה אישית לכל רגע ביום.", 17, MUTED, Typeface.NORMAL);
        body.setPadding(0, dp(8), 0, 0);
        prayerCard.addView(body);

        prayerCard.addView(metricRow());

        LinearLayout statusCard = gradientCard(
                new int[]{Color.rgb(19, 44, 47), Color.rgb(0, 105, 92)},
                Color.rgb(168, 218, 210),
                22
        );
        statusCard.setGravity(Gravity.RIGHT);
        root.addView(statusCard);

        TextView statusTitle = text("מה קורה היום", 22, Color.WHITE, Typeface.BOLD);
        statusTitle.setGravity(Gravity.RIGHT);
        statusCard.addView(statusTitle);

        TextView nextReminder = text(nextReminderSummary(), 18, Color.rgb(232, 248, 246), Typeface.BOLD);
        nextReminder.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nextParams.setMargins(0, dp(8), 0, 0);
        statusCard.addView(nextReminder, nextParams);

        TextView permissionStatus = text(permissionSummary(), 15, Color.rgb(222, 245, 241), Typeface.NORMAL);
        permissionStatus.setGravity(Gravity.RIGHT);
        statusCard.addView(permissionStatus);

        Button testButton = primaryButton("בדיקת תזכורת עכשיו");
        testButton.setOnClickListener(v -> openTestReminder());
        statusCard.addView(testButton);

        LinearLayout remindersCard = card(Color.WHITE, Color.rgb(221, 231, 235), 20);
        remindersCard.setGravity(Gravity.RIGHT);
        root.addView(remindersCard);

        TextView remindersTitle = text("זמני תזכורת", 22, INK, Typeface.BOLD);
        remindersTitle.setGravity(Gravity.RIGHT);
        remindersCard.addView(remindersTitle);

        List<ReminderSlot> slots = ReminderScheduler.getSlots(this);
        for (ReminderSlot slot : slots) {
            remindersCard.addView(rowFor(slot));
        }

        Button addReminderButton = primaryButton("הוספת תזכורת חדשה");
        addReminderButton.setOnClickListener(v -> {
            ReminderSlot slot = ReminderScheduler.addSlot(this);
            ReminderScheduler.schedule(this, slot);
            refreshContent();
        });
        remindersCard.addView(addReminderButton);

        CheckBox popupCheckBox = new CheckBox(this);
        popupCheckBox.setText("להקפיץ את ההודעה יפה על המסך בזמן התזכורת");
        popupCheckBox.setTextSize(17);
        popupCheckBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        popupCheckBox.setTextDirection(View.TEXT_DIRECTION_RTL);
        popupCheckBox.setChecked(ReminderScheduler.isPopupEnabled(this));
        popupCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> ReminderScheduler.savePopupEnabled(this, isChecked));
        remindersCard.addView(popupCheckBox);

        Button permissionButton = secondaryButton("לאשר התראות בטלפון");
        permissionButton.setOnClickListener(v -> requestNotificationPermissionIfNeeded());
        remindersCard.addView(permissionButton);

        Button alarmButton = secondaryButton("לאשר דיוק תזכורות");
        alarmButton.setOnClickListener(v -> openExactAlarmSettingsIfNeeded());
        remindersCard.addView(alarmButton);

        LinearLayout soundCard = gradientCard(
                new int[]{Color.WHITE, Color.rgb(250, 253, 252)},
                Color.rgb(221, 231, 235),
                20
        );
        soundCard.setGravity(Gravity.RIGHT);
        root.addView(soundCard);

        TextView soundTitle = text("צליל וקול", 22, INK, Typeface.BOLD);
        soundTitle.setGravity(Gravity.RIGHT);
        soundCard.addView(soundTitle);

        TextView soundText = text("בחר מה יישמע כשהתזכורת קופצת: פעמון, מוזיקה רגועה, קול שמקריא את ההודעה, או מנטרה עם רקע שקט.", 16, MUTED, Typeface.NORMAL);
        soundText.setGravity(Gravity.RIGHT);
        soundCard.addView(soundText);

        soundCard.addView(soundModeOptions());
        soundCard.addView(durationOptions());

        Button playButton = primaryButton("השמעת הצליל שבחרתי");
        playButton.setOnClickListener(v -> {
            if (previewPlayer == null) previewPlayer = new ReminderSoundPlayer();
            int playbackSeconds = ReminderScheduler.getPlaybackSeconds(this);
            previewPlayer.play(
                    this,
                    ReminderScheduler.getSoundMode(this),
                    ReminderScheduler.getMessage(this),
                    playbackSeconds
            );
            playButton.postDelayed(() -> {
                if (previewPlayer != null) previewPlayer.stop();
            }, playbackSeconds * 1000L);
        });
        soundCard.addView(playButton);

        Button stopPreviewButton = secondaryButton("עצירת השמעה");
        stopPreviewButton.setOnClickListener(v -> {
            if (previewPlayer != null) previewPlayer.stop();
        });
        soundCard.addView(stopPreviewButton);

        Button voiceSettingsButton = secondaryButton("הגדרות קול בטלפון");
        voiceSettingsButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
            } catch (RuntimeException ignored) {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            }
        });
        soundCard.addView(voiceSettingsButton);

        return scrollView;
    }

    private View metricRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(18), 0, 0);
        row.setLayoutParams(params);

        List<ReminderSlot> slots = ReminderScheduler.getSlots(this);
        int active = 0;
        for (ReminderSlot slot : slots) {
            if (ReminderScheduler.isEnabled(this, slot)) active++;
        }
        row.addView(chip("פעילות " + active, Color.rgb(232, 248, 246), TEAL), new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(chip(ReminderScheduler.SOUND_LABELS[ReminderScheduler.getSoundMode(this)], Color.rgb(255, 247, 232), GOLD), new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(chip(ReminderScheduler.isPopupEnabled(this) ? "מסך קופץ" : "התראה בלבד", Color.rgb(255, 239, 246), ROSE), new LinearLayout.LayoutParams(0, dp(44), 1));
        return row;
    }

    private View soundModeOptions() {
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(12), 0, 0);
        options.setLayoutParams(params);

        int selected = ReminderScheduler.getSoundMode(this);
        for (int i = 0; i < ReminderScheduler.SOUND_LABELS.length; i++) {
            final int mode = i;
            Button option = selectableButton(ReminderScheduler.SOUND_LABELS[i], i == selected);
            option.setOnClickListener(v -> {
                ReminderScheduler.saveSoundMode(this, mode);
                Toast.makeText(this, "הצליל נבחר", Toast.LENGTH_SHORT).show();
                refreshContent();
            });
            options.addView(option);
        }
        return options;
    }

    private View durationOptions() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(params);

        String[] labels = {"10 ש׳", "20 ש׳", "30 ש׳", "60 ש׳"};
        int[] values = {10, 20, 30, 60};
        int selected = ReminderScheduler.getSoundSeconds(this);
        for (int i = 0; i < values.length; i++) {
            final int seconds = values[i];
            Button option = selectableButton(labels[i], seconds == selected);
            option.setTextSize(14);
            option.setOnClickListener(v -> {
                ReminderScheduler.saveSoundSeconds(this, seconds);
                Toast.makeText(this, "משך הצליל נשמר", Toast.LENGTH_SHORT).show();
                refreshContent();
            });
            row.addView(option, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        return row;
    }

    private View rowFor(ReminderSlot slot) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.RIGHT);
        row.setPadding(dp(14), dp(16), dp(14), dp(14));
        row.setBackground(cardBackground(Color.rgb(251, 254, 253), Color.rgb(222, 233, 236), 18));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(12), 0, 0);
        row.setLayoutParams(rowParams);

        TextView label = text(slot.title, 19, INK, Typeface.BOLD);
        label.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(label);

        Button timeButton = secondaryButton(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)));
        timeButton.setText(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)));
        timeButton.setTextSize(27);
        row.addView(timeButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        LinearLayout timeControls = new LinearLayout(this);
        timeControls.setOrientation(LinearLayout.HORIZONTAL);
        timeControls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams timeControlsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeControlsParams.setMargins(0, dp(8), 0, 0);
        row.addView(timeControls, timeControlsParams);

        Button hourPlus = compactButton("+ שעה");
        Button hourMinus = compactButton("- שעה");
        Button minutePlus = compactButton("+ 5 דק׳");
        Button minuteMinus = compactButton("- 5 דק׳");
        timeControls.addView(hourPlus, new LinearLayout.LayoutParams(0, dp(46), 1));
        timeControls.addView(hourMinus, new LinearLayout.LayoutParams(0, dp(46), 1));
        timeControls.addView(minutePlus, new LinearLayout.LayoutParams(0, dp(46), 1));
        timeControls.addView(minuteMinus, new LinearLayout.LayoutParams(0, dp(46), 1));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        controlsParams.setMargins(0, dp(8), 0, 0);
        row.addView(controls, controlsParams);

        Button deleteButton = secondaryButton("מחיקה");
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(92), dp(52));
        deleteParams.setMargins(dp(8), 0, dp(8), 0);
        controls.addView(deleteButton, deleteParams);

        Switch toggle = new Switch(this);
        toggle.setChecked(ReminderScheduler.isEnabled(this, slot));
        controls.addView(toggle);

        EditText messageEdit = new EditText(this);
        messageEdit.setText(ReminderScheduler.getMessage(this, slot));
        messageEdit.setTextSize(17);
        messageEdit.setTextColor(INK);
        messageEdit.setGravity(Gravity.RIGHT | Gravity.TOP);
        messageEdit.setTextDirection(View.TEXT_DIRECTION_RTL);
        messageEdit.setMinLines(2);
        messageEdit.setSingleLine(false);
        messageEdit.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, dp(8), 0, 0);
        row.addView(messageEdit, messageParams);

        Button saveMessageButton = primaryButton("שמירת ההודעה לשעה הזו");
        saveMessageButton.setOnClickListener(v -> {
            ReminderScheduler.saveMessage(this, slot, messageEdit.getText().toString());
            Toast.makeText(this, "ההודעה נשמרה", Toast.LENGTH_SHORT).show();
        });
        row.addView(saveMessageButton);

        CompoundButton.OnCheckedChangeListener saveToggle = (buttonView, isChecked) -> {
            int[] hm = parseTime(timeButton.getText().toString());
            ReminderScheduler.save(this, slot, isChecked, hm[0], hm[1]);
            ReminderScheduler.scheduleAll(this);
        };
        toggle.setOnCheckedChangeListener(saveToggle);

        timeButton.setOnClickListener(v -> {
            int currentHour = ReminderScheduler.getHour(this, slot);
            int currentMinute = ReminderScheduler.getMinute(this, slot);
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeButton.setText(timeText(hourOfDay, minute));
                ReminderScheduler.save(this, slot, toggle.isChecked(), hourOfDay, minute);
                ReminderScheduler.scheduleAll(this);
            }, currentHour, currentMinute, true).show();
        });

        hourPlus.setOnClickListener(v -> adjustTime(slot, toggle.isChecked(), timeButton, 60));
        hourMinus.setOnClickListener(v -> adjustTime(slot, toggle.isChecked(), timeButton, -60));
        minutePlus.setOnClickListener(v -> adjustTime(slot, toggle.isChecked(), timeButton, 5));
        minuteMinus.setOnClickListener(v -> adjustTime(slot, toggle.isChecked(), timeButton, -5));

        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("למחוק את התזכורת?")
                    .setMessage(slot.title)
                    .setPositiveButton("מחיקה", (dialog, which) -> {
                        ReminderScheduler.deleteSlot(this, slot);
                        Toast.makeText(this, "התזכורת נמחקה", Toast.LENGTH_SHORT).show();
                        refreshContent();
                    })
                    .setNegativeButton("ביטול", null)
                    .show();
        });

        return row;
    }

    private LinearLayout card() {
        return card(Color.WHITE, Color.rgb(226, 235, 238), 16);
    }

    private LinearLayout card(int color, int strokeColor, int radiusDp) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground(color, strokeColor, radiusDp));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(18));
        card.setLayoutParams(params);
        return card;
    }

    private LinearLayout gradientCard(int[] colors, int strokeColor, int radiusDp) {
        LinearLayout card = card(Color.WHITE, strokeColor, radiusDp);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        card.setBackground(drawable);
        return card;
    }

    private GradientDrawable gradient(int... colors) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
    }

    private GradientDrawable cardBackground(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setGravity(Gravity.CENTER);
        textView.setTextDirection(View.TEXT_DIRECTION_RTL);
        textView.setLineSpacing(dp(2), 1.0f);
        return textView;
    }

    private Button button(String label) {
        return styledButton(label, Color.rgb(0, 137, 123), Color.WHITE, 18);
    }

    private Button primaryButton(String label) {
        return styledButton(label, TEAL, Color.WHITE, 18);
    }

    private Button secondaryButton(String label) {
        return styledButton(label, Color.rgb(232, 248, 246), Color.rgb(0, 105, 92), 16);
    }

    private Button compactButton(String label) {
        Button button = styledButton(label, Color.rgb(245, 247, 248), Color.rgb(24, 28, 32), 14);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(46)
        );
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private Button selectableButton(String label, boolean selected) {
        int background = selected ? Color.rgb(0, 105, 92) : Color.rgb(246, 253, 251);
        int foreground = selected ? Color.WHITE : INK;
        int stroke = selected ? Color.rgb(0, 105, 92) : Color.rgb(214, 232, 229);
        Button button = styledButton(label, background, foreground, 16);
        button.setGravity(Gravity.CENTER);
        button.setTextDirection(View.TEXT_DIRECTION_RTL);
        button.setBackground(cardBackground(background, stroke, 16));
        return button;
    }

    private TextView chip(String label, int backgroundColor, int textColor) {
        TextView chip = text(label, 13, textColor, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setBackground(cardBackground(backgroundColor, Color.TRANSPARENT, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        );
        params.setMargins(dp(3), 0, dp(3), 0);
        chip.setLayoutParams(params);
        return chip;
    }

    private Button styledButton(String label, int backgroundColor, int textColor, int textSize) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(textSize);
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setSingleLine(false);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setBackground(cardBackground(backgroundColor, Color.TRANSPARENT, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        params.setMargins(0, dp(12), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void adjustTime(ReminderSlot slot, boolean enabled, Button timeButton, int deltaMinutes) {
        int hour = ReminderScheduler.getHour(this, slot);
        int minute = ReminderScheduler.getMinute(this, slot);
        int total = (hour * 60 + minute + deltaMinutes) % (24 * 60);
        if (total < 0) total += 24 * 60;
        int newHour = total / 60;
        int newMinute = total % 60;
        timeButton.setText(timeText(newHour, newMinute));
        ReminderScheduler.save(this, slot, enabled, newHour, newMinute);
        ReminderScheduler.scheduleAll(this);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void openExactAlarmSettingsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = getSystemService(AlarmManager.class);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = ReminderScheduler.exactAlarmSettingsIntent();
                if (intent != null) startActivity(intent);
            }
        }
    }

    private void openTestReminder() {
        List<ReminderSlot> slots = ReminderScheduler.getSlots(this);
        int slotId = slots.isEmpty() ? 0 : slots.get(0).id;
        Intent intent = new Intent(this, ReminderPopupActivity.class);
        intent.putExtra("slot_id", slotId);
        intent.putExtra("is_test", true);
        startActivity(intent);
    }

    private String nextReminderSummary() {
        List<ReminderSlot> slots = ReminderScheduler.getSlots(this);
        ReminderSlot nextSlot = null;
        long nextMillis = Long.MAX_VALUE;
        for (ReminderSlot slot : slots) {
            if (!ReminderScheduler.isEnabled(this, slot)) continue;
            long trigger = ReminderScheduler.nextTriggerMillis(this, slot);
            if (trigger < nextMillis) {
                nextMillis = trigger;
                nextSlot = slot;
            }
        }
        if (nextSlot == null) return "אין כרגע תזכורות פעילות";

        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(nextMillis));
        return "התזכורת הבאה: " + nextSlot.title + " בשעה " + time;
    }

    private String permissionSummary() {
        boolean notificationsOk = Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean exactOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = getSystemService(AlarmManager.class);
            exactOk = alarmManager == null || alarmManager.canScheduleExactAlarms();
        }
        if (notificationsOk && exactOk) return "ההתראות והדיוק מוכנים";
        if (!notificationsOk && !exactOk) return "צריך לאשר התראות וגם דיוק תזכורות";
        if (!notificationsOk) return "צריך לאשר התראות בטלפון";
        return "מומלץ לאשר דיוק תזכורות";
    }

    private String timeText(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private int[] parseTime(String value) {
        String[] parts = value.split(":");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
