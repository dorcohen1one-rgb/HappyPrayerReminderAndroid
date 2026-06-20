package com.dor.happyprayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(buildContent());
        requestNotificationPermissionIfNeeded();
        ReminderScheduler.scheduleAll(this);
    }

    private void refreshContent() {
        setContentView(buildContent());
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(245, 247, 248));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(28));
        scrollView.addView(root);

        LinearLayout prayerCard = card();
        prayerCard.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(prayerCard);

        TextView heart = text("♥", 56, Color.rgb(216, 27, 96), Typeface.BOLD);
        prayerCard.addView(heart);

        TextView title = text("שכולם יהיו מאושרים ושמחים", 32, Color.rgb(24, 28, 32), Typeface.BOLD);
        title.setMaxLines(3);
        title.setEllipsize(null);
        prayerCard.addView(title);

        TextView subtitle = text("מכל העולם, מכל היצורים", 22, Color.rgb(76, 88, 94), Typeface.BOLD);
        prayerCard.addView(subtitle);

        TextView body = text("בחר כמה תזכורות שתרצה, שעה לכל תזכורת, והודעה אישית לכל שעה.", 17, Color.rgb(84, 96, 102), Typeface.NORMAL);
        body.setPadding(0, dp(8), 0, 0);
        prayerCard.addView(body);

        LinearLayout remindersCard = card();
        remindersCard.setGravity(Gravity.RIGHT);
        root.addView(remindersCard);

        TextView remindersTitle = text("זמני תזכורת", 22, Color.rgb(24, 28, 32), Typeface.BOLD);
        remindersTitle.setGravity(Gravity.RIGHT);
        remindersCard.addView(remindersTitle);

        List<ReminderSlot> slots = ReminderScheduler.getSlots(this);
        for (ReminderSlot slot : slots) {
            remindersCard.addView(rowFor(slot));
        }

        Button addReminderButton = button("הוספת תזכורת חדשה");
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

        Button permissionButton = button("לאשר התראות בטלפון");
        permissionButton.setOnClickListener(v -> requestNotificationPermissionIfNeeded());
        remindersCard.addView(permissionButton);

        Button alarmButton = button("לאשר דיוק תזכורות");
        alarmButton.setOnClickListener(v -> openExactAlarmSettingsIfNeeded());
        remindersCard.addView(alarmButton);

        LinearLayout soundCard = card();
        soundCard.setGravity(Gravity.RIGHT);
        root.addView(soundCard);

        TextView soundTitle = text("מנגינה", 22, Color.rgb(24, 28, 32), Typeface.BOLD);
        soundTitle.setGravity(Gravity.RIGHT);
        soundCard.addView(soundTitle);

        TextView soundText = text("צליל עדין וקצר יושמע בכל תזכורת.", 16, Color.rgb(84, 96, 102), Typeface.NORMAL);
        soundText.setGravity(Gravity.RIGHT);
        soundCard.addView(soundText);

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

        Button playButton = button("השמעת המנגינה");
        playButton.setOnClickListener(v -> {
            MediaPlayer player = MediaPlayer.create(this, R.raw.gentle_bell);
            player.setLooping(true);
            player.start();
            playButton.postDelayed(() -> {
                if (player.isPlaying()) player.stop();
                player.release();
            }, ReminderScheduler.getSoundSeconds(this) * 1000L);
        });
        soundCard.addView(playButton);

        return scrollView;
    }

    private View rowFor(ReminderSlot slot) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.RIGHT);
        row.setPadding(0, dp(14), 0, dp(10));

        TextView label = text(slot.title, 18, Color.rgb(24, 28, 32), Typeface.BOLD);
        label.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(label);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(controls);

        Button timeButton = new Button(this);
        timeButton.setText(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)));
        timeButton.setTextSize(18);
        controls.addView(timeButton, new LinearLayout.LayoutParams(dp(112), dp(52)));

        Button deleteButton = new Button(this);
        deleteButton.setText("מחיקה");
        deleteButton.setTextSize(15);
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

        Button saveMessageButton = button("שמירת ההודעה לשעה הזו");
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

        deleteButton.setOnClickListener(v -> {
            ReminderScheduler.deleteSlot(this, slot);
            refreshContent();
        });

        return row;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(R.drawable.card_background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(18));
        card.setLayoutParams(params);
        return card;
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
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(17);
        button.setSingleLine(false);
        button.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        params.setMargins(0, dp(12), 0, 0);
        button.setLayoutParams(params);
        return button;
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
