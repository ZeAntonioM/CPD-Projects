package fe.up.pt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server {
    private final int port;
    private final int mode;
    private ServerSocketChannel serverSocketChannel;
    private static final int maxPlayers = 5;

    public Server(int port, int mode){
       this.port = port;
       this.mode = mode;
    }

    public void start() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(this.port));
        System.out.println("Server started on mode " + this.mode + " on port " + this.port);
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(12345, 1);
        server.start();
        boolean run = true;

        while (run){
            System.out.println("Waiting for connection...");
            SocketChannel client = server.serverSocketChannel.accept();
            System.out.println("Client connected from " + client.getRemoteAddress());
            System.out.println("Connection accepted!");
            run = false;
        }

        server.serverSocketChannel.close();
    }
}
