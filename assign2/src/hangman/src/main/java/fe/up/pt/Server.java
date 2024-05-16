package fe.up.pt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition notEmpty = queueLock.newCondition();
    private final Socket[] clientQueue = new Socket[10];
    private int queueHead = 0;
    private int queueTail = 0;
    private final String host;
    private int port = 12345;
    private HashMap<String, User> allUsers = readUsers();
    private final HashMap<String, User> activeUsers = new HashMap<>();
    private List<Game> activeGames = new ArrayList<>();
    private int gameID = 0;

    public Server(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(12345, "localhost");
        server.start();
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(this.port);

        // Start the dedicated client handling thread

        System.out.println("Server started on port " + this.port + "!");
        while (true) {
            // Wait for a new connection
            Socket clientSocket = serverSocket.accept();

            // Acquire lock and add client to queue
            queueLock.lock();
            try {
                clientQueue[queueTail] = clientSocket;
                queueTail = (queueTail + 1) % clientQueue.length; // Wrap-around logic
                Thread.ofVirtual().start(new ClientHandler());
                notEmpty.signal(); // Signal the waiting client handling thread
            } finally {
                queueLock.unlock();
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

    private HashMap<String, User> readUsers() {
        HashMap<String, User> users = new HashMap<>();
        String line = "";

        try (BufferedReader br = new BufferedReader(new FileReader("src"+File.separator+"main"+File.separator+"java"+File.separator+"fe"+File.separator+"up"+File.separator+"pt"+File.separator+"users.csv"))) {
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                // use comma as separator
                String[] data = line.split(",");
                String username = data[0];
                String password = data[1];
                int rank = Integer.parseInt(data[2]);
                users.put(username, new User(username, password, "", rank, null));
            }
        } catch (IOException ignored) {
        }

        return users;
    }

    private void runGame(List<User> players, boolean ranked, String theme, String word) {

        Game game = new Game(gameID++, players, ranked, theme, word);

        activeGames.add(game);

        game.start();

        while (game.isRunning()) {
            for (int i = 0; i < game.getNumPlayers(); i++) {
                User player = game.getPlayers().get(i);
                ClientHandler connection = new ClientHandler();

                game.sendAskForGuess(player);

                connection.handleClientData(player.getSocket());
            }
        }

        game.end();
        activeGames.remove(game);
    }

    private synchronized boolean validateRequest(String token) {
        boolean valid = false;
        for (User user : activeUsers.values()) {
            valid = valid || user.setActiveToken(token);
        }
        return valid;
    }

    private class ClientHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                Socket clientSocket = null;

                // Acquire lock and remove client from queue
                queueLock.lock();
                try {
                    while (isEmpty()) {
                        notEmpty.await(); // Wait if the queue is empty
                    }
                    clientSocket = clientQueue[queueHead];
                    queueHead = (queueHead + 1) % clientQueue.length;
                } catch (InterruptedException e) {
                } finally {
                    queueLock.unlock();
                }

                while(handleClientData(clientSocket));

                try {
                    clientSocket.close();
                } catch (IOException e) {
                }
            }
        }

        private boolean handleClientData(Socket clientSocket) {
            try {BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());

                String[] clientMessage = readMessage(bufferedReader);
                String messageKey = clientMessage[0];
                StringBuilder token = new StringBuilder();
                User client = null;

                switch (messageKey) {
                    case "LGN":
                        client = clientLogin(clientMessage[1], clientMessage[2], clientSocket, token);
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + token);
                        } else {
                            writeMessage(printWriter, "ERR:Invalid credentials!");
                        }
                        break;
                    case "REG":
                        client = clientRegister(clientMessage[1], clientMessage[2], clientSocket, token);
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + token);
                        } else {
                            writeMessage(printWriter, "ERR:Username already exists!");
                        }
                        break;
                    case "LGO":
                        if (clientLogout(clientMessage[1])){
                            writeMessage(printWriter, "SUC:Logged out successfully!");
                        } else {
                            writeMessage(printWriter, "ERR:Invalid token or user is not logged in!");
                        }
                        return false;
                    // Stands for Join Simple Game
                    case "JSG":
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + client.getActiveToken());
                        } else {
                            writeMessage(printWriter, "ERR:Could not join game!");
                        }
                    // Stands for Join Ranked Game
                    case "JRG":
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + client.getActiveToken());
                        } else {
                            writeMessage(printWriter, "ERR:Could not join game!");
                        }
                    // Stands for Create Simple Game
                    case "CSG":
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + client.getActiveToken());
                        } else {
                            writeMessage(printWriter, "ERR: Could not create Game!");
                        }
                    // Stands for Create Ranked Game
                    case "CRG":
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + client.getActiveToken());
                        } else {
                            writeMessage(printWriter, "ERR: Could not create Game!");
                        }

                    // Stands for Game: Start
                    case "GST":
                        //user = clientGuess(clientMessage[1], clientMessage[2]);
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + client.getActiveToken());
                        } else {
                            writeMessage(printWriter, "ERR:Cannot Start Game!");
                        }
                    // Stands for Game: Send Guess
                    case "GSG":
                        //user = clientGuess(clientMessage[1], clientMessage[2]);
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + client.getActiveToken());
                        } else {
                            writeMessage(printWriter, "ERR:Cannot Send Guess!");
                        }
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

        private boolean clientLogout(String token) {
            if (!validateRequest(token)) return false;
            for (User user : activeUsers.values()) {
                for (String userToken : user.getTokens()) {
                    if (userToken.equals(token)) {
                        activeUsers.remove(token);
                        return true;
                    }
                }
            }
            return false;
        }

        private synchronized User clientRegister(String username, String password, Socket userSocket, StringBuilder retToken) {
            User user = allUsers.get(username);
            if (user != null) return null;

            String token = UUID.randomUUID().toString();
            User newUser = new User(username, password, token, 1000, userSocket);
            try {
                // Open the file
                FileWriter fileWriter = new FileWriter("src"+File.separator+"main"+File.separator+"java"+File.separator+"fe"+File.separator+"up"+File.separator+"pt"+File.separator+"users.csv", true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(newUser.getUsername() + "," + newUser.getPassword() + "," + newUser.getRank() + "\n");
                bufferedWriter.close();
            } catch (IOException ignored) {
                return null;
            }
            allUsers.put(username, newUser);
            activeUsers.put(username, newUser);

            retToken.append(token);
            return newUser;
        }


        private synchronized User clientLogin(String username, String password, Socket userSocket, StringBuilder retToken) {
            User user = allUsers.get(username);
            if (user != null && user.getPassword().equals(password)) {

                System.out.println("User " + username + " logged in!");
                String token = UUID.randomUUID().toString();

                if (!user.addToken(token)) return null;

                activeUsers.putIfAbsent(username, user);

                user.setSocket(userSocket);

                retToken.append(token);
                return user;
            }

            return null;
        }

        private User clientGuess(String token, String guess) {
            
            return null;
        }

        private boolean isEmpty() {
            return queueHead == queueTail;
        }
    }
}