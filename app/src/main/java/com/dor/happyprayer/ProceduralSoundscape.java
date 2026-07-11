package com.dor.happyprayer;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** A tiny generative instrument: every reminder becomes a soft, evolving performance. */
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
        // Three intentionally consonant palettes: Cmaj7, D6 and Amaj7.
        double[][] palettes = {
                {130.81, 164.81, 196.00, 246.94, 523.25, 659.25, 783.99},
                {146.83, 185.00, 220.00, 246.94, 587.33, 739.99, 880.00},
                {110.00, 138.59, 164.81, 207.65, 440.00, 554.37, 659.25}
        };
        double[] notes = palettes[Math.max(0, Math.min(2, preset))];
        double[] phases = new double[4];
        double bellPhase = 0;
        int totalFrames = seconds * SAMPLE_RATE;
        float bellStart = 1.8f;
        int bellIndex = 0;

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

                double breath = .5 - .5 * Math.cos(Math.PI * 2 * time / 9.0);
                double masterEnvelope = smooth(Math.min(1, time / 3.2))
                        * smooth(Math.min(1, (1 - progress) * 10));
                double pad = 0;
                double[] weights = {.38, .22, .17, .12};
                for (int voice = 0; voice < 4; voice++) {
                    phases[voice] += Math.PI * 2 * notes[voice] / SAMPLE_RATE;
                    pad += Math.sin(phases[voice]) * weights[voice];
                    pad += Math.sin(phases[voice] * .5) * weights[voice] * .12;
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
                double sample = (pad * (.045 + breath * .012) + bell * .085) * masterEnvelope;
                double movement = .5 + .16 * Math.sin(time * .22);
                pcm[i * 2] = toShort(sample * (1.06 - movement));
                pcm[i * 2 + 1] = toShort(sample * (.06 + movement));
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
