package client;

import javax.sound.sampled.*;
import java.net.*;

public class AudioSender implements Runnable {

    private DatagramSocket socket;
    private InetAddress peer;
    private AudioCodec codec = new AudioCodec();

    public AudioSender(DatagramSocket socket, InetAddress peer) {
        this.socket = socket;
        this.peer = peer;
    }

    public void run() {
        try {

            AudioFormat format = new AudioFormat(8000, 16, 1, true, true);
            TargetDataLine mic_line = AudioSystem.getTargetDataLine(format);

            mic_line.open(format);
            mic_line.start();

            byte[] buffer = new byte[960];

            while (true) {
                int count = mic_line.read(buffer, 0, buffer.length);

                byte[] encoded = codec.encode(buffer);

                DatagramPacket packet = new DatagramPacket(encoded, encoded.length, peer, 6000);

                socket.send(packet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}