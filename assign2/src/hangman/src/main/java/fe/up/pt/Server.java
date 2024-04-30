package fe.up.pt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Server {
    private final int port;
    private final int mode;
    private ServerSocketChannel serverSocketChannel;
    private static final int maxPlayers = 5;
    private final Connection connection;

    public Server(int port, int mode, String host){
       this.port = port;
       this.mode = mode;
       this.connection = new Connection(this.port, host);
    }

    public void start() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(this.port));
        System.out.println("Server started on mode " + this.mode + " on port " + this.port);
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(12345, 1, "localhost");
        server.start();
        boolean run = true;

        while (run){
            System.out.println("Waiting for connection...");
            SocketChannel client = server.serverSocketChannel.accept();
            System.out.println("Client connected from " + client.getRemoteAddress());
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            client.read(buffer);
            //print message in buffer
            System.out.println(new String(buffer.array(), StandardCharsets.UTF_8));
            // respond to client
            client.write(ByteBuffer.wrap("Hello client!".getBytes()));
            System.out.println("Message sent to client!");
            run = false;

        }

        server.serverSocketChannel.close();
    }
}
