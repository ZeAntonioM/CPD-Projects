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
    public Client (String username, String password, String token, int rank) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.rank = rank;
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

}

