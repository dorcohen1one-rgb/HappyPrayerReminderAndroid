package com.dor.happyprayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

final class ReminderSoundPlayer {
    private MediaPlayer mediaPlayer;
    private TextToSpeech textToSpeech;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    void play(Context context, int soundMode, String message, int seconds) {
        stop();
        if (soundMode == ReminderScheduler.SOUND_VOICE || soundMode == ReminderScheduler.SOUND_MANTRA) {
            speak(context, soundMode, message);
            return;
        }

        int soundResource = R.raw.gentle_bell;
        if (soundMode == ReminderScheduler.SOUND_CALM_PAD) {
            soundResource = R.raw.calm_pad;
        } else if (soundMode == ReminderScheduler.SOUND_SOFT_CHIMES) {
            soundResource = R.raw.soft_chimes;
        }

        mediaPlayer = MediaPlayer.create(context, soundResource);
        if (mediaPlayer == null) return;
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private void speak(Context context, int soundMode, String message) {
        String text = message == null || message.trim().isEmpty()
                ? ReminderScheduler.DEFAULT_MESSAGE
                : message.trim();
        if (soundMode == ReminderScheduler.SOUND_MANTRA) {
            text = "הלוואי שכולם יהיו מאושרים ושמחים. הלוואי שכל היצורים יהיו בטוחים, רגועים, ושלווים.";
        }
        final String textToSpeak = text;

        textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
            mainHandler.post(() -> {
                if (status != TextToSpeech.SUCCESS || textToSpeech == null) return;

                int languageResult = textToSpeech.setLanguage(new Locale("he", "IL"));
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                        languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }

                textToSpeech.setSpeechRate(0.78f);
                textToSpeech.setPitch(0.95f);

                Bundle params = new Bundle();
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "happy_prayer_voice");
            });
        });
    }
}
