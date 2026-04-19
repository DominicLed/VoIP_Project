package client;

import javax.sound.sampled.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class AudioReceiver implements Runnable {

    /* Define the audion format and frame size */
    private static final float SAMPLE_RATE = 16000.0f; /* How often the microphones analog sound vave is measured and converted into digital values */
    private static final int CHANNELS = 1; /* Mono audion (single channel) */
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int FRAME_MS = 20; /* Each audio frame represents 20 milliseconds of sound */
    private static final int FRAME_BYTES = (int) (SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE * FRAME_MS / 1000);

    private final DatagramSocket socket;
    private final AudioCodec codec = new AudioCodec();
    private final AtomicLong most_recent_received_nanos;

    public AudioReceiver(DatagramSocket socket, AtomicLong most_recent_received_nanos) {
        this.socket = socket;
        this.most_recent_received_nanos = most_recent_received_nanos;
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format, FRAME_BYTES * 12);
            speakers.start();

            setLineGain(speakers, 4.0f);

            byte[] recv = new byte[1500];

            while (true) {
                DatagramPacket packet = new DatagramPacket(recv, recv.length);
                socket.receive(packet);

                byte[] payload = Arrays.copyOf(packet.getData(), packet.getLength());
                byte[] decoded = codec.decode(payload);

                most_recent_received_nanos.set(System.nanoTime());
                speakers.write(decoded, 0, decoded.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setLineGain(Line line, float db) {
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl c = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float clamped = Math.max(c.getMinimum(), Math.min(c.getMaximum(), db));
            c.setValue(clamped);
        }
    }
}