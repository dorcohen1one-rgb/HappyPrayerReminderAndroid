package com.dor.happyprayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
final class ReminderSoundPlayer {
    private MediaPlayer bedPlayer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    void play(Context context, int soundMode, String message, int seconds) {
        stop();
        switch (soundMode) {
            case ReminderScheduler.SOUND_MEDITATION_PIANO:
                playBed(context, R.raw.meditation_piano, 0.38f);
                break;
            case ReminderScheduler.SOUND_SOFT_DAYDREAM:
                playBed(context, R.raw.soft_daydream, 0.34f);
                break;
            case ReminderScheduler.SOUND_PERSONAL:
                playPersonal(context, ReminderScheduler.getPersonalAudioUri(context), 0.55f);
                break;
            case ReminderScheduler.SOUND_SILENT:
            default:
                break;
        }
    }

    void stop() {
        stopPlayer(bedPlayer);
        bedPlayer = null;
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void playBed(Context context, int soundResource, float volume) {
        bedPlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (bedPlayer == null) return;
        bedPlayer.setVolume(volume, volume);
        bedPlayer.setLooping(true);
        bedPlayer.start();
        fadeIn(bedPlayer, volume, 1800);
    }

    private void playPersonal(Context context, String value, float volume) {
        if (value == null || value.trim().isEmpty()) return;
        try {
            bedPlayer = MediaPlayer.create(context.getApplicationContext(), Uri.parse(value));
            if (bedPlayer == null) return;
            bedPlayer.setVolume(volume, volume);
            bedPlayer.setLooping(true);
            bedPlayer.start();
            fadeIn(bedPlayer, volume, 700);
        } catch (RuntimeException ignored) {
            bedPlayer = null;
        }
    }


    private void fadeIn(MediaPlayer player, float target, int durationMs) {
        player.setVolume(0f, 0f);
        final int steps = 18;
        for (int i = 1; i <= steps; i++) {
            final float volume = target * i / steps;
            mainHandler.postDelayed(() -> {
                try {
                    if (player.isPlaying()) player.setVolume(volume, volume);
                } catch (IllegalStateException ignored) {
                    // The preview may have been closed while the envelope was running.
                }
            }, durationMs * i / steps);
        }
    }

    private void stopPlayer(MediaPlayer player) {
        if (player == null) return;
        try {
            if (player.isPlaying()) {
                player.stop();
            }
        } catch (IllegalStateException ignored) {
            // Ignore players already stopped by the system.
        }
        player.release();
    }
}
