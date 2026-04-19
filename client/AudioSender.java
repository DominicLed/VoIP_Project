package client;

import javax.sound.sampled.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/* AudioSender and AudioReceiver implement Runnable so they can run on separate threads, 
allowing sending and receiving audio concurrently for real-time voice communication. */
public class AudioSender implements Runnable {

    private static final int AUDIO_PORT = 6000;
    private static final float SAMPLE_RATE = 16000.0f;
    private static final int CHANNELS = 1;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int FRAME_MS = 20;
    /* How many btes are in one 20ms frame. Sets speaker buffer size = 640 bytes after math*/
    private static final int FRAME_BYTES = (int) (SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE * FRAME_MS / 1000);

    /* PCM -> Pulse Code Mudulation */

    private final DatagramSocket socket;
    private final AudioCodec codec = new AudioCodec();
    private final InetAddress peer;
    private final AtomicLong most_recent_received_nanos;

    public AudioSender(DatagramSocket socket, InetAddress peer, AtomicLong most_recent_receoved_nanos) {
        this.socket = socket;
        this.peer = peer;
        this.most_recent_received_nanos = most_recent_receoved_nanos;
    }

    @Override
    public void run() {
        try {
            /* ============ SETS UP AUDIO READING ENVIRONEMNT =================== */ 
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
            TargetDataLine micLine = AudioSystem.getTargetDataLine(format);
            micLine.open(format, FRAME_BYTES * 8);
            micLine.start();
            /* ================================================================= */

            /* Amplifies microphone input by about +8 dB (decibiles) (if supported) */
            /* Can make voice louder but may increase clipping/noise if too high. */
            adjust_audio_volume_or_gain(micLine, 8.0f);

            byte[] audio_frame = new byte[FRAME_BYTES];

            while (true) {
                /* Read in a specified amount of bytes into the buffer */
                int bytesRead = readExact(micLine, audio_frame, FRAME_BYTES);
                if (bytesRead <= 0) continue;

                /* Echo feedback supression gate */
                boolean recentPlayback = (System.nanoTime() - most_recent_received_nanos.get()) < 120_000_000L;
                if (recentPlayback) {
                    continue;
                }

                applyAgc(audio_frame, 0, bytesRead, 9000);

                byte[] encoded = codec.encode(Arrays.copyOf(audio_frame, bytesRead));
                DatagramPacket packet = new DatagramPacket(encoded, encoded.length, peer, AUDIO_PORT);
                socket.send(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Repetedly reads audio data from the microphone line until it has collected the exact number of bytes required */
    private static int readExact(TargetDataLine mic_input_stream, byte[] buf, int bytes_requested) {
        int numBytesInBuf = 0;

        while (numBytesInBuf < bytes_requested) {
            int bytesRead = mic_input_stream.read(buf, numBytesInBuf, bytes_requested - numBytesInBuf);
            /* No more bytes left to read */
            if (bytesRead <= 0) {
                break;
            }
            numBytesInBuf += bytesRead;
        }
        return numBytesInBuf;
    }

    /* Adjust the audio volume/gain level of a sound line (microphone input line or speaker output line), using decibliles(dB),
    but only if that device supports gain control */
    private static void adjust_audio_volume_or_gain(Line line, float db) {
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            /* Aquiring the volume control for the device */
            FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            /* Ensures the requested dB stayes in the sypported range */
            float clamped = Math.max(control.getMinimum(), Math.min(control.getMaximum(), db));
            control.setValue(clamped);
        }
        return;
    }

    /* Simple Automatic Gain Control (AGC) algorithm.
    Automatically adjust the loudness of a chunk of microphone audio so quiet speech 
    becomes louder and loud speech becomes softer, aiming for a target level. */
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