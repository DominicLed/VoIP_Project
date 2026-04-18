package client;

import javax.sound.sampled.*;
import java.net.*;

public class AudioReceiver implements Runnable {

    private DatagramSocket socket;
    private AudioCodec codec = new AudioCodec();

    public AudioReceiver(DatagramSocket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            AudioFormat format = new AudioFormat(8000, 16, 1, true, true);
            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);

            speakers.open(format);
            speakers.start();

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] decoded = codec.decode(packet.getData());

                speakers.write(decoded, 0, decoded.length);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}