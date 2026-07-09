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
            playBed(context, R.raw.calm_pad, 0.14f);
            playAccent(context, R.raw.soft_chimes, 0.08f);
            playVoice(context, R.raw.mantra_voice);
            return;
        }
        if (soundMode == ReminderScheduler.SOUND_VOICE) {
            playBed(context, R.raw.calm_pad, 0.10f);
            playAccent(context, R.raw.gentle_bell, 0.07f);
            playVoice(context, R.raw.default_voice);
            return;
        }

        int soundResource = R.raw.gentle_bell;
        float volume = 1.0f;
        if (soundMode == ReminderScheduler.SOUND_CALM_PAD) {
            soundResource = R.raw.calm_pad;
            volume = 0.56f;
        } else if (soundMode == ReminderScheduler.SOUND_SOFT_CHIMES) {
            soundResource = R.raw.soft_chimes;
            volume = 0.72f;
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
    }

    private void playAccent(Context context, int soundResource, float volume) {
        accentPlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (accentPlayer == null) return;
        accentPlayer.setVolume(volume, volume);
        accentPlayer.setLooping(false);
        accentPlayer.start();
    }

    private void playVoice(Context context, int soundResource) {
        voicePlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (voicePlayer == null) {
            playAccent(context, R.raw.gentle_bell, 0.08f);
            return;
        }
        voicePlayer.setVolume(0.97f, 0.97f);
        voicePlayer.setLooping(false);
        voicePlayer.start();
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
