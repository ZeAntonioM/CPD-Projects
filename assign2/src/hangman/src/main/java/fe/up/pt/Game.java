package fe.up.pt;
import jdk.net.Sockets;
import java.util.List;

public class Game {
    private List<Sockets> userSockets;
    private int players;

    public Game(int players, List<Sockets> userSockets) {
        this.userSockets = userSockets;
        this.players = players;
    }

    public void start() {
        System.out.println("Game started!");
    }
}
