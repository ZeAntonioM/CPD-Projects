package fe.up.pt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Time;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CountDownLatch;

import static java.util.Collections.shuffle;

public class Game {

    //Server side
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition notEmpty = queueLock.newCondition();
    private final Queue<Socket> clientQueue = new Queue<>();

    private final HashMap<String, User> gameUsers;
    private int queueHead = 0;
    private int queueTail = 0;


    // Game
    private final String host;
    private int port;
    public HashMap<String, User> activeUsers = new HashMap<String, User>();
    private List<User> players = new ArrayList<User>();
    private int numPlayers;
    private boolean ranked;
    private String theme;
    private String word;
    private boolean running;
    private String guessedWord;
    private List<Integer> ranks = new ArrayList<Integer>();

    public Game(int port, String host, HashMap<String, User> users, boolean ranked, String theme, String word) {

        this.port = port;
        this.host = host;

        this.activeUsers = users;
        this.gameUsers = getUserTokens();
        this.ranked = ranked;
        this.theme = theme;
        this.word = word;
        this.guessedWord = this.word.replaceAll("[a-zA-Z]", "_");

        this.running = true;
    }

    public HashMap<String, User> getUserTokens() {
        HashMap<String, User> userTokens = new HashMap<String, User>();
        for (User user : this.activeUsers.values()) {
            userTokens.put(user.getActiveToken(), user);
        }
        return userTokens;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void run() {
        try {
            this.wait_for_players();
        } catch (IOException e) {
            System.out.println("Error waiting for players: " + e.getMessage());
        }
        /*this.start();
        while (this.running) {
            for (User player : this.players) {
                this.sendGameMessage(player, "yourTurn:" + this.guessedWord);
            }
        }
        this.end();*/
    }

    private void sendGameMessage(User player, String tokens) {
        try {
            PrintWriter printWriter = new PrintWriter(player.getSocket().getOutputStream(), true);
            writeMessage(printWriter, "GAM" + ":" + tokens + ":" + player.getActiveToken());
        } catch (IOException e) {
            System.out.println("Error sending game message: " + e.getMessage());
        }
    }

    public void wait_for_players() throws IOException {
        System.out.println("Waiting for players to join the game...");

        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            long endTime = System.currentTimeMillis() + 10000;

            while (System.currentTimeMillis() < endTime) {
                serverSocket.setSoTimeout((int) (endTime - System.currentTimeMillis())); //Timeout of 2 seconds
                    // Wait for a new connection
                    Socket clientSocket = serverSocket.accept();

                    // Acquire lock and add client to queue
                    clientQueue.enqueue(clientSocket);
                    Thread.ofVirtual().start(new ClientHandler());

            }

            System.out.println("Game started!");

        }
        catch (SocketTimeoutException e) {
            System.out.println("Timeout reached! Game starting...");
            this.start();
            while (this.running) {
                for (User player : this.players) {
                    this.sendGameMessage(player, "yourTurn:" + this.guessedWord);
                }
            }
            this.end();
        }


    }

    public void start() {

        this.numPlayers = players.size();
        shuffle(this.players);


        for (User player : this.players) {
            this.ranks.add(player.getRank());
        }

        for (User player : this.players) {
            this.sendGameMessage(player, "start:" + this.theme + ":" + this.guessedWord);
        }
    }

    public void end() {
        for (User player : this.players) {
            this.sendGameMessage(player, "end:" + this.word);
        }
    }

    public void receiveGuess(String guess, User player) {
        if ((guess.length() > 1) && (guess.equals(word))) {
            if (this.ranked) this.updateRank(player, 20);

            this.guessedWord = this.word;
            this.running = false;
            for (User p : this.players) this.sendGameMessage(p, "correctGuess:" + guess + ":" + this.guessedWord + ":" + player.getUsername());

        }
        else if (guess.length() > 1) {
            if (this.ranked) this.updateRank(player, -10);

            for (User p : this.players) this.sendGameMessage(p, "wrongGuess:" + guess + ":" + player.getUsername());
        }
        else if (this.checkLetter(guess)){
            if (this.ranked) this.updateRank(player, 5);

            this.updateGuessedWord(guess);
            for (User p : this.players) this.sendGameMessage(p, "correctGuess:" + guess + ":" + this.guessedWord + ":" + player.getUsername());
        }
        else {
            if (this.ranked) this.updateRank(player, -3);

            for (User p : this.players) this.sendGameMessage(p, "wrongGuess:" + guess + ":" + player.getUsername());
        }
    }

    private void updateRank(User player, int points) {
        int playerNumber = this.players.indexOf(player);
        this.ranks.set(playerNumber, this.ranks.get(playerNumber) + points);
    }

    private boolean checkLetter(String letter) {
        return this.word.contains(letter);
    }

    private void updateGuessedWord(String letter) {
        for (int i = 0; i < this.word.length(); i++) {
            if (this.word.charAt(i) == letter.charAt(0)) {
                this.guessedWord = this.guessedWord.substring(0, i) + letter + this.guessedWord.substring(i + 1);
            }
        }
    }

    public void writeMessage(PrintWriter printWriter, String message) throws IOException {
        printWriter.println(message);
        printWriter.flush();
    }

    public String[] readMessage(BufferedReader bufferedReader) throws IOException {
        return bufferedReader.readLine().split(":");
    }

    private class ClientHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                Socket clientSocket = clientQueue.dequeue();
                if (clientSocket == null) {
                    continue;
                }

                while (handleClientData(clientSocket)) ;

                try {
                    clientSocket.close();
                } catch (IOException e) {
                }
            }
        }

        private boolean handleClientData(Socket clientSocket) {

            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());

                String[] clientMessage = readMessage(bufferedReader);
                String messageKey = clientMessage[0];

                switch (messageKey) {
                    case "GIN":
                        players.add(gameUsers.get(clientMessage[1]));
                        gameUsers.get(clientMessage[1]).setSocket(clientSocket);
                        System.out.println("Player " + gameUsers.get(clientMessage[1]).getUsername() + " joined the game!");
                        writeMessage(printWriter, "GAM:wait");
                        break;
                    case "GGS":
                        receiveGuess(clientMessage[1], gameUsers.get(clientMessage[2]));
                        break;
                    default:
                        System.out.println("Unknown request received!: " + messageKey);
                        writeMessage(printWriter, "ERR:Unknown request!");
                        break;
                }

            } catch (IOException e) {
                System.out.println("Error while handling client data: " + e.getMessage());
                return false;
            }
            return true;
        }


        private boolean isEmpty() {
            return queueHead == queueTail;
        }

    }


}
