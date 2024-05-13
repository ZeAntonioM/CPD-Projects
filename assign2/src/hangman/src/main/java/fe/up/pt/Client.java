package fe.up.pt;

public class Client {

    private final String username;
    private final String password;
    private String token;
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
    public String getPassword() {
        return this.password;
    }

   public int getRank(){
        return this.rank;
   }

   public String getToken(){
        return this.token;
   }

    public void setToken(String token){
          this.token = token;
    }

   public void increaseRank(int points){
        this.rank += points;
   }

}

