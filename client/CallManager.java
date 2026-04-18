package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

public class CallManager {

    private static final int REGISTRY_PORT = 4000;
    private static final int AUDIO_PORT = 6000;

    private static final Object CALL_LOCK = new Object();
    private static final AtomicLong LAST_RX_NANOS = new AtomicLong(0L);

    private static Socket pendingCallSocket = null;
    private static boolean audioStarted = false;

    public static void listenForCalls(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Listening for calls on TCP " + port);
            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handleIncoming(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleIncoming(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = in.readLine();

            if (!"CALL".equals(line)) {
                socket.close();
                return;
            }

            synchronized (CALL_LOCK) {
                if (pendingCallSocket != null) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("DECLINE");
                    socket.close();
                    return;
                }

                pendingCallSocket = socket;
                System.out.println("📞 Incoming call from " + socket.getInetAddress().getHostAddress());
                System.out.println("Type 'accept' or 'decline' in the command prompt.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void acceptPendingCall() {
        Socket socketToAccept;

        synchronized (CALL_LOCK) {
            if (pendingCallSocket == null) {
                System.out.println("No incoming call to accept.");
                return;
            }
            socketToAccept = pendingCallSocket;
            pendingCallSocket = null;
        }

        try {
            PrintWriter out = new PrintWriter(socketToAccept.getOutputStream(), true);
            out.println("ACCEPT");
            System.out.println("Call accepted successfully.");
            startAudio(socketToAccept.getInetAddress());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socketToAccept.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void declinePendingCall() {
        Socket socketToDecline;

        synchronized (CALL_LOCK) {
            if (pendingCallSocket == null) {
                System.out.println("No incoming call to decline.");
                return;
            }
            socketToDecline = pendingCallSocket;
            pendingCallSocket = null;
        }

        try {
            PrintWriter out = new PrintWriter(socketToDecline.getOutputStream(), true);
            out.println("DECLINE");
            System.out.println("Call declined.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socketToDecline.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void callUser(String username, String serverIp) {
        try (
            Socket lookup = new Socket(serverIp, REGISTRY_PORT);
            PrintWriter out = new PrintWriter(lookup.getOutputStream(), true);
            BufferedReader serverResponse = new BufferedReader(new InputStreamReader(lookup.getInputStream()))
        ) {
            out.println("LOOKUP " + username);
            String response = serverResponse.readLine();

            if ("NOT_FOUND".equals(response)) {
                System.out.println("User not found");
                return;
            }

            String[] addr = response.split(":");
            if (addr.length != 2) {
                System.out.println("Invalid lookup response: " + response);
                return;
            }

            String peerIp = addr[0];
            int peerSignalPort = Integer.parseInt(addr[1]);

            try (
                Socket call = new Socket(peerIp, peerSignalPort);
                PrintWriter callOut = new PrintWriter(call.getOutputStream(), true);
                BufferedReader callIn = new BufferedReader(new InputStreamReader(call.getInputStream()))
            ) {
                callOut.println("CALL");

                String reply = callIn.readLine();
                if ("ACCEPT".equals(reply)) {
                    System.out.println("Call accepted.");
                    startAudio(InetAddress.getByName(peerIp));
                } else {
                    System.out.println("Call declined.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startAudio(InetAddress peer) throws Exception {
        synchronized (CALL_LOCK) {
            if (audioStarted) {
                System.out.println("Audio is already running.");
                return;
            }
            audioStarted = true;
        }

        DatagramSocket socket = new DatagramSocket(AUDIO_PORT);
        System.out.println("Audio started on UDP " + AUDIO_PORT + " with peer " + peer.getHostAddress());

        new Thread(new AudioSender(socket, peer, LAST_RX_NANOS)).start();
        new Thread(new AudioReceiver(socket, LAST_RX_NANOS)).start();
    }
}