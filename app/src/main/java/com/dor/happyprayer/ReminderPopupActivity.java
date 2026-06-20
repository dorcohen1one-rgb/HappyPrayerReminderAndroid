package com.dor.happyprayer;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
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

public final class ReminderPopupActivity extends Activity {
    private MediaPlayer player;

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
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(0, 137, 123), Color.rgb(245, 247, 248)}
        );
        screen.setBackground(background);
        screen.setPadding(dp(18), dp(24), dp(18), dp(24));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(28), dp(24), dp(28));
        GradientDrawable card = new GradientDrawable();
        card.setColor(Color.WHITE);
        card.setCornerRadius(dp(24));
        card.setStroke(dp(1), Color.rgb(220, 232, 232));
        root.setBackground(card);

        TextView smallTitle = new TextView(this);
        smallTitle.setText("תזכורת טובה");
        smallTitle.setTextSize(20);
        smallTitle.setTextColor(Color.rgb(0, 105, 92));
        smallTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        smallTitle.setGravity(Gravity.CENTER);
        root.addView(smallTitle);

        TextView heart = new TextView(this);
        heart.setText("♥");
        heart.setTextSize(72);
        heart.setTextColor(Color.rgb(216, 27, 96));
        heart.setGravity(Gravity.CENTER);
        root.addView(heart);

        TextView message = new TextView(this);
        int slotId = getIntent().getIntExtra("slot_id", 0);
        message.setText(ReminderScheduler.getMessage(this, slotId));
        message.setTextSize(32);
        message.setTextColor(Color.rgb(24, 28, 32));
        message.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        message.setGravity(Gravity.CENTER);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setLineSpacing(dp(6), 1.0f);
        root.addView(message, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView footer = new TextView(this);
        footer.setText("מכל העולם, מכל היצורים");
        footer.setTextSize(18);
        footer.setTextColor(Color.rgb(84, 96, 102));
        footer.setGravity(Gravity.CENTER);
        footer.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        footerParams.setMargins(0, dp(16), 0, 0);
        root.addView(footer, footerParams);

        Button closeButton = new Button(this);
        closeButton.setText("סגור");
        closeButton.setTextSize(20);
        closeButton.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        );
        buttonParams.setMargins(0, dp(28), 0, 0);
        root.addView(closeButton, buttonParams);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        screen.addView(root, cardParams);
        return screen;
    }

    private void playSound() {
        player = MediaPlayer.create(this, R.raw.gentle_bell);
        if (player == null) return;

        player.setLooping(true);
        player.start();
        int seconds = ReminderScheduler.getSoundSeconds(this);
        getWindow().getDecorView().postDelayed(this::stopSound, seconds * 1000L);
    }

    private void stopSound() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();
        player = null;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
