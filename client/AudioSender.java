package client;

import javax.sound.sampled.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class AudioSender implements Runnable {

    private static final int AUDIO_PORT = 6000;
    private static final float SAMPLE_RATE = 16000.0f;
    private static final int CHANNELS = 1;
    private static final int FRAME_MS = 20;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int FRAME_BYTES = (int) (SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE * FRAME_MS / 1000);

    private final DatagramSocket socket;
    private final InetAddress peer;
    private final AudioCodec codec = new AudioCodec();
    private final AtomicLong lastRxNanos;

    public AudioSender(DatagramSocket socket, InetAddress peer, AtomicLong lastRxNanos) {
        this.socket = socket;
        this.peer = peer;
        this.lastRxNanos = lastRxNanos;
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
            TargetDataLine micLine = AudioSystem.getTargetDataLine(format);
            micLine.open(format, FRAME_BYTES * 8);
            micLine.start();

            setLineGain(micLine, 8.0f);

            byte[] frame = new byte[FRAME_BYTES];

            while (true) {
                int read = readExact(micLine, frame, FRAME_BYTES);
                if (read <= 0) continue;

                boolean recentPlayback = (System.nanoTime() - lastRxNanos.get()) < 120_000_000L;
                if (recentPlayback) {
                    continue;
                }

                applyAgc(frame, 0, read, 9000);

                byte[] encoded = codec.encode(Arrays.copyOf(frame, read));
                DatagramPacket packet = new DatagramPacket(encoded, encoded.length, peer, AUDIO_PORT);
                socket.send(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int readExact(TargetDataLine line, byte[] buf, int wanted) {
        int off = 0;
        while (off < wanted) {
            int n = line.read(buf, off, wanted - off);
            if (n <= 0) break;
            off += n;
        }
        return off;
    }

    private static void setLineGain(Line line, float db) {
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl c = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float clamped = Math.max(c.getMinimum(), Math.min(c.getMaximum(), db));
            c.setValue(clamped);
        }
    }

    private static void applyAgc(byte[] pcm, int off, int len, int targetRms) {
        long sumSq = 0;
        int samples = 0;
        for (int i = off; i + 1 < off + len; i += 2) {
            int s = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            sumSq += (long) s * s;
            samples++;
        }
        if (samples == 0) return;

        double rms = Math.sqrt(sumSq / (double) samples);
        if (rms < 200.0) return;

        double gain = targetRms / rms;
        gain = Math.max(0.7, Math.min(2.5, gain));

        for (int i = off; i + 1 < off + len; i += 2) {
            int s = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            int out = (int) Math.round(s * gain);
            if (out > Short.MAX_VALUE) out = Short.MAX_VALUE;
            if (out < Short.MIN_VALUE) out = Short.MIN_VALUE;
            pcm[i] = (byte) (out & 0xFF);
            pcm[i + 1] = (byte) ((out >>> 8) & 0xFF);
        }
    }
}