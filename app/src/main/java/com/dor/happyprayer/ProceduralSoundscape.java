package com.dor.happyprayer;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Original, gently evolving sound worlds rendered locally for private, offline reminders. */
final class ProceduralSoundscape {
    private static final int SAMPLE_RATE = 44100;
    private volatile boolean running;
    private Thread renderThread;
    private AudioTrack track;

    void play(int preset, int seconds) {
        stop();
        running = true;
        renderThread = new Thread(() -> render(Math.max(0, Math.min(6, preset)), Math.max(8, seconds)),
                "prayer-soundscape");
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
                .setBufferSizeInBytes(Math.max(minBuffer, 16_384))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        track = localTrack;
        localTrack.play();

        final double[][] palettes = {
                {130.81, 164.81, 196.00, 246.94, 523.25, 659.25, 783.99}, // crystal bells
                {110.00, 146.83, 164.81, 220.00, 440.00, 587.33, 659.25}, // warm breath
                {146.83, 185.00, 220.00, 293.66, 587.33, 739.99, 880.00}, // dawn pentatonic
                {87.31, 130.81, 174.61, 220.00, 349.23, 523.25, 698.46},  // guided voice
                {98.00, 146.83, 196.00, 246.94, 392.00, 587.33, 783.99},  // mantra
                {130.81, 174.61, 220.00, 261.63, 523.25, 659.25, 783.99}, // forest harp
                {73.42, 110.00, 146.83, 220.00, 293.66, 440.00, 587.33}   // moon bowls
        };
        final double[] notes = palettes[preset];
        final int frames = 1024;
        final int totalFrames = seconds * SAMPLE_RATE;
        final short[] pcm = new short[frames * 2];
        final double[] phases = new double[10];
        final double[] weights = {.30, .23, .16, .11};
        long seed = System.nanoTime() ^ ((long) preset << 20);
        double strikePhase = 0;
        double strikeAt = preset == 2 ? 0.8 : preset == 5 ? 1.1 : preset == 6 ? 2.5 : 1.7;
        int strikeIndex = preset == 4 ? 1 : 0;

        for (int base = 0; running && base < totalFrames; base += frames) {
            int count = Math.min(frames, totalFrames - base);
            for (int i = 0; i < count; i++) {
                double time = (base + i) / (double) SAMPLE_RATE;
                double progress = (base + i) / (double) totalFrames;
                double fade = smooth(Math.min(1, time / 2.8)) * smooth(Math.min(1, (1 - progress) * 8));
                double breath = .5 - .5 * Math.cos(Math.PI * 2 * time / (preset == 4 ? 7.5 : preset == 6 ? 12.0 : 11.0));
                double pad = 0;

                for (int voice = 0; voice < 4; voice++) {
                    double detune = 1.0 + (voice - 1.5) * .0015;
                    phases[voice] += Math.PI * 2 * notes[voice] / SAMPLE_RATE;
                    phases[voice + 4] += Math.PI * 2 * notes[voice] * detune / SAMPLE_RATE;
                    double weight = weights[voice];
                    pad += Math.sin(phases[voice]) * weight;
                    pad += Math.sin(phases[voice + 4]) * weight * .38;
                    if (preset == 1 || preset == 3 || preset == 4 || preset == 5 || preset == 6) {
                        pad += Math.sin(phases[voice] * .5) * weight * .14;
                    }
                }

                double gap = preset == 0 ? 5.5 : preset == 1 ? 9.0 : preset == 2 ? 2.25 : preset == 3 ? 7.0 : preset == 4 ? 4.2 : preset == 5 ? 6.4 : 8.8;
                if (time >= strikeAt + gap) {
                    strikeAt += gap;
                    strikeIndex = (strikeIndex + (preset == 2 ? 1 : 2)) % 3;
                    strikePhase = 0;
                }
                double age = time - strikeAt;
                double tone = 0;
                if (age >= 0 && age < 5.8) {
                    double frequency = notes[4 + strikeIndex];
                    strikePhase += Math.PI * 2 * frequency / SAMPLE_RATE;
                    double attack = Math.min(1, age / (preset == 2 ? .018 : preset == 5 ? .045 : .09));
                    double decay = Math.exp(-age * (preset == 2 ? 1.35 : preset == 6 ? .42 : .67));
                    tone = attack * decay * (Math.sin(strikePhase) * .70
                            + Math.sin(strikePhase * 2.01) * .16
                            + Math.sin(strikePhase * 3.04) * .06);
                }

                seed = seed * 6364136223846793005L + 1442695040888963407L;
                double air = (((seed >>> 33) / (double) (1L << 31)) - 1.0) * .0015;
                double padLevel = preset == 0 ? .026 : preset == 1 ? .070 : preset == 2 ? .020 : preset == 5 ? .038 : preset == 6 ? .055 : .050;
                double toneLevel = preset == 0 ? .12 : preset == 1 ? .025 : preset == 2 ? .105 : preset == 3 ? .030 : preset == 5 ? .070 : preset == 6 ? .052 : .045;
                double pulse = preset == 4 || preset == 6 ? .82 + breath * .18 : 1.0;
                double sample = (pad * (padLevel + breath * .008) * pulse + tone * toneLevel + air) * fade;
                double pan = .5 + .20 * Math.sin(time * (preset == 2 ? .52 : .16) + preset);
                pcm[i * 2] = toShort(sample * (1.05 - pan));
                pcm[i * 2 + 1] = toShort(sample * (.05 + pan));
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
