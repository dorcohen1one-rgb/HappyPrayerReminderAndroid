package com.dor.happyprayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import java.util.Locale;
import java.util.Set;

final class ReminderSoundPlayer {
    private MediaPlayer mediaPlayer;
    private MediaPlayer fallbackVoicePlayer;
    private TextToSpeech textToSpeech;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    void play(Context context, int soundMode, String message, int seconds) {
        stop();
        if (soundMode == ReminderScheduler.SOUND_MANTRA) {
            playLoopingSound(context, R.raw.calm_pad, 0.28f);
            speakSerene(context, "הלוואי שכל היצורים יהיו בטוחים, רגועים, ושלווים. הלוואי שכולם יהיו מאושרים ושמחים.", R.raw.mantra_voice);
            return;
        }
        if (soundMode == ReminderScheduler.SOUND_VOICE) {
            playLoopingSound(context, R.raw.calm_pad, 0.16f);
            String spokenText = normalizeMessage(message);
            speakSerene(context, spokenText, R.raw.default_voice);
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

    private String normalizeMessage(String message) {
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            return ReminderScheduler.DEFAULT_MESSAGE;
        }
        return text;
    }

    private void playLoopingSound(Context context, int soundResource, float volume) {
        mediaPlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (mediaPlayer == null) return;
        mediaPlayer.setVolume(volume, volume);
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
        if (fallbackVoicePlayer != null) {
            if (fallbackVoicePlayer.isPlaying()) {
                fallbackVoicePlayer.stop();
            }
            fallbackVoicePlayer.release();
            fallbackVoicePlayer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void speakSerene(Context context, String message, int fallbackSoundResource) {
        final String textToSpeak = normalizeMessage(message);

        textToSpeech = new TextToSpeech(context.getApplicationContext(), status ->
                mainHandler.post(() -> {
                    if (status != TextToSpeech.SUCCESS || textToSpeech == null) {
                        playFallbackVoice(context, fallbackSoundResource);
                        return;
                    }

                    if (!configureSpeechEngine(textToSpeech)) {
                        playFallbackVoice(context, fallbackSoundResource);
                        return;
                    }

                    textToSpeech.setSpeechRate(0.84f);
                    textToSpeech.setPitch(0.92f);
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
                        playFallbackVoice(context, fallbackSoundResource);
                    }
                }));
    }

    private boolean configureSpeechEngine(TextToSpeech speech) {
        int languageResult = speech.setLanguage(new Locale("he", "IL"));
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            int fallbackLanguageResult = speech.setLanguage(Locale.getDefault());
            if (fallbackLanguageResult == TextToSpeech.LANG_MISSING_DATA ||
                    fallbackLanguageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                return false;
            }
        }

        Voice bestVoice = chooseBestVoice(speech.getVoices());
        if (bestVoice != null) {
            try {
                speech.setVoice(bestVoice);
            } catch (IllegalArgumentException ignored) {
                // Keep the engine's default voice if the selected voice is not accepted.
            }
        }
        return true;
    }

    private Voice chooseBestVoice(Set<Voice> voices) {
        if (voices == null || voices.isEmpty()) return null;
        Voice best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Voice voice : voices) {
            Locale locale = voice.getLocale();
            if (locale == null || !"he".equals(locale.getLanguage())) continue;
            int score = 0;
            switch (voice.getQuality()) {
                case Voice.QUALITY_VERY_HIGH:
                    score += 40;
                    break;
                case Voice.QUALITY_HIGH:
                    score += 28;
                    break;
                case Voice.QUALITY_NORMAL:
                    score += 15;
                    break;
                default:
                    score += 6;
                    break;
            }
            switch (voice.getLatency()) {
                case Voice.LATENCY_VERY_LOW:
                    score += 18;
                    break;
                case Voice.LATENCY_LOW:
                    score += 12;
                    break;
                case Voice.LATENCY_NORMAL:
                    score += 6;
                    break;
                default:
                    score += 2;
                    break;
            }
            if (!voice.isNetworkConnectionRequired()) {
                score += 8;
            }
            if (score > bestScore) {
                bestScore = score;
                best = voice;
            }
        }
        return best;
    }

    private void playFallbackVoice(Context context, int soundResource) {
        if (mediaPlayer != null) return;
        fallbackVoicePlayer = MediaPlayer.create(context.getApplicationContext(), soundResource);
        if (fallbackVoicePlayer == null) {
            playLoopingSound(context, R.raw.gentle_bell, 1.0f);
            return;
        }
        fallbackVoicePlayer.setVolume(0.94f, 0.94f);
        fallbackVoicePlayer.setLooping(false);
        fallbackVoicePlayer.start();
    }
}
