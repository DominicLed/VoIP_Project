package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class old_VoIPClient {
    private static final int SERVER_PORT = 4000;

    public static String username;

    public static void main(String[] args) throws Exception {

        String serverIp = args[0];
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter username: ");
        username = scanner.nextLine();

        /* Register the newly joined user */
        register(serverIp);
        
        System.out.println("==========" + username + " registered " + "==========");
        
        new Thread(() -> CallManager.listenForCalls(5500)).start();

        while (true) {
            System.out.println("(list) online users or (call) a user: ");
            String cmd = scanner.nextLine();

            if (cmd.equals("list")) {
                listUsers(serverIp);
            } else if (cmd.startsWith("call")) {
                String target = cmd.split(" ")[1];
                CallManager.callUser(target, serverIp);
            }
        }
    }

    private static void register(String serverIp) throws Exception {
       
        System.out.println("========== Inside register function: ==========");
     
        /* Create a connection with the server to register the user */
        Socket socket = new Socket(serverIp, SERVER_PORT);

        /* Extract the clients local IP from the socket */
        String clientIP = socket.getLocalAddress().getHostAddress();

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.println("REGISTER " + username + " " + clientIP + " 5500");

        socket.close();
    }

    /* Create a connection with the server to list the online/registerd users*/
    private static void listUsers(String serverIp) throws Exception {
        
        Socket socket = new Socket(serverIp, SERVER_PORT);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        /* Request a list of online users from the server */
        out.println("LIST");

        /* Prints the list of online users */
        System.out.println("Online: " + in.readLine());
        socket.close();
    }
}