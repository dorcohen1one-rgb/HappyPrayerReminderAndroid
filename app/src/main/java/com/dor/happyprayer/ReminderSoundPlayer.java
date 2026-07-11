package com.dor.happyprayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

final class ReminderSoundPlayer {
    private MediaPlayer bedPlayer;
    private MediaPlayer accentPlayer;
    private MediaPlayer voicePlayer;
    private ProceduralSoundscape soundscape;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    void play(Context context, int soundMode, String message, int seconds) {
        stop();
        soundscape = new ProceduralSoundscape();
        if (soundMode == ReminderScheduler.SOUND_MANTRA) {
            soundscape.play(1, seconds);
            playVoice(context, R.raw.mantra_voice);
            return;
        }
        if (soundMode == ReminderScheduler.SOUND_VOICE) {
            soundscape.play(1, seconds);
            playVoice(context, R.raw.default_voice);
            return;
        }
        soundscape.play(soundMode, seconds);
    }

    void stop() {
        stopPlayer(bedPlayer);
        bedPlayer = null;
        stopPlayer(accentPlayer);
        accentPlayer = null;
        stopPlayer(voicePlayer);
        voicePlayer = null;
        mainHandler.removeCallbacksAndMessages(null);
        if (soundscape != null) soundscape.stop();
        soundscape = null;
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
