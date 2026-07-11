package com.dor.happyprayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

final class ReminderSoundPlayer {
    private MediaPlayer bedPlayer;
    private MediaPlayer accentPlayer;
    private MediaPlayer voicePlayer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    void play(Context context, int soundMode, String message, int seconds) {
        stop();
        if (soundMode == ReminderScheduler.SOUND_MANTRA) {
            playBed(context, R.raw.calm_pad, 0.12f);
            mainHandler.postDelayed(() -> playAccent(context, R.raw.soft_chimes, 0.075f), 650);
            playVoice(context, R.raw.mantra_voice);
            return;
        }
        if (soundMode == ReminderScheduler.SOUND_VOICE) {
            playBed(context, R.raw.calm_pad, 0.09f);
            mainHandler.postDelayed(() -> playAccent(context, R.raw.gentle_bell, 0.055f), 450);
            playVoice(context, R.raw.default_voice);
            return;
        }

        if (soundMode == ReminderScheduler.SOUND_BELL) {
            playAccent(context, R.raw.gentle_bell, 0.48f);
            mainHandler.postDelayed(() -> playBed(context, R.raw.soft_chimes, 0.16f), 1150);
            return;
        }

        int soundResource = R.raw.gentle_bell;
        float volume = 0.48f;
        if (soundMode == ReminderScheduler.SOUND_CALM_PAD) {
            soundResource = R.raw.calm_pad;
            volume = 0.38f;
        } else if (soundMode == ReminderScheduler.SOUND_SOFT_CHIMES) {
            soundResource = R.raw.soft_chimes;
            volume = 0.46f;
        }

        playBed(context, soundResource, volume);
    }

    void stop() {
        stopPlayer(bedPlayer);
        bedPlayer = null;
        stopPlayer(accentPlayer);
        accentPlayer = null;
        stopPlayer(voicePlayer);
        voicePlayer = null;
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

    private void playAccent(Context context, int soundResource, float volume) {
        accentPlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (accentPlayer == null) return;
        accentPlayer.setVolume(volume, volume);
        accentPlayer.setLooping(false);
        accentPlayer.start();
        fadeIn(accentPlayer, volume, 550);
    }

    private void playVoice(Context context, int soundResource) {
        voicePlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (voicePlayer == null) {
            playAccent(context, R.raw.gentle_bell, 0.08f);
            return;
        }
        voicePlayer.setVolume(0.82f, 0.82f);
        voicePlayer.setLooping(false);
        voicePlayer.start();
        fadeIn(voicePlayer, 0.82f, 700);
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
