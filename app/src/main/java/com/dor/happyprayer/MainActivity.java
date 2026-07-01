package com.dor.happyprayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
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
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 42;
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
        scrollView.setBackground(gradient(Color.rgb(232, 248, 246), Color.rgb(250, 245, 255)));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(16), dp(22), dp(16), dp(28));
        scrollView.addView(root);

        LinearLayout prayerCard = card(Color.WHITE, Color.rgb(210, 232, 229), 24);
        prayerCard.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(prayerCard);

        TextView heart = text("♥", 62, Color.rgb(216, 27, 96), Typeface.BOLD);
        prayerCard.addView(heart);

        TextView title = text("שכולם יהיו מאושרים ושמחים", 34, Color.rgb(18, 32, 35), Typeface.BOLD);
        title.setMaxLines(3);
        title.setEllipsize(null);
        prayerCard.addView(title);

        TextView subtitle = text("תזכורות אישיות עם צליל, קול ומסך יפה", 20, Color.rgb(0, 105, 92), Typeface.BOLD);
        prayerCard.addView(subtitle);

        TextView body = text("בחר כמה תזכורות שתרצה, שעה לכל תזכורת, והודעה אישית לכל שעה.", 17, Color.rgb(84, 96, 102), Typeface.NORMAL);
        body.setPadding(0, dp(8), 0, 0);
        prayerCard.addView(body);

        LinearLayout remindersCard = card(Color.WHITE, Color.rgb(226, 235, 238), 20);
        remindersCard.setGravity(Gravity.RIGHT);
        root.addView(remindersCard);

        TextView remindersTitle = text("זמני תזכורת", 22, Color.rgb(24, 28, 32), Typeface.BOLD);
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

        LinearLayout soundCard = card(Color.WHITE, Color.rgb(226, 235, 238), 20);
        soundCard.setGravity(Gravity.RIGHT);
        root.addView(soundCard);

        TextView soundTitle = text("מנגינה", 22, Color.rgb(24, 28, 32), Typeface.BOLD);
        soundTitle.setGravity(Gravity.RIGHT);
        soundCard.addView(soundTitle);

        TextView soundText = text("בחר מה יישמע כשהתזכורת קופצת: צליל, מוזיקה רגועה, או קול שמקריא את ההודעה.", 16, Color.rgb(84, 96, 102), Typeface.NORMAL);
        soundText.setGravity(Gravity.RIGHT);
        soundCard.addView(soundText);

        Spinner soundModeSpinner = new Spinner(this);
        ArrayAdapter<String> soundModeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ReminderScheduler.SOUND_LABELS);
        soundModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        soundModeSpinner.setAdapter(soundModeAdapter);
        soundModeSpinner.setSelection(ReminderScheduler.getSoundMode(this));
        soundModeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                ReminderScheduler.saveSoundMode(MainActivity.this, position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        soundCard.addView(soundModeSpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));

        Spinner soundSpinner = new Spinner(this);
        String[] soundOptions = {"10 שניות", "20 שניות", "30 שניות", "60 שניות"};
        int[] soundValues = {10, 20, 30, 60};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, soundOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        soundSpinner.setAdapter(adapter);
        int currentSeconds = ReminderScheduler.getSoundSeconds(this);
        for (int i = 0; i < soundValues.length; i++) {
            if (soundValues[i] == currentSeconds) {
                soundSpinner.setSelection(i);
                break;
            }
        }
        soundSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                ReminderScheduler.saveSoundSeconds(MainActivity.this, soundValues[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        soundCard.addView(soundSpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));

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

    private View rowFor(ReminderSlot slot) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.RIGHT);
        row.setPadding(dp(12), dp(14), dp(12), dp(12));
        row.setBackground(cardBackground(Color.rgb(250, 253, 253), Color.rgb(226, 235, 238), 18));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(12), 0, 0);
        row.setLayoutParams(rowParams);

        TextView label = text(slot.title, 18, Color.rgb(24, 28, 32), Typeface.BOLD);
        label.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(label);

        Button timeButton = secondaryButton(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)));
        timeButton.setText(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)));
        timeButton.setTextSize(24);
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
        messageEdit.setTextColor(Color.rgb(24, 28, 32));
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
        saveMessageButton.setOnClickListener(v -> ReminderScheduler.saveMessage(this, slot, messageEdit.getText().toString()));
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
            ReminderScheduler.deleteSlot(this, slot);
            refreshContent();
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

    private GradientDrawable gradient(int top, int bottom) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
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
        return styledButton(label, Color.rgb(0, 137, 123), Color.WHITE, 18);
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
