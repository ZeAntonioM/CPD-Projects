package fe.up.pt;

import java.net.Socket;

public class User {
    private final String username;
    private final String password;
    private String token;
    private int rank;

    private Socket socket;

    public User(String username, String password, String token, int rank, Socket socket) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.rank = rank;
        this.socket = socket;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public int getRank() {
        return this.rank;
    }

    public String getToken() {
        return this.token;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setSocket(Socket userSocket) {
        this.socket = userSocket;
    }
}
