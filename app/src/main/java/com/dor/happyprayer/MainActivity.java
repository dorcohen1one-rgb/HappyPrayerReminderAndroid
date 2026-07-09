package com.dor.happyprayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
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
    private boolean contentReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        styleSystemBars();

        setContentView(buildContent());
        contentReady = true;
        requestNotificationPermissionIfNeeded();
        ReminderScheduler.scheduleAll(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (contentReady) refreshContent();
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
        scrollView.setBackground(gradient(Color.rgb(218, 245, 241), Color.rgb(255, 250, 237), Color.rgb(246, 241, 255)));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(14), dp(18), dp(14), dp(28));
        scrollView.addView(root);

        LinearLayout prayerCard = gradientCard(
                new int[]{Color.rgb(255, 255, 255), Color.rgb(238, 252, 248), Color.rgb(255, 246, 227)},
                Color.rgb(177, 222, 214),
                26
        );
        prayerCard.setGravity(Gravity.CENTER_HORIZONTAL);
        prayerCard.setPadding(dp(18), dp(20), dp(18), dp(22));
        root.addView(prayerCard);

        AmbientHeartView ambientHeartView = new AmbientHeartView(this);
        prayerCard.addView(ambientHeartView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(172)
        ));

        TextView title = text("שכולם יהיו מאושרים ושמחים", 34, INK, Typeface.BOLD);
        title.setMaxLines(3);
        title.setEllipsize(null);
        prayerCard.addView(title);

        TextView subtitle = text("רגע קטן ביום שפותח את הלב", 20, TEAL, Typeface.BOLD);
        prayerCard.addView(subtitle);

        TextView body = text("תזכורות אישיות עם מנטרה, קול, מוזיקה עדינה ומסך שמרגיש כמו עצירה טובה.", 16, MUTED, Typeface.NORMAL);
        body.setPadding(0, dp(8), 0, 0);
        prayerCard.addView(body);

        prayerCard.addView(metricRow());

        LinearLayout statusCard = gradientCard(
                new int[]{Color.rgb(12, 37, 43), Color.rgb(0, 105, 92), Color.rgb(15, 150, 126)},
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

        Button realTestButton = lightButton("בדיקת התראה אמיתית בעוד דקה");
        realTestButton.setOnClickListener(v -> scheduleOneMinuteTest());
        statusCard.addView(realTestButton);

        LinearLayout remindersCard = card(Color.rgb(255, 255, 255), Color.rgb(219, 231, 234), 20);
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
                new int[]{Color.WHITE, Color.rgb(247, 253, 251), Color.rgb(255, 250, 238)},
                Color.rgb(221, 231, 235),
                20
        );
        soundCard.setGravity(Gravity.RIGHT);
        root.addView(soundCard);

        TextView soundTitle = text("צליל וקול", 22, INK, Typeface.BOLD);
        soundTitle.setGravity(Gravity.RIGHT);
        soundCard.addView(soundTitle);

        TextView soundText = text("בחר את האופי של הרגע: קריסטל, רקע רגוע, צלצולי אור, קול חם או מנטרה חיה.", 16, MUTED, Typeface.NORMAL);
        soundText.setGravity(Gravity.RIGHT);
        soundCard.addView(soundText);

        soundCard.addView(soundModeOptions());
        soundCard.addView(durationOptions());

        Button playButton = primaryButton("השמעת הצליל שבחרתי");
        playButton.setOnClickListener(v -> {
            if (previewPlayer == null) previewPlayer = new ReminderSoundPlayer();
            int playbackSeconds = ReminderScheduler.getPlaybackSeconds(this, ReminderScheduler.getMessage(this));
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
        row.setBackground(cardBackground(Color.rgb(251, 254, 253), Color.rgb(217, 231, 234), 18));
        row.setElevation(dp(1));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(12), 0, 0);
        row.setLayoutParams(rowParams);

        TextView label = text(slot.title, 19, INK, Typeface.BOLD);
        label.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(label);

        TextView statusChip = chip(ReminderScheduler.isEnabled(this, slot) ? "פעיל" : "כבוי",
                ReminderScheduler.isEnabled(this, slot) ? Color.rgb(232, 248, 246) : Color.rgb(255, 239, 246),
                ReminderScheduler.isEnabled(this, slot) ? TEAL : ROSE);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(34)
        );
        statusParams.setMargins(0, dp(8), 0, 0);
        row.addView(statusChip, statusParams);

        Button timeButton = new Button(this);
        timeButton.setText(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)));
        timeButton.setAllCaps(false);
        timeButton.setTextSize(30);
        timeButton.setTypeface(Typeface.DEFAULT_BOLD);
        timeButton.setTextColor(INK);
        GradientDrawable timeBackground = new GradientDrawable();
        timeBackground.setColor(Color.rgb(255, 250, 238));
        timeBackground.setCornerRadius(dp(18));
        timeBackground.setStroke(dp(1), Color.rgb(232, 220, 188));
        timeButton.setBackground(timeBackground);
        LinearLayout.LayoutParams timeButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(70)
        );
        timeButtonParams.setMargins(0, dp(14), 0, 0);
        row.addView(timeButton, timeButtonParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        controlsParams.setMargins(0, dp(8), 0, 0);
        row.addView(controls, controlsParams);

        Switch toggle = new Switch(this);
        toggle.setChecked(ReminderScheduler.isEnabled(this, slot));
        controls.addView(toggle);

        EditText messageEdit = new EditText(this);
        messageEdit.setText(ReminderScheduler.getMessage(this, slot));
        messageEdit.setTextSize(16);
        messageEdit.setTextColor(INK);
        messageEdit.setGravity(Gravity.RIGHT | Gravity.TOP);
        messageEdit.setTextDirection(View.TEXT_DIRECTION_RTL);
        messageEdit.setMinLines(2);
        messageEdit.setSingleLine(false);
        messageEdit.setPadding(dp(10), dp(8), dp(10), dp(8));
        messageEdit.setBackground(cardBackground(Color.WHITE, Color.rgb(225, 234, 237), 14));
        messageEdit.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                ReminderScheduler.saveMessage(this, slot, messageEdit.getText().toString());
            }
        });
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, dp(12), 0, 0);
        row.addView(messageEdit, messageParams);

        Button editButton = primaryButton("עריכה מהירה");
        editButton.setOnClickListener(v -> showReminderEditor(slot, timeButton, messageEdit, toggle));
        row.addView(editButton);

        Button deleteButton = secondaryButton("מחיקה");
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
        row.addView(deleteButton);

        CompoundButton.OnCheckedChangeListener saveToggle = (buttonView, isChecked) -> {
            int[] hm = parseTime(timeButton.getText().toString());
            ReminderScheduler.save(this, slot, isChecked, hm[0], hm[1]);
            ReminderScheduler.scheduleAll(this);
            statusChip.setText(isChecked ? "פעיל" : "כבוי");
        };
        toggle.setOnCheckedChangeListener(saveToggle);

        timeButton.setOnClickListener(v -> showReminderEditor(slot, timeButton, messageEdit, toggle));

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
        card.setElevation(dp(2));

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

    private Button lightButton(String label) {
        return styledButton(label, Color.rgb(237, 251, 247), Color.rgb(0, 105, 92), 16);
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
        button.setAllCaps(false);
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

    private void styleSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(226, 247, 243));
        window.setNavigationBarColor(Color.rgb(246, 241, 255));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void showReminderEditor(ReminderSlot slot, Button timeButton, EditText messageEdit, Switch toggle) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView editorTitle = text("עריכה מהירה", 18, INK, Typeface.BOLD);
        editorTitle.setGravity(Gravity.RIGHT);
        content.addView(editorTitle);

        EditText timeInput = new EditText(this);
        timeInput.setHint("שעה: 9, 930, 09:30");
        timeInput.setText(timeButton.getText().toString());
        timeInput.setTextSize(16);
        timeInput.setSingleLine(true);
        timeInput.setGravity(Gravity.CENTER);
        timeInput.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        timeInput.setTextDirection(View.TEXT_DIRECTION_LTR);
        timeInput.setBackground(cardBackground(Color.WHITE, Color.rgb(225, 234, 237), 14));
        LinearLayout.LayoutParams timeInputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        timeInputParams.setMargins(0, dp(12), 0, 0);
        content.addView(timeInput, timeInputParams);

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams quickRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        quickRowParams.setMargins(0, dp(10), 0, 0);
        content.addView(quickRow, quickRowParams);

        Button morning = compactButton("בוקר");
        morning.setOnClickListener(v -> timeInput.setText("09:00"));
        Button noon = compactButton("צהריים");
        noon.setOnClickListener(v -> timeInput.setText("14:00"));
        Button evening = compactButton("ערב");
        evening.setOnClickListener(v -> timeInput.setText("20:00"));
        quickRow.addView(morning, new LinearLayout.LayoutParams(0, dp(44), 1));
        quickRow.addView(noon, new LinearLayout.LayoutParams(0, dp(44), 1));
        quickRow.addView(evening, new LinearLayout.LayoutParams(0, dp(44), 1));

        EditText messageInput = new EditText(this);
        messageInput.setText(messageEdit.getText().toString());
        messageInput.setHint("הודעה אישית");
        messageInput.setTextSize(16);
        messageInput.setTextColor(INK);
        messageInput.setGravity(Gravity.RIGHT | Gravity.TOP);
        messageInput.setTextDirection(View.TEXT_DIRECTION_RTL);
        messageInput.setMinLines(3);
        messageInput.setSingleLine(false);
        messageInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        messageInput.setBackground(cardBackground(Color.WHITE, Color.rgb(225, 234, 237), 14));
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, dp(12), 0, 0);
        content.addView(messageInput, messageParams);

        CheckBox enabledCheck = new CheckBox(this);
        enabledCheck.setText("פעילה");
        enabledCheck.setChecked(toggle.isChecked());
        enabledCheck.setTextDirection(View.TEXT_DIRECTION_RTL);
        content.addView(enabledCheck);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("שמירה", null)
                .setNegativeButton("ביטול", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int[] hm = parseTypedTime(timeInput.getText().toString());
            if (hm == null) {
                Toast.makeText(this, "הקלד שעה כמו 9, 930 או 09:30", Toast.LENGTH_LONG).show();
                return;
            }
            String formatted = timeText(hm[0], hm[1]);
            timeButton.setText(formatted);
            messageEdit.setText(messageInput.getText().toString());
            ReminderScheduler.save(this, slot, enabledCheck.isChecked(), hm[0], hm[1]);
            ReminderScheduler.saveMessage(this, slot, messageInput.getText().toString());
            ReminderScheduler.scheduleAll(this);
            refreshContent();
            Toast.makeText(this, "נשמר", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
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

    private void scheduleOneMinuteTest() {
        List<ReminderSlot> slots = ReminderScheduler.getSlots(this);
        if (slots.isEmpty()) {
            Toast.makeText(this, "אין תזכורות לבדיקה", Toast.LENGTH_SHORT).show();
            return;
        }
        ReminderScheduler.scheduleInMinutes(this, slots.get(0).id, 1);
        Toast.makeText(this, "נקבעה בדיקת התראה בעוד דקה", Toast.LENGTH_LONG).show();
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

    private int[] parseTypedTime(String rawValue) {
        if (rawValue == null) return null;
        String value = rawValue.trim()
                .replace(".", ":")
                .replace(" ", "");
        if (value.isEmpty()) return null;

        int hour;
        int minute;
        try {
            if (value.contains(":")) {
                String[] parts = value.split(":");
                if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) return null;
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } else {
                if (!value.matches("\\d{1,4}")) return null;
                int number = Integer.parseInt(value);
                if (value.length() <= 2) {
                    hour = number;
                    minute = 0;
                } else {
                    hour = number / 100;
                    minute = number % 100;
                }
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
        return new int[]{hour, minute};
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class AmbientHeartView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path heart = new Path();
        private final RectF oval = new RectF();

        AmbientHeartView(android.content.Context context) {
            super(context);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            postInvalidateOnAnimation();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h * 0.52f;
            float pulse = 1f + 0.035f * (float) Math.sin(System.currentTimeMillis() / 620.0);

            paint.setStyle(Paint.Style.FILL);
            drawCircle(canvas, cx, cy, h * 0.52f * pulse, Color.argb(44, 0, 137, 123));
            drawCircle(canvas, cx - w * 0.24f, cy - h * 0.12f, h * 0.23f, Color.argb(34, 216, 27, 96));
            drawCircle(canvas, cx + w * 0.26f, cy + h * 0.08f, h * 0.20f, Color.argb(38, 191, 138, 0));

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpLocal(1.5f));
            paint.setColor(Color.argb(90, 255, 255, 255));
            for (int i = 0; i < 3; i++) {
                float r = h * (0.27f + i * 0.095f) * pulse;
                oval.set(cx - r, cy - r, cx + r, cy + r);
                canvas.drawOval(oval, paint);
            }

            canvas.save();
            canvas.translate(cx, cy - h * 0.02f);
            canvas.scale(pulse, pulse);
            buildHeartPath(h * 0.34f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(216, 27, 96));
            canvas.drawPath(heart, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpLocal(2));
            paint.setColor(Color.argb(190, 255, 255, 255));
            canvas.drawPath(heart, paint);
            canvas.restore();

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(dpLocal(15));
            paint.setColor(Color.rgb(0, 105, 92));
            canvas.drawText("לנשום. לברך. להיזכר בטוב.", cx, h - dpLocal(10), paint);

            postInvalidateOnAnimation();
        }

        private void drawCircle(Canvas canvas, float cx, float cy, float radius, int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(cx, cy, radius, paint);
        }

        private void buildHeartPath(float size) {
            heart.reset();
            heart.moveTo(0, size * 0.42f);
            heart.cubicTo(-size * 1.18f, -size * 0.18f, -size * 0.68f, -size * 1.08f, 0, -size * 0.55f);
            heart.cubicTo(size * 0.68f, -size * 1.08f, size * 1.18f, -size * 0.18f, 0, size * 0.42f);
            heart.close();
        }

        private float dpLocal(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}
