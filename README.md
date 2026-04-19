# VoIP_Project
This project is a standalone Voice over IP (VoIP) communication system developed in Java, designed to allow multiple users on the same local network to make real-time voice calls through the terminal. The application simulates the core functionality of modern calling platforms, but without the use of graphical interfaces or third-party communication frameworks.

The system enables users to register as online, discover other available users, initiate calls, accept or decline incoming calls, and engage in live two-way voice communication using UDP-based audio streaming.

## USAGE:
1. User A needs to extablish themself as both the client and the server. They run the server code on their local machine.
2. The server code must be run before anything else on person A's machine. The only argument to the programme is the tailscale IP of the server machine.
3. User B, C, ... can now run the client code with the tailscale IP of the server.
4. Follow the prompts given by the programme.

## DEPENDENCIES:
All users need tailscale installed and need to be on the same tailnet. 

## COMPILATION:
From project root run: **javac -d out client/*.java server/*.java**

## RUNNING THE CODE:
From the porject root:

1. Spin up the server first with: **java -cp out server.RestryServer <Tailscale IP address of the server machine>**
2. Spin up a client with: **java -cp out client.VoIPClient <Tailscale IP address of server>**
