package com.dor.happyprayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Main screen organized around an everyday pause, rather than a page of settings. */
public final class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 42;
    private static final int INK = Color.rgb(20, 35, 52);
    private static final int MUTED = Color.rgb(79, 96, 111);
    private static final int TEAL = Color.rgb(0, 118, 108);
    private static final int DEEP_TEAL = Color.rgb(0, 82, 78);
    private static final int GOLD = Color.rgb(208, 151, 43);
    private static final int SKY = Color.rgb(235, 247, 250);
    private static final String[] MUSIC_DESCRIPTIONS = {
            "צלילים צלולים ונקיים לפתיחה עדינה.",
            "הרמוניה חמה ונושמת שמאטה את הקצב.",
            "פעמונים בהירים ותנועה שקטה של אור.",
            "קול עברי רגוע עם ליווי רך.",
            "הנחיה איטית, נשימה ומילים טובות.",
            "רק המילים והמסך — ללא צליל כלל.",
            "קטע פסנתר אמיתי, רגוע ומלא — ללא שכבת צליל מלאכותית.",
            "פסנתר אמיתי ואווירה חולמנית, רכה ואיטית."
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ReminderSoundPlayer previewPlayer;
    private ScrollView contentScrollView;
    private int section = 0;
    private boolean contentReady;
    private boolean previewPlaying;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        styleSystemBars();
        setContentView(buildContent());
        contentReady = true;
        requestNotificationPermissionIfNeeded();
        ReminderScheduler.scheduleAll(this);
        handler.postDelayed(this::showWelcomeIfNeeded, 450);
    }

    @Override protected void onResume() {
        super.onResume();
        if (contentReady) refreshContent(false);
    }

    @Override protected void onDestroy() {
        contentReady = false;
        stopPreview();
        super.onDestroy();
    }

    private void refreshContent(boolean keepScroll) {
        int position = keepScroll && contentScrollView != null ? contentScrollView.getScrollY() : 0;
        setContentView(buildContent());
        if (contentScrollView != null) contentScrollView.post(() -> contentScrollView.scrollTo(0, position));
    }

    private View buildContent() {
        FrameLayout scene = new FrameLayout(this);
        scene.addView(new SunriseBackgroundView(this), fullFrame());

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        scene.addView(shell, fullFrame());

        ScrollView scroll = new ScrollView(this);
        contentScrollView = scroll;
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        shell.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(24));
        scroll.addView(page);

        addTopBar(page);
        if (section == 0) buildHome(page);
        else if (section == 1) buildReminders(page);
        else if (section == 2) buildMusic(page);
        else buildSettings(page);

        if (previewPlaying) addMiniPlayer(page);
        shell.addView(buildNavigation(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(74)));
        return scene;
    }

    private void addTopBar(LinearLayout page) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(row, matchWrap());

        TextView title = text("שכולם מאושרים", 24, INK, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(46), 1));

        TextView mark = text("✦", 28, GOLD, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        row.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(46)));
    }

    private void buildHome(LinearLayout page) {
        TextView eyebrow = text(greeting(), 15, DEEP_TEAL, Typeface.BOLD);
        eyebrow.setGravity(Gravity.RIGHT);
        page.addView(eyebrow, matchWrap());

        LinearLayout quote = card(Color.argb(238, 255, 255, 255), Color.argb(90, 255, 255, 255));
        quote.setPadding(dp(22), dp(24), dp(22), dp(22));
        page.addView(quote);
        TextView label = text("המשפט של היום", 14, TEAL, Typeface.BOLD);
        label.setGravity(Gravity.RIGHT);
        quote.addView(label);
        TextView opening = text(DailyOpening.today(), 26, INK, Typeface.BOLD);
        opening.setGravity(Gravity.RIGHT);
        opening.setLineSpacing(dp(5), 1f);
        LinearLayout.LayoutParams openingParams = matchWrap();
        openingParams.setMargins(0, dp(10), 0, 0);
        quote.addView(opening, openingParams);
        TextView hint = text("מחר יחכה כאן משפט חדש.", 15, MUTED, Typeface.NORMAL);
        hint.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams hintParams = matchWrap();
        hintParams.setMargins(0, dp(14), 0, 0);
        quote.addView(hint, hintParams);

        LinearLayout next = darkCard();
        page.addView(next);
        TextView nextLabel = text("התזכורת הקרובה", 14, Color.rgb(174, 232, 221), Typeface.BOLD);
        nextLabel.setGravity(Gravity.RIGHT);
        next.addView(nextLabel);
        TextView nextTitle = text(nextReminderTitle(), 23, Color.WHITE, Typeface.BOLD);
        nextTitle.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams nextTitleParams = matchWrap();
        nextTitleParams.setMargins(0, dp(6), 0, 0);
        next.addView(nextTitle, nextTitleParams);
        TextView nextDetail = text(nextReminderDetail(), 16, Color.rgb(221, 246, 242), Typeface.NORMAL);
        nextDetail.setGravity(Gravity.RIGHT);
        next.addView(nextDetail);
        Button pause = primaryButton("רגע של שקט");
        pause.setOnClickListener(v -> { section = 2; refreshContent(false); });
        next.addView(pause);

        LinearLayout practice = card(Color.argb(232, 255, 253, 248), Color.argb(55, 208, 151, 43));
        page.addView(practice);
        TextView practiceTitle = text("כוונה קטנה להיום", 19, INK, Typeface.BOLD);
        practiceTitle.setGravity(Gravity.RIGHT);
        practice.addView(practiceTitle);
        TextView practiceText = text("בחרו אדם אחד, קרוב או רחוק, ושלחו לו בלב איחול טוב.", 16, MUTED, Typeface.NORMAL);
        practiceText.setGravity(Gravity.RIGHT);
        practiceText.setLineSpacing(dp(3), 1f);
        practice.addView(practiceText);
    }

    private void buildReminders(LinearLayout page) {
        addPageHeading(page, "תזכורות", "הזמנים האישיים שלכם לאורך היום.");
        for (ReminderSlot slot : ReminderScheduler.getSlots(this)) page.addView(reminderRow(slot));
        Button add = primaryButton("+ הוספת תזכורת");
        add.setOnClickListener(v -> {
            ReminderSlot slot = ReminderScheduler.addSlot(this);
            ReminderScheduler.schedule(this, slot);
            refreshContent(false);
        });
        page.addView(add);
    }

    private View reminderRow(ReminderSlot slot) {
        LinearLayout row = card(Color.argb(244, 255, 255, 255), Color.argb(65, 10, 85, 82));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(heading, matchWrap());

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.RIGHT);
        heading.addView(labels, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView title = text(slot.title, 19, INK, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT);
        labels.addView(title);
        TextView message = text(shortMessage(ReminderScheduler.getMessage(this, slot)), 14, MUTED, Typeface.NORMAL);
        message.setGravity(Gravity.RIGHT);
        message.setMaxLines(1);
        message.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(message);
        TextView days = text(ReminderScheduler.daysSummary(this, slot), 13, TEAL, Typeface.BOLD);
        days.setGravity(Gravity.RIGHT);
        labels.addView(days);

        TextView time = text(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)), 22, TEAL, Typeface.BOLD);
        time.setGravity(Gravity.CENTER);
        heading.addView(time, new LinearLayout.LayoutParams(dp(78), dp(52)));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams controlsParams = matchWrap();
        controlsParams.setMargins(0, dp(10), 0, 0);
        row.addView(controls, controlsParams);
        Switch toggle = new Switch(this);
        toggle.setText(ReminderScheduler.isEnabled(this, slot) ? "פעילה" : "כבויה");
        toggle.setTextSize(14);
        toggle.setTextColor(INK);
        toggle.setTextDirection(View.TEXT_DIRECTION_RTL);
        toggle.setChecked(ReminderScheduler.isEnabled(this, slot));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            ReminderScheduler.save(this, slot, checked,
                    ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot));
            ReminderScheduler.scheduleAll(this);
            button.setText(checked ? "פעילה" : "כבויה");
        });
        controls.addView(toggle, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button edit = outlineButton("עריכה");
        edit.setOnClickListener(v -> showReminderEditor(slot));
        controls.addView(edit, new LinearLayout.LayoutParams(dp(110), dp(48)));
        Button copy = outlineButton("שכפול");
        copy.setOnClickListener(v -> duplicateReminder(slot));
        controls.addView(copy, new LinearLayout.LayoutParams(dp(88), dp(48)));
        return row;
    }

    private void buildMusic(LinearLayout page) {
        addPageHeading(page, "מוזיקה לרגע הזה", "בחרו עולם קולי. כל בחירה מנוגנת בצורה אחרת.");
        int selected = ReminderScheduler.getSoundMode(this);
        for (int i = 0; i < ReminderScheduler.SOUND_LABELS.length; i++) {
            final int mode = i;
            LinearLayout track = card(i == selected ? Color.rgb(229, 247, 243) : Color.argb(240, 255, 255, 255),
                    i == selected ? Color.rgb(94, 173, 160) : Color.argb(60, 10, 85, 82));
            TextView name = text(ReminderScheduler.SOUND_LABELS[i], 19, INK, Typeface.BOLD);
            name.setGravity(Gravity.RIGHT);
            track.addView(name);
            TextView description = text(MUSIC_DESCRIPTIONS[i], 15, MUTED, Typeface.NORMAL);
            description.setGravity(Gravity.RIGHT);
            track.addView(description);
            Button choose = i == selected ? outlineButton("נבחרה") : outlineButton("בחירה");
            choose.setOnClickListener(v -> {
                ReminderScheduler.saveSoundMode(this, mode);
                refreshContent(true);
            });
            track.addView(choose);
            page.addView(track);
        }

        LinearLayout duration = card(Color.argb(238, 255, 255, 255), Color.argb(55, 10, 85, 82));
        page.addView(duration);
        TextView durationTitle = text("כמה לשמוע בכל פעם?", 18, INK, Typeface.BOLD);
        durationTitle.setGravity(Gravity.RIGHT);
        duration.addView(durationTitle);
        LinearLayout options = new LinearLayout(this);
        options.setGravity(Gravity.CENTER);
        int[] values = {5, 10, 20, 30, 60};
        for (int seconds : values) {
            Button option = compactButton(seconds + " ש׳", seconds == ReminderScheduler.getSoundSeconds(this));
            option.setOnClickListener(v -> {
                ReminderScheduler.saveSoundSeconds(this, seconds);
                refreshContent(true);
            });
            options.addView(option, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        duration.addView(options, matchWrap());
        TextView durationHint = text("המנגינה נעצרת אוטומטית אחרי הזמן שבחרתם.", 14, MUTED, Typeface.NORMAL);
        durationHint.setGravity(Gravity.RIGHT);
        duration.addView(durationHint);

        Button play = primaryButton(previewPlaying ? "מנגנים עכשיו" : "האזנה למנגינה");
        play.setEnabled(!previewPlaying);
        play.setOnClickListener(v -> startPreview());
        page.addView(play);
    }

    private void buildSettings(LinearLayout page) {
        addPageHeading(page, "הגדרות", "רק מה שנחוץ כדי שהתזכורות יעבדו היטב.");
        addHealthCard(page);
        LinearLayout card = card(Color.argb(240, 255, 255, 255), Color.argb(55, 10, 85, 82));
        page.addView(card);
        CheckBox popup = new CheckBox(this);
        popup.setText("מסך תזכורת מלא ועדין בזמן תזכורת");
        popup.setTextSize(16);
        popup.setTextColor(INK);
        popup.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        popup.setTextDirection(View.TEXT_DIRECTION_RTL);
        popup.setChecked(ReminderScheduler.isPopupEnabled(this));
        popup.setOnCheckedChangeListener((v, checked) -> ReminderScheduler.savePopupEnabled(this, checked));
        card.addView(popup);
        CheckBox airplane = new CheckBox(this);
        airplane.setText("השהיית תזכורות במצב טיסה");
        airplane.setTextSize(16);
        airplane.setTextColor(INK);
        airplane.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        airplane.setTextDirection(View.TEXT_DIRECTION_RTL);
        airplane.setChecked(ReminderScheduler.isAirplanePauseEnabled(this));
        airplane.setOnCheckedChangeListener((v, checked) -> ReminderScheduler.saveAirplanePauseEnabled(this, checked));
        card.addView(airplane);
        Button notifications = outlineButton("אישור התראות בטלפון");
        notifications.setOnClickListener(v -> requestNotificationPermissionIfNeeded());
        card.addView(notifications);
        Button exact = outlineButton("אישור דיוק תזכורות");
        exact.setOnClickListener(v -> openExactAlarmSettingsIfNeeded());
        card.addView(exact);
        Button test = outlineButton("בדיקת תזכורת עכשיו");
        test.setOnClickListener(v -> openTestReminder());
        card.addView(test);
        Button minute = outlineButton("בדיקה אמיתית בעוד דקה");
        minute.setOnClickListener(v -> scheduleOneMinuteTest());
        card.addView(minute);

        Button feedback = outlineButton("שליחת משוב קצר");
        feedback.setOnClickListener(v -> showFeedbackDialog());
        page.addView(feedback);
    }

    private void addMiniPlayer(LinearLayout page) {
        LinearLayout player = darkCard();
        LinearLayout.LayoutParams params = player.getLayoutParams() instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) player.getLayoutParams() : matchWrap();
        params.setMargins(0, dp(2), 0, dp(16));
        player.setLayoutParams(params);
        TextView label = text("מתנגן עכשיו", 13, Color.rgb(174, 232, 221), Typeface.BOLD);
        label.setGravity(Gravity.RIGHT);
        player.addView(label);
        TextView name = text(ReminderScheduler.SOUND_LABELS[ReminderScheduler.getSoundMode(this)], 18, Color.WHITE, Typeface.BOLD);
        name.setGravity(Gravity.RIGHT);
        player.addView(name);
        Button stop = outlineButton("עצור");
        stop.setTextColor(Color.WHITE);
        stop.setOnClickListener(v -> stopPreview());
        player.addView(stop);
        page.addView(player);
    }

    private View buildNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(12), dp(8), dp(12), dp(10));
        nav.setBackgroundColor(Color.argb(248, 255, 255, 255));
        String[] labels = {"בית", "תזכורות", "מוזיקה", "הגדרות"};
        for (int i = 0; i < labels.length; i++) {
            final int target = i;
            Button item = navButton(labels[i], i == section);
            item.setOnClickListener(v -> { section = target; refreshContent(false); });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(54), 1));
        }
        return nav;
    }

    private void addPageHeading(LinearLayout page, String title, String subtitle) {
        TextView heading = text(title, 28, INK, Typeface.BOLD);
        heading.setGravity(Gravity.RIGHT);
        page.addView(heading);
        TextView detail = text(subtitle, 16, MUTED, Typeface.NORMAL);
        detail.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(4), 0, dp(14));
        page.addView(detail, params);
    }

    private void startPreview() {
        if (previewPlayer == null) previewPlayer = new ReminderSoundPlayer();
        int seconds = ReminderScheduler.getPlaybackSeconds(this, ReminderScheduler.getMessage(this));
        previewPlayer.play(this, ReminderScheduler.getSoundMode(this), ReminderScheduler.getMessage(this), seconds);
        previewPlaying = true;
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::stopPreview, seconds * 1000L);
        refreshContent(true);
    }

    private void stopPreview() {
        handler.removeCallbacksAndMessages(null);
        if (previewPlayer != null) previewPlayer.stop();
        previewPlaying = false;
        if (contentReady) refreshContent(true);
    }

    private void showReminderEditor(ReminderSlot slot) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(6), dp(8), dp(4));
        EditText title = editorInput(slot.title, "שם התזכורת");
        content.addView(title);
        EditText time = editorInput(timeText(ReminderScheduler.getHour(this, slot), ReminderScheduler.getMinute(this, slot)), "שעה: 09:00");
        time.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        time.setTextDirection(View.TEXT_DIRECTION_LTR);
        content.addView(time);
        EditText message = editorInput(ReminderScheduler.getMessage(this, slot), "משפט אישי");
        message.setMinLines(3);
        message.setSingleLine(false);
        message.setGravity(Gravity.RIGHT | Gravity.TOP);
        content.addView(message);
        final boolean[] selectedDays = ReminderScheduler.getDays(this, slot);
        Button days = outlineButton("ימים: " + ReminderScheduler.daysSummary(this, slot));
        days.setOnClickListener(v -> showDaysPicker(selectedDays, days));
        content.addView(days);
        new AlertDialog.Builder(this)
                .setTitle("עריכת תזכורת")
                .setView(content)
                .setNegativeButton("ביטול", null)
                .setNeutralButton("מחיקה", (dialog, which) -> confirmDelete(slot))
                .setPositiveButton("שמירה", (dialog, which) -> {
                    int[] parsed = parseTypedTime(time.getText().toString());
                    if (parsed == null) {
                        Toast.makeText(this, "הקלד שעה כמו 9, 930 או 09:30", Toast.LENGTH_LONG).show();
                        return;
                    }
                    saveTitle(slot, title.getText().toString());
                    ReminderScheduler.save(this, slot, ReminderScheduler.isEnabled(this, slot), parsed[0], parsed[1]);
                    ReminderScheduler.saveMessage(this, slot, message.getText().toString());
                    ReminderScheduler.saveDays(this, slot, selectedDays);
                    ReminderScheduler.scheduleAll(this);
                    refreshContent(false);
                }).show();
    }

    private void showDaysPicker(boolean[] selectedDays, Button label) {
        String[] days = {"יום א׳", "יום ב׳", "יום ג׳", "יום ד׳", "יום ה׳", "יום ו׳", "שבת"};
        new AlertDialog.Builder(this)
                .setTitle("באילו ימים תופיע התזכורת?")
                .setMultiChoiceItems(days, selectedDays, (dialog, which, checked) -> selectedDays[which] = checked)
                .setNegativeButton("ביטול", null)
                .setPositiveButton("אישור", (dialog, which) -> {
                    boolean oneSelected = false;
                    for (boolean selected : selectedDays) oneSelected |= selected;
                    if (!oneSelected) {
                        for (int i = 0; i < selectedDays.length; i++) selectedDays[i] = true;
                        Toast.makeText(this, "נבחרו כל הימים כדי שלא תישאר תזכורת ריקה", Toast.LENGTH_SHORT).show();
                    }
                    label.setText("ימים: " + daysSummary(selectedDays));
                }).show();
    }

    private String daysSummary(boolean[] days) {
        int count = 0;
        for (boolean day : days) if (day) count++;
        if (count == 7) return "כל יום";
        String[] labels = {"א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"};
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            if (!days[i]) continue;
            if (result.length() > 0) result.append(" · ");
            result.append(labels[i]);
        }
        return result.toString();
    }

    private void confirmDelete(ReminderSlot slot) {
        new AlertDialog.Builder(this)
                .setTitle("למחוק את " + slot.title + "?")
                .setMessage("אי אפשר לשחזר תזכורת שנמחקה.")
                .setNegativeButton("ביטול", null)
                .setPositiveButton("מחיקה", (dialog, which) -> {
                    ReminderScheduler.deleteSlot(this, slot);
                    refreshContent(false);
                }).show();
    }

    private void duplicateReminder(ReminderSlot source) {
        ReminderSlot copy = ReminderScheduler.addSlot(this);
        saveTitle(copy, source.title + " (עותק)");
        ReminderScheduler.save(this, copy, ReminderScheduler.isEnabled(this, source),
                ReminderScheduler.getHour(this, source), ReminderScheduler.getMinute(this, source));
        ReminderScheduler.saveMessage(this, copy, ReminderScheduler.getMessage(this, source));
        ReminderScheduler.saveDays(this, copy, ReminderScheduler.getDays(this, source));
        ReminderScheduler.scheduleAll(this);
        Toast.makeText(this, "נוצרה תזכורת חדשה לעריכה", Toast.LENGTH_SHORT).show();
        refreshContent(false);
    }

    private void saveTitle(ReminderSlot slot, String title) {
        String cleaned = title == null ? "" : title.trim();
        getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE).edit()
                .putString("slot_" + slot.id + "_title", cleaned.isEmpty() ? slot.title : cleaned).apply();
    }

    private EditText editorInput(String value, String hint) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint(hint);
        input.setTextSize(16);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        input.setPadding(dp(12), dp(6), dp(12), dp(6));
        input.setBackground(round(Color.WHITE, Color.rgb(210, 224, 226), 10));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, 0);
        input.setLayoutParams(params);
        return input;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void showWelcomeIfNeeded() {
        if (isFinishing() || getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE).getBoolean("welcome_seen", false)) return;
        new AlertDialog.Builder(this)
                .setTitle("ברוכים הבאים ✦")
                .setMessage("כדי שהתזכורות יעבדו היטב:\n\n1. אשרו התראות בטלפון.\n2. אשרו דיוק תזכורות אם הטלפון מבקש.\n3. עברו ללשונית תזכורות ובחרו את הזמנים והימים שמתאימים לכם.")
                .setNegativeButton("אחר כך", (dialog, which) -> markWelcomeSeen())
                .setPositiveButton("הגדרה מהירה", (dialog, which) -> {
                    markWelcomeSeen();
                    section = 1;
                    refreshContent(false);
                }).show();
    }

    private void markWelcomeSeen() {
        getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE).edit().putBoolean("welcome_seen", true).apply();
    }

    private void addHealthCard(LinearLayout page) {
        LinearLayout health = card(Color.argb(238, 232, 248, 246), Color.argb(80, 0, 118, 108));
        TextView title = text("בדיקת תקינות", 18, INK, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT);
        health.addView(title);
        TextView detail = text(reminderHealthSummary(), 15, MUTED, Typeface.NORMAL);
        detail.setGravity(Gravity.RIGHT);
        detail.setLineSpacing(dp(3), 1f);
        health.addView(detail);
        Button check = outlineButton("בדיקה מפורטת");
        check.setOnClickListener(v -> showHealthDialog());
        health.addView(check);
        page.addView(health);
    }

    private String reminderHealthSummary() {
        int active = 0;
        for (ReminderSlot slot : ReminderScheduler.getSlots(this)) if (ReminderScheduler.isEnabled(this, slot)) active++;
        String notifications = notificationsGranted() ? "התראות מאושרות" : "נדרש אישור התראות";
        String exact = exactAlarmGranted() ? "תזכורות מדויקות פעילות" : "ייתכן עיכוב קל בתזכורות";
        return active + " תזכורות פעילות · " + notifications + "\n" + exact;
    }

    private void showHealthDialog() {
        String notification = notificationsGranted() ? "✓ התראות: מאושרות" : "! התראות: יש לאשר";
        String exact = exactAlarmGranted() ? "✓ דיוק תזכורות: מאושר" : "! דיוק תזכורות: יש לאשר כדי לקבל התראות בזמן";
        new AlertDialog.Builder(this)
                .setTitle("מצב התזכורות")
                .setMessage(notification + "\n" + exact + "\n\n" + nextReminderTitle() + " — " + nextReminderDetail())
                .setNegativeButton("סגור", null)
                .setNeutralButton("אישור התראות", (dialog, which) -> requestNotificationPermissionIfNeeded())
                .setPositiveButton("אישור דיוק", (dialog, which) -> openExactAlarmSettingsIfNeeded())
                .show();
    }

    private boolean notificationsGranted() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean exactAlarmGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager manager = getSystemService(AlarmManager.class);
        return manager != null && manager.canScheduleExactAlarms();
    }

    private void showFeedbackDialog() {
        String[] choices = {"ממש עוזרת לי", "נעימה, אבל אפשר לשפר", "נתקלתי בבעיה"};
        new AlertDialog.Builder(this)
                .setTitle("איך האפליקציה מרגישה לך?")
                .setSingleChoiceItems(choices, -1, (dialog, which) -> {
                    getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE).edit().putInt("last_feedback", which).apply();
                    dialog.dismiss();
                    Toast.makeText(this, "תודה — המשוב נשמר במכשיר", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void openExactAlarmSettingsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager manager = getSystemService(AlarmManager.class);
            if (manager != null && !manager.canScheduleExactAlarms()) {
                Intent intent = ReminderScheduler.exactAlarmSettingsIntent();
                if (intent != null) startActivity(intent);
            }
        }
    }

    private void openTestReminder() {
        List<ReminderSlot> slots = ReminderScheduler.getSlots(this);
        Intent intent = new Intent(this, ReminderPopupActivity.class);
        intent.putExtra("slot_id", slots.isEmpty() ? 0 : slots.get(0).id);
        intent.putExtra("is_test", true);
        startActivity(intent);
    }

    private void scheduleOneMinuteTest() {
        List<ReminderSlot> slots = ReminderScheduler.getSlots(this);
        if (slots.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager manager = getSystemService(AlarmManager.class);
            if (manager != null && !manager.canScheduleExactAlarms()) {
                Toast.makeText(this, "יש לאשר דיוק תזכורות ואז לנסות שוב", Toast.LENGTH_LONG).show();
                openExactAlarmSettingsIfNeeded();
                return;
            }
        }
        ReminderScheduler.scheduleTestInMinutes(this, slots.get(0).id, 1);
        Toast.makeText(this, "נקבעה בדיקת התראה בעוד דקה", Toast.LENGTH_LONG).show();
    }

    private String nextReminderTitle() {
        ReminderSlot slot = nextSlot();
        return slot == null ? "אין תזכורות פעילות" : slot.title;
    }

    private String nextReminderDetail() {
        ReminderSlot slot = nextSlot();
        if (slot == null) return "עברו ללשונית תזכורות כדי להוסיף אחת חדשה.";
        long at = ReminderScheduler.nextTriggerMillis(this, slot);
        Calendar today = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(at);
        String prefix = today.get(Calendar.YEAR) == next.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == next.get(Calendar.DAY_OF_YEAR)
                ? "היום" : DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(at));
        return prefix + " בשעה " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(at));
    }

    private ReminderSlot nextSlot() {
        ReminderSlot result = null;
        long closest = Long.MAX_VALUE;
        for (ReminderSlot slot : ReminderScheduler.getSlots(this)) {
            if (!ReminderScheduler.isEnabled(this, slot)) continue;
            long at = ReminderScheduler.nextTriggerMillis(this, slot);
            if (at < closest) { closest = at; result = slot; }
        }
        return result;
    }

    private String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11) return "בוקר של אור";
        if (hour < 17) return "רגע טוב באמצע היום";
        if (hour < 21) return "ערב שקט";
        return "לילה רך";
    }

    private String shortMessage(String message) {
        if (message == null) return "";
        return message.replace('\n', ' ').trim();
    }

    private String timeText(int hour, int minute) { return String.format(Locale.US, "%02d:%02d", hour, minute); }

    private int[] parseTypedTime(String raw) {
        if (raw == null) return null;
        String value = raw.trim().replace('.', ':').replace(" ", "");
        try {
            int hour, minute;
            if (value.contains(":")) {
                String[] pieces = value.split(":");
                if (pieces.length != 2) return null;
                hour = Integer.parseInt(pieces[0]); minute = Integer.parseInt(pieces[1]);
            } else {
                if (!value.matches("\\d{1,4}")) return null;
                int number = Integer.parseInt(value);
                hour = value.length() <= 2 ? number : number / 100;
                minute = value.length() <= 2 ? 0 : number % 100;
            }
            return hour >= 0 && hour < 24 && minute >= 0 && minute < 60 ? new int[]{hour, minute} : null;
        } catch (NumberFormatException ignored) { return null; }
    }

    private LinearLayout card(int color, int stroke) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(round(color, stroke, 12));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, dp(8));
        card.setLayoutParams(params);
        return card;
    }

    private LinearLayout darkCard() {
        LinearLayout card = card(Color.argb(236, 0, 76, 75), Color.argb(120, 159, 226, 214));
        card.setBackground(gradient(new int[]{Color.rgb(0, 76, 75), Color.rgb(10, 106, 100)}));
        return card;
    }

    private Button primaryButton(String label) {
        Button button = baseButton(label, Color.WHITE, 17);
        button.setBackground(gradient(new int[]{Color.rgb(0, 125, 113), Color.rgb(0, 154, 136)}));
        return button;
    }

    private Button outlineButton(String label) {
        Button button = baseButton(label, TEAL, 15);
        button.setBackground(round(Color.TRANSPARENT, Color.rgb(128, 183, 174), 10));
        return button;
    }

    private Button compactButton(String label, boolean selected) {
        Button button = baseButton(label, selected ? Color.WHITE : TEAL, 14);
        button.setBackground(round(selected ? TEAL : Color.TRANSPARENT,
                selected ? TEAL : Color.rgb(128, 183, 174), 10));
        return button;
    }

    private Button navButton(String label, boolean selected) {
        Button button = baseButton(label, selected ? TEAL : MUTED, 13);
        button.setPadding(dp(2), 0, dp(2), 0);
        button.setBackground(round(selected ? Color.rgb(221, 244, 240) : Color.TRANSPARENT, Color.TRANSPARENT, 10));
        return button;
    }

    private Button baseButton(String label, int color, int size) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(color);
        button.setTextSize(size);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setTextDirection(View.TEXT_DIRECTION_RTL);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(48));
        button.setStateListAnimator(null);
        return button;
    }

    private TextView text(String value, int size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private GradientDrawable round(int color, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable gradient(int[] colors) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(12));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private FrameLayout.LayoutParams fullFrame() {
        return new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    private void styleSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(230, 245, 248));
        window.setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }

    private static final class SunriseBackgroundView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        SunriseBackgroundView(android.content.Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{Color.rgb(231, 246, 249), Color.rgb(249, 252, 246), Color.rgb(239, 248, 245)}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * .82f, h * .08f, w * .5f,
                    new int[]{Color.argb(105, 255, 206, 115), Color.argb(30, 255, 235, 191), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
            canvas.drawCircle(w * .82f, h * .08f, w * .5f, paint);
            paint.setShader(null);
            paint.setColor(Color.argb(36, 0, 118, 108));
            for (int i = 0; i < 5; i++) canvas.drawCircle(w * (.12f + i * .24f), h * (.88f + (i % 2) * .04f), w * .26f, paint);
        }
    }
}
