package fe.up.pt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class Server {
    private final int port;
    private final int mode;
    private Dictionary<String, List<String>> word_list;
    private ServerSocketChannel serverSocketChannel;
    private static final int maxPlayers = 5;

    public Server(int port, int mode, String host){
       this.port = port;
       this.mode = mode;
       this.word_list = readWordList();
    }

    private Dictionary<String, List<String>> readWordList(){
        Dictionary<String, List<String>> wordList = new Hashtable<>();
        String line = "";

        try (BufferedReader br = new BufferedReader(new FileReader("src\\main\\java\\fe\\up\\pt\\word_list.csv"))) {
            while ((line = br.readLine()) != null) {
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
            SocketChannel client = server.serverSocketChannel.accept();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            client.read(buffer);
            buffer.flip();
            ByteBuffer validBuffer = ByteBuffer.allocate(buffer.remaining());
            validBuffer.put(buffer);
            validBuffer.flip();

            String message = new String(validBuffer.array(), StandardCharsets.UTF_8);
            switch (message.split(":")[0].trim()) {
                case "LGN":
                    System.out.println("Login request received!");
                    System.out.println("Username: " + message.split(":")[1]);
                    System.out.println("Password: " + message.split(":")[2]);
                    break;
                case "REG":
                    System.out.println("Register request received!");
                    System.out.println("Username: " + message.split(":")[1]);
                    System.out.println("Password: " + message.split(":")[2]);
                    break;
                case "OUT":
                    System.out.println("Logout request received!");
                    System.out.println("Token: " + message.split(":")[1]);
                    run = false;
                    break;
                default:
                    System.out.println("Unknown request received!");
                    break;
            }
            // respond to client
            client.write(ByteBuffer.wrap("Hello client!".getBytes()));

        }
        server.serverSocketChannel.close();
    }
}
