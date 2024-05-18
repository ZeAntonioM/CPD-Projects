package fe.up.pt;

import java.lang.reflect.Array;
import java.net.Socket;
import java.util.Arrays;

public class User {
    private final String username;
    private final String password;
    private String[] tokens = new String[10];
    private int rank;

    private Socket socket;

    public User(String username, String password, String token, int rank, Socket socket) {
        this.username = username;
        this.password = password;
        this.tokens[0] = token;
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

    public String[] getTokens() {
        return this.tokens;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public boolean addToken (String token) {
        for (int i = 0; i < this.tokens.length; i++) {
            if (this.tokens[i] == null) {
                this.tokens[i] = token;
                return true;
            }
        }
        return false;
    }

    public synchronized boolean setActiveToken (String token){
        boolean ret = false;
        String[] newTokens = Arrays.copyOf(this.tokens, this.tokens.length);
        for (int i = 0; i < newTokens.length; i++) {
            if (newTokens[i] != null && newTokens[i].equals(token)) {
                newTokens[0] = token;
                ret = true;
            }
            newTokens[i] = null;
        }
        if (ret) {
            this.tokens = newTokens;
        }
        return ret;
    }

    public void setSocket(Socket userSocket) {
        this.socket = userSocket;
    }

}
