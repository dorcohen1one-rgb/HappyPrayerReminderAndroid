package com.dor.happyprayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

final class ReminderSoundPlayer {
    private MediaPlayer mediaPlayer;
    private MediaPlayer voicePlayer;
    private TextToSpeech textToSpeech;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    void play(Context context, int soundMode, String message, int seconds) {
        stop();
        if (soundMode == ReminderScheduler.SOUND_MANTRA) {
            playLoopingSound(context, R.raw.calm_pad, 0.36f);
            playVoice(context, R.raw.mantra_voice, 0.94f);
            return;
        }
        if (soundMode == ReminderScheduler.SOUND_VOICE) {
            playLoopingSound(context, R.raw.calm_pad, 0.18f);
            playVoice(context, R.raw.default_voice, 0.94f);
            return;
        }

        int soundResource = R.raw.gentle_bell;
        if (soundMode == ReminderScheduler.SOUND_CALM_PAD) {
            soundResource = R.raw.calm_pad;
        } else if (soundMode == ReminderScheduler.SOUND_SOFT_CHIMES) {
            soundResource = R.raw.soft_chimes;
        }

        playLoopingSound(context, soundResource, 1.0f);
    }

    private void playLoopingSound(Context context, int soundResource, float volume) {
        mediaPlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (mediaPlayer == null) return;
        mediaPlayer.setVolume(volume, volume);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void playVoice(Context context, int soundResource, float volume) {
        voicePlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (voicePlayer == null) {
            speak(context, ReminderScheduler.SOUND_VOICE, ReminderScheduler.DEFAULT_MESSAGE);
            return;
        }
        voicePlayer.setVolume(volume, volume);
        voicePlayer.setLooping(false);
        voicePlayer.start();
    }

    void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (voicePlayer != null) {
            if (voicePlayer.isPlaying()) {
                voicePlayer.stop();
            }
            voicePlayer.release();
            voicePlayer = null;
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
                if (status != TextToSpeech.SUCCESS || textToSpeech == null) {
                    playFallbackIfSilent(context);
                    return;
                }

                int languageResult = textToSpeech.setLanguage(new Locale("he", "IL"));
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                        languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    int defaultLanguageResult = textToSpeech.setLanguage(Locale.getDefault());
                    if (defaultLanguageResult == TextToSpeech.LANG_MISSING_DATA ||
                            defaultLanguageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        playFallbackIfSilent(context);
                        return;
                    }
                }

                textToSpeech.setSpeechRate(0.78f);
                textToSpeech.setPitch(0.95f);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build();
                    textToSpeech.setAudioAttributes(attributes);
                }

                Bundle params = new Bundle();
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
                params.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
                int result = textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "happy_prayer_voice");
                if (result == TextToSpeech.ERROR) {
                    playFallbackIfSilent(context);
                }
            });
        });
    }

    private void playFallbackIfSilent(Context context) {
        if (mediaPlayer != null) return;
        playLoopingSound(context, R.raw.gentle_bell, 1.0f);
    }
}
