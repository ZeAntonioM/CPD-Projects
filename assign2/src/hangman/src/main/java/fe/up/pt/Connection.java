package fe.up.pt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Connection {

   private final int port;
   private final String host;
   private SocketChannel socket;

   public Connection(int port, String host) {
      this.port = port;
      this.host = host;
   }

   public void connect() throws IOException {
      this.socket = SocketChannel.open();
      this.socket.connect(new InetSocketAddress(this.host, this.port));
      System.out.println("Made connection request to " + this.host + " on port " + this.port);
   }

   public void disconnect() throws IOException {
      this.socket.close();
      System.out.println("Connection closed!");
   }

   private void send(String message) throws IOException {
      this.socket.write(ByteBuffer.wrap(message.getBytes()));
      System.out.println("Sent message: " + message);
   }

   public String receive() throws IOException {
      ByteBuffer buffer = ByteBuffer.allocate(1024);
      this.socket.read(buffer);
      return new String(buffer.array()).trim();
   }

   public void sendLogin(String username, String password) throws IOException {
      this.send("LGN:" + username + ":" + password);
   }

    public void sendRegister(String username, String password) throws IOException {
        this.send("REG:" + username + ":" + password);
    }

    public void sendLogout(String sessionToken) throws IOException {
        this.send("OUT:" + sessionToken);
    }

   public static void main(String[] args) throws IOException {
      Connection connection = new Connection(12345, "localhost");
      connection.connect();

      Scanner scanner = new Scanner(System.in);


        boolean active = true;
        while(active) {
           System.out.println("Login (L), Register (R) or Logout (O)?");
           String action = scanner.nextLine();
           while (!action.equals("L") && !action.equals("R") && !action.equals("O")) {
              System.out.println("Invalid action. Login (L), Register (R) or Logout (O)?");
              action = scanner.nextLine();
           }
           if (action.equals("L")) {
              System.out.println("Enter username: ");
              String username = scanner.nextLine();
              System.out.println("Enter password: ");
              String password = scanner.nextLine();

              connection.sendLogin(username, password);
           } else if (action.equals("R")) {
              System.out.println("Enter username: ");
              String username = scanner.nextLine();
              System.out.println("Enter password: ");
              String password = scanner.nextLine();

              connection.sendRegister(username, password);
           } else {
              System.out.println("Enter session token: ");
              String token = scanner.nextLine();
              connection.sendLogout(token);
              active = false;
           }

              String message = connection.receive();
                System.out.println("Received message: " + message);
        }
      connection.disconnect();
   }
}
