package com.dor.happyprayer;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** A slow, layered ambient instrument with smooth envelopes and no repeated loop point. */
final class ProceduralSoundscape {
    private static final int SAMPLE_RATE = 44100;
    private volatile boolean running;
    private Thread renderThread;
    private AudioTrack track;

    void play(int preset, int seconds) {
        stop();
        running = true;
        renderThread = new Thread(() -> render(preset, Math.max(8, seconds)), "prayer-soundscape");
        renderThread.start();
    }

    void stop() {
        running = false;
        AudioTrack current = track;
        track = null;
        if (current != null) {
            try { current.pause(); } catch (IllegalStateException ignored) { }
            try { current.flush(); } catch (IllegalStateException ignored) { }
        }
        renderThread = null;
    }

    private void render(int preset, int seconds) {
        int minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack localTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(Math.max(minBuffer, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        track = localTrack;
        localTrack.play();

        final int frames = 1024;
        short[] pcm = new short[frames * 2];
        // Consonant palettes, voiced across octaves so they feel like a room rather than a ringtone.
        double[][] palettes = {
                {130.81, 164.81, 196.00, 246.94, 523.25, 659.25, 783.99},
                {146.83, 185.00, 220.00, 246.94, 587.33, 739.99, 880.00},
                {110.00, 138.59, 164.81, 207.65, 440.00, 554.37, 659.25}
        };
        double[] notes = palettes[Math.max(0, Math.min(2, preset))];
        double[] phases = new double[8];
        double bellPhase = 0;
        int totalFrames = seconds * SAMPLE_RATE;
        float bellStart = 1.8f;
        int bellIndex = 0;
        long seed = System.nanoTime() ^ (preset * 7919L);

        for (int base = 0; running && base < totalFrames; base += frames) {
            int count = Math.min(frames, totalFrames - base);
            for (int i = 0; i < count; i++) {
                float time = (base + i) / (float) SAMPLE_RATE;
                float progress = (base + i) / (float) totalFrames;
                float bellGap = preset == 2 ? 3.2f : 4.8f;
                if (time >= bellStart + bellGap) {
                    bellStart += bellGap;
                    bellIndex = (bellIndex + 2) % 3;
                    bellPhase = 0;
                }

                double breath = .5 - .5 * Math.cos(Math.PI * 2 * time / 10.5);
                double masterEnvelope = smooth(Math.min(1, time / 3.2))
                        * smooth(Math.min(1, (1 - progress) * 10));
                double pad = 0;
                double[] weights = {.28, .20, .16, .11};
                for (int voice = 0; voice < 4; voice++) {
                    phases[voice] += Math.PI * 2 * notes[voice] / SAMPLE_RATE;
                    // A lightly detuned companion keeps the chord warm without chorusing harshly.
                    phases[voice + 4] += Math.PI * 2 * notes[voice] * (1.0017 + voice * .0004) / SAMPLE_RATE;
                    pad += Math.sin(phases[voice]) * weights[voice];
                    pad += Math.sin(phases[voice + 4]) * weights[voice] * .42;
                    pad += Math.sin(phases[voice] * .5) * weights[voice] * .10;
                }

                double bellAge = time - bellStart;
                double bell = 0;
                if (bellAge >= 0 && bellAge < 5.5) {
                    double frequency = notes[4 + bellIndex];
                    bellPhase += Math.PI * 2 * frequency / SAMPLE_RATE;
                    double attack = Math.min(1, bellAge / .12);
                    double decay = Math.exp(-bellAge * .72);
                    bell = attack * decay * (Math.sin(bellPhase) * .74
                            + Math.sin(bellPhase * 2) * .17
                            + Math.sin(bellPhase * 3) * .05);
                }
                // Very low-level filtered noise gives the pad air, while deterministic randomness
                // preserves a clean render thread without allocating during playback.
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                double air = (((seed >>> 33) / (double) (1L << 31)) - 1.0) * .0022;
                double sample = (pad * (.040 + breath * .014) + bell * .092 + air) * masterEnvelope;
                double movement = .5 + .19 * Math.sin(time * .17 + preset);
                pcm[i * 2] = toShort(sample * (1.08 - movement));
                pcm[i * 2 + 1] = toShort(sample * (.04 + movement));
            }
            if (localTrack.write(pcm, 0, count * 2, AudioTrack.WRITE_BLOCKING) < 0) break;
        }

        if (track == localTrack) track = null;
        try { localTrack.stop(); } catch (IllegalStateException ignored) { }
        localTrack.release();
    }

    private short toShort(double sample) {
        return (short) (Math.max(-1, Math.min(1, sample)) * 32767);
    }

    private double smooth(double value) {
        return value * value * (3 - 2 * value);
    }
}
