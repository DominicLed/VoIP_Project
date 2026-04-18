package server;

    import java.io.*;
    import java.net.*;
    import java.util.concurrent.ConcurrentHashMap;

public class RegistryServer {

    private static final int PORT = 4000;
    /* [key: username] -- [value: IP address:port]  */
    private static ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {

        /* Binds to and listens on all local network interfaces -> 0.0.0.0 */
        ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));
        System.out.println("🌐 Registry Server running...");

        while (true) {
            /* Blocks on accept */
            Socket socket = serverSocket.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private static void handleClient(Socket socket) {
        System.out.println("New connection from " + socket.getInetAddress().getHostAddress());
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line = in.readLine();
            String[] parts = line.split(" ");

            switch (parts[0]) {
                /* Adding user to the concurrent hash map */
                case "REGISTER":
                    users.put(parts[1], parts[2] + ":" + parts[3]);
                    out.println("OK");
                    break; 
                case "LIST":
                    /* Return a list of all online users */
                    out.println(String.join(",", users.keySet()));
                    break;

                case "LOOKUP":
                    /* Lookup a specific user to see if they are online */
                    out.println(users.getOrDefault(parts[1], "NOT_FOUND"));
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}