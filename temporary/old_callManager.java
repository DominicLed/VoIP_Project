package client;

import java.io.*;
import java.net.*;

public class old_callManager {

    public static void listenForCalls(int port) {

        try (ServerSocket server = new ServerSocket(port)) {
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
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            if ("CALL".equals(in.readLine())) {
                System.out.println("📞 Incoming call (r/d)");

                BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
                String input = console.readLine();
                
                System.out.println(input);
                if ("r".equals(input)) {
                    out.println("ACCEPT");
                    System.out.println("Call accepted succesfully");
                    startAudio(socket.getInetAddress());
                } else {
                    out.println("DECLINE");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void callUser(String username, String serverIp) {
        try {
            
            Socket lookup = new Socket(serverIp, 4000);

            PrintWriter out = new PrintWriter(lookup.getOutputStream(), true);
            BufferedReader server_response = new BufferedReader(new InputStreamReader(lookup.getInputStream()));

            /* Request the server to lookup the client being called */
            out.println("LOOKUP " + username);
            String response = server_response.readLine();

            if ("NOT_FOUND".equals(response)) {
                System.out.println("User not found");
                return;
            }
            /* IP:port */
            String[] addr = response.split(":");

            /* Initiating a socket connection to a client */
            Socket call = new Socket(addr[0], Integer.parseInt(addr[1]));

            PrintWriter callOut = new PrintWriter(call.getOutputStream(), true);
            BufferedReader callIn = new BufferedReader(new InputStreamReader(call.getInputStream()));

            System.out.println("========== Trying to call: " + username +   " ==========");

            callOut.println("CALL");

            if ("ACCEPT".equals(callIn.readLine())) {
                System.out.println("Call accepted YAY");
                startAudio(InetAddress.getByName(addr[0]));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startAudio(InetAddress peer) throws Exception {
        /* Creates a UDP socket */
        DatagramSocket socket = new DatagramSocket(6000);

        /* This thread handles outgoing audio */
        new Thread(new AudioSender(socket, peer)).start();
        /* This thread handles incomming audio */
        new Thread(new AudioReceiver(socket)).start();
    }
}