package fe.up.pt;
import jdk.net.Sockets;
import java.util.List;

public class Game {
    private List<Sockets> userSockets;
    private int players;
    private boolean ranked;
    public Game(int players, List<Sockets> userSockets, boolean ranked) {
        this.userSockets = userSockets;
        this.players = players;
        this.ranked = ranked;
    }

    public void start() {
        System.out.println("Game started!");




    }
}
