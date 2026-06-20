package com.dor.happyprayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

final class ReminderSoundPlayer {
    private MediaPlayer mediaPlayer;
    private TextToSpeech textToSpeech;

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
        textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status != TextToSpeech.SUCCESS || textToSpeech == null) return;
            textToSpeech.setLanguage(new Locale("he", "IL"));
            textToSpeech.setSpeechRate(0.82f);
            textToSpeech.setPitch(0.92f);
            String text = message;
            if (soundMode == ReminderScheduler.SOUND_MANTRA) {
                text = "הלוואי שכולם יהיו מאושרים ושמחים. הלוואי שכל היצורים יהיו בטוחים, רגועים, ושלווים.";
            }
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "happy_prayer_voice");
        });
    }
}
