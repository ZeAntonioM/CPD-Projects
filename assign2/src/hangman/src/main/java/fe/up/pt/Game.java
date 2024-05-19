package fe.up.pt;

import java.io.*;
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
    private int queueHead = 0;
    private int queueTail = 0;

    // Game
    private final String host;
    private int port;
    private List<User> players = new ArrayList<User>();
    private int numPlayers;
    private boolean ranked;
    private final String theme;
    private final String word;
    private boolean running;
    private String guessedWord;
    private HashMap<String, User> gameUsers;
    private List<Integer> ranks = new ArrayList<Integer>();

    public Game(int port, String host, HashMap<String, User> gameUsers, boolean ranked, String theme, String word) {

        this.port = port;
        this.host = host;

        this.gameUsers = gameUsers;
        this.ranked = ranked;
        this.theme = theme;
        this.word = word;
        this.guessedWord = this.word.replaceAll("[a-zA-Z]", "_");

        this.running = true;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void writeMessage(PrintWriter printWriter, String message) throws IOException {
        printWriter.println(message);
        printWriter.flush();
    }

    public String[] readMessage(BufferedReader bufferedReader) throws IOException {
        return bufferedReader.readLine().split(":");
    }

    private void sendGameMessage(String message, User player) {

        try {
            message = "GAM:" + message + ":" + player.getActiveToken();
            PrintWriter printWriter = new PrintWriter(player.getSocket().getOutputStream(), true);
            writeMessage(printWriter, message);
        }
        catch (IOException e) {
            System.out.println("Error sending game message: " + e.getMessage());
        }


    }

    private void sendGameMessageAll(String message) {
        for (User player : this.players) {
            sendGameMessage(message, player);
        }
    }


    public void run() {
        try {
            this.waitForPlayers();
            if (players.isEmpty()) {
                System.out.println("No players joined the game!");
                this.running = false;
                return;
            }
            System.out.println("Game started!");
            this.start();
            this.gameLoop();
            this.end();

        }
        catch (IOException e) {
            System.out.println("Error on waiting for players: " + e.getMessage());
        }
    }

    private void waitForPlayers() throws IOException {
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
            System.out.println("Timeout reached! No More Players can join");
        }

    }

    private void start() {
        shuffle(this.players);

        for (User player : this.players) {
            this.ranks.add(player.getRank());
        }

        sendGameMessageAll("start:" + this.theme + ":" + this.guessedWord);

    }

    private void gameLoop() {
        while (this.running) {
            for (User player : this.players) {
                sendGameMessage("yourTurn", player);
                // receive message
            }
        }
    }

    private void end() {

        sendGameMessageAll("end");

    }

    private void treatGuess(String guess, User player) {

        guess = guess.toLowerCase();

        if ((guess.length() == word.length())) {

            if (guess.equals(word)) {
                if (ranked) this.updateRank(player, 20);

                this.running = false;
                this.sendGameMessageAll("correctGuess:" + guess + ":" + this.word + ":" + player.getUsername());
            }
            else {
                if (ranked) this.updateRank(player, -10);

                this.sendGameMessageAll("wrongGuess:" + guess + ":" + this.guessedWord + ":" + player.getUsername());
            }

        }
        else {

            if (word.contains(guess)) {
                if (ranked) this.updateRank(player, 5);

                this.updateGuessedWord(guess);
                if (this.guessedWord.equals(this.word)) this.running = false;

                this.sendGameMessageAll("correctGuess:" + guess + ":" + this.guessedWord + ":" + player.getUsername());
            }
            else {
                if (ranked) this.updateRank(player, -3);

                this.sendGameMessageAll("wrongGuess:" + guess + ":" + this.guessedWord + ":" + player.getUsername());
            }

        }

    }

    public void updateRank(User player, int points) {
            int playerNumber = this.players.indexOf(player);
            this.ranks.set(playerNumber, this.ranks.get(playerNumber) + points);
    }

    public void updateGuessedWord(String letter) {
        for (int i = 0; i < this.word.length(); i++) {
            if (this.word.charAt(i) == letter.charAt(0)) {
                this.guessedWord = this.guessedWord.substring(0, i) + letter + this.guessedWord.substring(i + 1);
            }
        }
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
                        treatGuess(clientMessage[1], gameUsers.get(clientMessage[2]));
                        break;
                    default:
                        System.out.println("Unknown request received!: " + messageKey);
                        //writeMessage(printWriter, "ERR:Unknown request!");
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
