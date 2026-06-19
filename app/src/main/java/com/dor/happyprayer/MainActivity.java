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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

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

        TextView body = text("הלוואי שכל מי שצריך אור, נחמה ושמחה יקבל אותם עכשיו.", 17, Color.rgb(84, 96, 102), Typeface.NORMAL);
        body.setPadding(0, dp(8), 0, 0);
        prayerCard.addView(body);

        LinearLayout remindersCard = card();
        remindersCard.setGravity(Gravity.RIGHT);
        root.addView(remindersCard);

        TextView remindersTitle = text("זמני תזכורת", 22, Color.rgb(24, 28, 32), Typeface.BOLD);
        remindersTitle.setGravity(Gravity.RIGHT);
        remindersCard.addView(remindersTitle);

        for (ReminderSlot slot : ReminderScheduler.SLOTS) {
            remindersCard.addView(rowFor(slot));
        }

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

        Button playButton = button("השמעת המנגינה");
        playButton.setOnClickListener(v -> MediaPlayer.create(this, R.raw.gentle_bell).start());
        soundCard.addView(playButton);

        return scrollView;
    }

    private View rowFor(ReminderSlot slot) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.setPadding(0, dp(12), 0, dp(8));

        Button timeButton = new Button(this);
        timeButton.setText(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)));
        timeButton.setTextSize(18);
        row.addView(timeButton, new LinearLayout.LayoutParams(dp(128), dp(52)));

        TextView label = text(slot.title, 18, Color.rgb(24, 28, 32), Typeface.BOLD);
        label.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(0, dp(52), 1));

        Switch toggle = new Switch(this);
        toggle.setChecked(ReminderScheduler.isEnabled(this, slot));
        row.addView(toggle);

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
