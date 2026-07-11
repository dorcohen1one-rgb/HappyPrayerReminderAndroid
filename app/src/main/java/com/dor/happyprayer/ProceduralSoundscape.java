package com.dor.happyprayer;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.Random;

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
        Random random = new Random(System.nanoTime());
        double[] scale = preset == 0
                ? new double[]{261.63, 329.63, 392.00, 523.25, 659.25}
                : preset == 2
                ? new double[]{293.66, 369.99, 440.00, 554.37, 739.99}
                : new double[]{220.00, 277.18, 329.63, 440.00, 554.37};
        double phaseA = 0, phaseB = 0, shimmerPhase = 0;
        int totalFrames = seconds * SAMPLE_RATE;
        float sparkleTime = -10f;
        double sparkleFrequency = scale[0] * 2;

        for (int base = 0; running && base < totalFrames; base += frames) {
            int count = Math.min(frames, totalFrames - base);
            for (int i = 0; i < count; i++) {
                float time = (base + i) / (float) SAMPLE_RATE;
                float progress = (base + i) / (float) totalFrames;
                if (time - sparkleTime > (preset == 2 ? 2.1f : 3.8f) && random.nextFloat() < 0.00016f) {
                    sparkleTime = time;
                    sparkleFrequency = scale[random.nextInt(scale.length)] * (preset == 1 ? 1.0 : 2.0);
                }

                double breath = .5 - .5 * Math.cos(Math.PI * 2 * time / 7.5);
                double masterEnvelope = Math.min(1, time / 2.4) * Math.min(1, (1 - progress) * 8);
                double fundamental = scale[preset == 2 ? 1 : 0] / 2;
                phaseA += Math.PI * 2 * fundamental / SAMPLE_RATE;
                phaseB += Math.PI * 2 * (fundamental * 1.501) / SAMPLE_RATE;
                shimmerPhase += Math.PI * 2 * sparkleFrequency / SAMPLE_RATE;

                double pad = Math.sin(phaseA) * .42 + Math.sin(phaseB) * .20
                        + Math.sin(phaseA * .501) * .16;
                double age = time - sparkleTime;
                double sparkle = age >= 0 && age < 4.5
                        ? Math.sin(shimmerPhase) * Math.exp(-age * (preset == 0 ? 1.25 : .72))
                        + Math.sin(shimmerPhase * 2.003) * .22 * Math.exp(-age * 1.8)
                        : 0;
                double air = (random.nextDouble() * 2 - 1) * .018 * breath;
                double sample = (pad * (.055 + breath * .025) + sparkle * .16 + air) * masterEnvelope;
                double pan = .5 + .32 * Math.sin(time * .31);
                pcm[i * 2] = toShort(sample * (1.15 - pan));
                pcm[i * 2 + 1] = toShort(sample * (.35 + pan));
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
}
