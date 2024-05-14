package fe.up.pt;
import jdk.net.Sockets;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.io.*;

public class Game {
    private List<Connection> connections;
    private int players;
    private boolean ranked;

    public Game(int players, List<Connection> connections, boolean ranked) {
        this.connections = connections;
        this.players = players;
        this.ranked = ranked;
    }

    public void start() throws IOException {

        for (int i = 0; i < this.players; i++) {
            this.connections.get(i).send("GSTART");
        }
        System.out.println("Game started!");




    }
}
