package fe.up.pt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {

    private final String username;
    private final String password;
    private final String token;
    private int rank;

    private final int port;
    private final String host;
    private SocketChannel socket;
    public Client (String username, String password, String token, int rank, int port, String host) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.rank = rank;
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
        System.out.println("Disconnected from server");
    }

    public void sendMessage(String message) throws IOException {
        this.socket.write(ByteBuffer.wrap(message.getBytes()));
    }

    public String getUsername(){
        return this.username;
    }

   public int getRank(){
        return this.rank;
   }

   public void increaseRank(int points){
        this.rank += points;
   }

   public static void main(String[] args) throws IOException {
       Client client = new Client("user", "pass", "token", 0, 12345, "localhost");
       client.connect();
   }

}

