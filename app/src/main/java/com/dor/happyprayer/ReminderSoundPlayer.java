package com.dor.happyprayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.Locale;

final class ReminderSoundPlayer {
    private MediaPlayer bedPlayer;
    private MediaPlayer accentPlayer;
    private MediaPlayer voicePlayer;
    private ProceduralSoundscape soundscape;
    private TextToSpeech speech;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    void play(Context context, int soundMode, String message, int seconds) {
        stop();
        soundscape = new ProceduralSoundscape();
        if (soundMode == ReminderScheduler.SOUND_MANTRA) {
            soundscape.play(2, seconds);
            speak(context, message, true);
            return;
        }
        if (soundMode == ReminderScheduler.SOUND_VOICE) {
            soundscape.play(1, seconds);
            speak(context, message, false);
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
        if (speech != null) {
            speech.stop();
            speech.shutdown();
            speech = null;
        }
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

    private void speak(Context context, String message, boolean isMantra) {
        String prayer = message == null || message.trim().isEmpty()
                ? ReminderScheduler.DEFAULT_MESSAGE : message.trim();
        String script = isMantra
                ? "ניקח נשימה איטית. " + prayer + ". נניח למילים להישאר איתנו בשקט."
                : "רגע לעצמך. " + prayer + ".";
        speech = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status != TextToSpeech.SUCCESS || speech == null) return;
            Locale hebrew = new Locale("he", "IL");
            if (speech.setLanguage(hebrew) == TextToSpeech.LANG_MISSING_DATA) return;
            selectBestHebrewVoice(hebrew);
            speech.setSpeechRate(isMantra ? 0.76f : 0.84f);
            speech.setPitch(isMantra ? 0.93f : 0.98f);
            speech.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) { }
                @Override public void onDone(String utteranceId) { }
                @Override public void onError(String utteranceId) { }
            });
            mainHandler.postDelayed(() -> {
                if (speech != null) speech.speak(script, TextToSpeech.QUEUE_FLUSH, null, "prayer-guidance");
            }, 1100L);
        });
    }

    private void selectBestHebrewVoice(Locale hebrew) {
        if (speech == null || speech.getVoices() == null) return;
        Voice best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Voice candidate : speech.getVoices()) {
            if (!hebrew.getLanguage().equals(candidate.getLocale().getLanguage())) continue;
            // Prefer the best voice that can play without a data connection. It remains reliable for alarms.
            int score = candidate.getQuality() + (candidate.isNetworkConnectionRequired() ? 0 : 10_000);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        if (best != null) speech.setVoice(best);
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
