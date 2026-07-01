package com.dor.happyprayer;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class ReminderPopupActivity extends Activity {
    private static final int INK = Color.rgb(17, 24, 39);
    private static final int TEAL = Color.rgb(0, 137, 123);
    private static final int ROSE = Color.rgb(216, 27, 96);
    private ReminderSoundPlayer soundPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(buildContent());
        playSound();
    }

    @Override
    protected void onDestroy() {
        stopSound();
        super.onDestroy();
    }

    private View buildContent() {
        FrameLayout screen = new FrameLayout(this);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(5, 42, 48), Color.rgb(0, 105, 92), Color.rgb(255, 244, 218), Color.rgb(247, 238, 255)}
        );
        screen.setBackground(background);
        screen.setPadding(dp(16), dp(22), dp(16), dp(22));

        HeartHaloView halo = new HeartHaloView(this);
        screen.addView(halo, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(22), dp(26), dp(22), dp(24));
        GradientDrawable card = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.WHITE, Color.rgb(246, 253, 251), Color.rgb(255, 250, 239)}
        );
        card.setCornerRadius(dp(28));
        card.setStroke(dp(2), Color.rgb(182, 224, 216));
        root.setBackground(card);
        root.setElevation(dp(8));

        TextView smallTitle = new TextView(this);
        smallTitle.setText(getIntent().getBooleanExtra("is_test", false) ? "בדיקת תזכורת" : "תזכורת טובה");
        smallTitle.setTextSize(20);
        smallTitle.setTextColor(TEAL);
        smallTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        smallTitle.setGravity(Gravity.CENTER);
        root.addView(smallTitle);

        TextView sparkle = new TextView(this);
        sparkle.setText("✦");
        sparkle.setTextSize(32);
        sparkle.setTextColor(Color.rgb(191, 138, 0));
        sparkle.setGravity(Gravity.CENTER);
        root.addView(sparkle);

        TextView heart = new TextView(this);
        heart.setText("♥");
        heart.setTextSize(74);
        heart.setTextColor(ROSE);
        heart.setGravity(Gravity.CENTER);
        root.addView(heart);

        TextView message = new TextView(this);
        int slotId = getIntent().getIntExtra("slot_id", 0);
        String reminderMessage = ReminderScheduler.getMessage(this, slotId);
        message.setText(reminderMessage);
        message.setTextSize(33);
        message.setTextColor(INK);
        message.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        message.setGravity(Gravity.CENTER);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setLineSpacing(dp(7), 1.0f);
        root.addView(message, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView footer = new TextView(this);
        footer.setText("רגע קטן של אור לפני שממשיכים.");
        footer.setTextSize(18);
        footer.setTextColor(Color.rgb(91, 104, 114));
        footer.setGravity(Gravity.CENTER);
        footer.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        footerParams.setMargins(0, dp(16), 0, 0);
        root.addView(footer, footerParams);

        Button closeButton = new Button(this);
        closeButton.setText("סגור ועצור צליל");
        closeButton.setAllCaps(false);
        closeButton.setTextSize(20);
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable closeBackground = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(0, 105, 92), TEAL}
        );
        closeBackground.setCornerRadius(dp(16));
        closeButton.setBackground(closeBackground);
        closeButton.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        );
        buttonParams.setMargins(0, dp(28), 0, 0);
        root.addView(closeButton, buttonParams);

        Button snoozeButton = new Button(this);
        snoozeButton.setText("עוד 10 דקות");
        snoozeButton.setAllCaps(false);
        snoozeButton.setTextSize(18);
        snoozeButton.setTextColor(TEAL);
        snoozeButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable snoozeBackground = new GradientDrawable();
        snoozeBackground.setColor(Color.rgb(232, 248, 246));
        snoozeBackground.setStroke(dp(1), Color.rgb(190, 225, 219));
        snoozeBackground.setCornerRadius(dp(16));
        snoozeButton.setBackground(snoozeBackground);
        snoozeButton.setOnClickListener(v -> snooze());
        LinearLayout.LayoutParams snoozeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        snoozeParams.setMargins(0, dp(10), 0, 0);
        root.addView(snoozeButton, snoozeParams);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        screen.addView(root, cardParams);
        return screen;
    }

    private void playSound() {
        int slotId = getIntent().getIntExtra("slot_id", 0);
        soundPlayer = new ReminderSoundPlayer();
        int seconds = ReminderScheduler.getPlaybackSeconds(this);
        soundPlayer.play(
                this,
                ReminderScheduler.getSoundMode(this),
                ReminderScheduler.getMessage(this, slotId),
                seconds
        );
        getWindow().getDecorView().postDelayed(this::stopSound, seconds * 1000L);
    }

    private void stopSound() {
        if (soundPlayer == null) return;
        soundPlayer.stop();
        soundPlayer = null;
    }

    private void snooze() {
        int slotId = getIntent().getIntExtra("slot_id", 0);
        if (slotId > 0 && !getIntent().getBooleanExtra("is_test", false)) {
            ReminderScheduler.scheduleInMinutes(this, slotId, 10);
            Toast.makeText(this, "התזכורת תחזור בעוד 10 דקות", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "זו בדיקה, לא נקבע נודניק", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class HeartHaloView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF oval = new RectF();

        HeartHaloView(android.content.Context context) {
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
            float cy = h * 0.34f;
            float pulse = 1f + 0.045f * (float) Math.sin(System.currentTimeMillis() / 700.0);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(42, 255, 255, 255));
            canvas.drawCircle(cx, cy, h * 0.30f * pulse, paint);
            paint.setColor(Color.argb(36, 255, 213, 79));
            canvas.drawCircle(cx - w * 0.28f, cy + h * 0.08f, h * 0.16f, paint);
            paint.setColor(Color.argb(40, 216, 27, 96));
            canvas.drawCircle(cx + w * 0.27f, cy - h * 0.05f, h * 0.14f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpLocal(1.2f));
            paint.setColor(Color.argb(64, 255, 255, 255));
            for (int i = 0; i < 4; i++) {
                float r = h * (0.16f + i * 0.055f) * pulse;
                oval.set(cx - r, cy - r, cx + r, cy + r);
                canvas.drawOval(oval, paint);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(34, 255, 255, 255));
            for (int i = 0; i < 9; i++) {
                float angle = (float) (i * Math.PI * 2 / 9 + System.currentTimeMillis() / 4200.0);
                float r = h * 0.27f;
                canvas.drawCircle(cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r, dpLocal(3.2f), paint);
            }

            postInvalidateOnAnimation();
        }

        private float dpLocal(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}
