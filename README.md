# VoIP_Project
UDP voice calls with java

## COMPILATION:
From project root run: **javac -d out client/*.java server/*.java**

## RUNNING THE CODE:
From the porject root:

1) Spin up the server first with: **java -cp out server.RestryServer**
2) Spin up a client with: **java -cp out client.VoIPClient <Tailscale IP address of server>**
