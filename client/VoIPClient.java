package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class VoIPClient {
    private static final int SERVER_PORT = 4000;
    private static final int CALL_SIGNAL_PORT = 5500;

    public static String username;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -cp out client.VoIPClient <server-ip>");
            return;
        }

        String serverIp = args[0];
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter username: ");
        username = scanner.nextLine().trim();

        register(serverIp);

        new Thread(() -> CallManager.listenForCalls(CALL_SIGNAL_PORT)).start();

        printHelp();
        while (true) {
            System.out.print("(list/call/accept/decline/help): ");
            String cmd = scanner.nextLine().trim();

            if (cmd.equals("list")) {
                listUsers(serverIp);
            } else if (cmd.startsWith("call ")) {
                String[] parts = cmd.split("\\s+", 2);
                if (parts.length < 2 || parts[1].isBlank()) {
                    System.out.println("Usage: call <username>");
                    continue;
                }

                String target = parts[1].trim();
                if (target.equals(username)) {
                    System.out.println("You cannot call yourself.");
                    continue;
                }

                CallManager.callUser(target, serverIp);
            } else if (cmd.equals("accept")) {
                CallManager.acceptPendingCall();
            } else if (cmd.equals("decline")) {
                CallManager.declinePendingCall();
            } else if (cmd.equals("help")) {
                printHelp();
            } else {
                System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    private static void register(String serverIp) throws Exception {
      
        try (
            Socket socket = new Socket(serverIp, SERVER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String clientIP = socket.getLocalAddress().getHostAddress();
            out.println("REGISTER " + username + " " + clientIP + " " + CALL_SIGNAL_PORT);

            String ack = in.readLine();
        }
    }

    private static void listUsers(String serverIp) throws Exception {
        try (
            Socket socket = new Socket(serverIp, SERVER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("LIST");
            System.out.println("Online: " + in.readLine());
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  list");
        System.out.println("  call <username>");
        System.out.println("  accept");
        System.out.println("  decline");
        System.out.println("  help");
    }
}