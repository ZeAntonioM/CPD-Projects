package fe.up.pt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Time;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
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
    private CyclicBarrier barrier;

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



    public void writeMessage(Socket clientSocket, String message) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);

        printWriter.println(message);
        printWriter.flush();
    }

    public String[] readMessage(Socket clientSocket) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String message = bufferedReader.readLine();

        return message.split(":");

    }

    private void sendGameMessage(String message, User player) {

        try {
            message = "GAM:" + message;
            writeMessage(player.getSocket(), message);
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

        int i = 0;
        for (User player : this.players) {
            this.ranks.add(player.getRank());
            i++;
        }

        this.barrier = new CyclicBarrier(i + 1);
        sendGameMessageAll("start:" + this.theme + ":" + this.guessedWord);

    }

    private void gameLoop() throws IOException {
        int i = 0;
        while (this.running) {
            User player = this.players.get(i);
            sendGameMessageAll("turn:" + player.getActiveToken());
            try {
                barrier.await();
            } catch (Exception e) {
                System.out.println("Error on barrier: " + e.getMessage());
            }
            i = (i + 1) % this.players.size();
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
                String[] clientMessage = readMessage(clientSocket);
                String messageKey = clientMessage[0];
                System.out.println("Received Game: " + Arrays.toString(clientMessage));

                switch (messageKey) {
                    case "GIN":
                        players.add(gameUsers.get(clientMessage[1]));
                        gameUsers.get(clientMessage[1]).setSocket(clientSocket);
                        System.out.println("Player " + gameUsers.get(clientMessage[1]).getUsername() + " joined the game!");
                        writeMessage(clientSocket, "GAM:wait");
                        break;
                    case "GGS":
                        treatGuess(clientMessage[1], gameUsers.get(clientMessage[2]));
                        try {
                            barrier.await();
                        } catch (Exception e) {
                            System.out.println("Error on barrier: " + e.getMessage());
                        }
                        break;
                    case "GWA":
                        try {
                            barrier.await();
                        } catch (Exception e) {
                            System.out.println("Error on barrier: " + e.getMessage());
                        }
                        break;
                    default:
                        System.out.println("GAME: Unknown request received!: " + messageKey);
                        writeMessage(clientSocket, "ERR:Unknown request!");
                        break;
                }

            } catch (IOException e) {
                System.out.println("Error while handling client data: " + e.getMessage());
                return false;
            }
            return true;
        }
    }
}
