package fe.up.pt;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {
    private final int port;
    private final int mode;
    private Dictionary<String, List<String>> word_list;
    private List<Client> registeredUsers;
    private List<String> loggedUsers = new ArrayList<>();
    private ServerSocketChannel serverSocketChannel;
    private static final int maxPlayers = 5;

    public Server(int port, int mode, String host){
       this.port = port;
       this.mode = mode;
       this.word_list = readWordList();
       this.registeredUsers = readUsers();
    }

    private Dictionary<String, List<String>> readWordList(){
        Dictionary<String, List<String>> wordList = new Hashtable<>();
        String line = "";

        try (BufferedReader br = new BufferedReader(new FileReader("src\\main\\java\\fe\\up\\pt\\word_list.csv"))) {
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first){
                    first = false;
                    continue;
                }
                // use comma as separator
                String[] data = line.split(",");
                String theme = data[0];
                String word = data[1];
                List<String> words = wordList.get(theme);

                if (words == null) {
                    words = new ArrayList<>();
                    wordList.put(theme, words);
                }
                else {
                    words.add(word);
                }

            }
        } catch (IOException ignored) {
        }

        return wordList;
    }

    private List<Client> readUsers(){
        List<Client> users = new ArrayList<>();
        String line = "";

        try (BufferedReader br = new BufferedReader(new FileReader("src\\main\\java\\fe\\up\\pt\\users.csv"))) {
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first){
                    first = false;
                    continue;
                }
                // use comma as separator
                String[] data = line.split(",");
                String username = data[0];
                String password = data[1];
                int rank = Integer.parseInt(data[2]);
                users.add(new Client(username, password,"" ,rank));
            }
        } catch (IOException ignored) {
        }

        return users;
    }

    public void start() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(this.port));
        System.out.println("Server started on mode " + this.mode + " on port " + this.port);
    }
    private String readMessage(SocketChannel client) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        client.read(buffer);
        buffer.flip();
        ByteBuffer validBuffer = ByteBuffer.allocate(buffer.remaining());
        validBuffer.put(buffer);
        validBuffer.flip();

        return new String(validBuffer.array(), StandardCharsets.UTF_8);
    }

    private void sendMessage(SocketChannel client, String message) throws IOException{
        client.write(ByteBuffer.wrap(message.getBytes()));
    }

    private Client loginUser(String username, String password) {
        for (Client user: this.registeredUsers){
            if (user.getUsername().equals(username) && user.getPassword().equals(password)){
                System.out.println("User " + username + " logged in!");
                String token = user.getToken();

                if (token.isEmpty()) user.setToken(UUID.randomUUID().toString());
                if (!this.loggedUsers.contains(token)) this.loggedUsers.add(token);

                return user;
            }
        }
        return null;
    }

    private Client registerUser(String username, String password) {
        for (Client user: this.registeredUsers){
            if (user.getUsername().equals(username)){
                return null;
            }
        }
        String token = UUID.randomUUID().toString();
        Client newUser = new Client(username, password, token,1000);
        try {
            // Open the file
            FileWriter fileWriter = new FileWriter("src\\main\\java\\fe\\up\\pt\\users.csv", true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(newUser.getUsername() + "," + newUser.getPassword() + ","  + newUser.getRank() + "\n");
            bufferedWriter.close();
        } catch (IOException ignored) {
            return null;
        }
        this.registeredUsers.add(newUser);
        this.loggedUsers.add(token);

        return newUser;
    }

    private boolean logoutUser(String token) {
        for (String loggedToken: this.loggedUsers){
            if (loggedToken.equals(token)){
                this.loggedUsers.remove(token);
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(12345, 1, "localhost");
        server.start();
        boolean run = true;

        //TODO: seperate this loop on threads
        while (run){
            SocketChannel client = server.serverSocketChannel.accept();
            while (run) {
                String message = server.readMessage(client);
                switch (message.split(":")[0].trim()) {
                    case "LGN":
                        System.out.println("Login request received!");

                        Client user = server.loginUser(message.split(":")[1], message.split(":")[2]);
                        if (user != null) {
                            server.sendMessage(client, "SUC:" + user.getToken());
                        } else {
                            server.sendMessage(client, "ERR");
                        }

                        break;
                    case "REG":
                        System.out.println("Register request received!");

                        Client newUser = server.registerUser(message.split(":")[1], message.split(":")[2]);
                        if (newUser != null) {
                            server.sendMessage(client, "SUC:" + newUser.getToken());
                        } else {
                            server.sendMessage(client, "ERR");
                        }

                        break;
                    case "OUT":
                        System.out.println("Logout request received!");

                        if (server.logoutUser(message.split(":")[1])) {
                            server.sendMessage(client, "SUC");
                        } else {
                            server.sendMessage(client, "ERR");
                        }
                        
                        if (server.loggedUsers.isEmpty()) run = false;
                        System.out.println("Server shutting down!");
                        break;
                    default:
                        System.out.println("Unknown request received!");
                        break;
                }

            }
        }
        server.serverSocketChannel.close();
    }
}
