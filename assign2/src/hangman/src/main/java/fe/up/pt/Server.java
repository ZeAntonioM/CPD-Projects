package fe.up.pt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.mindrot.jbcrypt.BCrypt;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

        try (BufferedReader br = new BufferedReader(new FileReader("src\\main\\java\\fe\\up\\pt\\users.csv"))) {
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
                            writeMessage(printWriter, "SUC");
                            return false;
                        } else {
                            writeMessage(printWriter, "ERR:Invalid token or user is not logged in!");
                        }
                        break;
                    case "GAM":
                        System.out.println(clientMessage[1] + " of type " + clientMessage[2] + " for token " + clientMessage[3]);
                        writeMessage(printWriter, "SUC");
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

        private boolean clientLogout(String token) {
            if (!validateRequest(token)) return false;
            for (User user : activeUsers.values()) {
                for (String userToken : user.getTokens()) {
                    if (userToken.equals(token)) {
                        activeUsers.remove(token);
                        System.out.println("User " + user.getUsername() + " logged out!");
                        return true;
                    }
                }
            }
            return false;
        }

        private synchronized User clientRegister(String username, String password, Socket userSocket, StringBuilder retToken) {
            User user = allUsers.get(username);
            if (user != null) return null;

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            String token = UUID.randomUUID().toString();
            User newUser = new User(username, hashedPassword, token, 1000, userSocket);
            newUser.addToken(token);
            try {
                // Open the file
                FileWriter fileWriter = new FileWriter("src\\main\\java\\fe\\up\\pt\\users.csv", true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(newUser.getUsername() + "," + newUser.getPassword() + "," + newUser.getRank() + "\n");
                bufferedWriter.close();
            } catch (IOException ignored) {
                return null;
            }
            allUsers.put(username, newUser);
            activeUsers.put(username, newUser);

            retToken.append(token);
            System.out.println("User " + username + " registered!");
            return newUser;
        }

        private synchronized User clientLogin(String username, String password, Socket userSocket, StringBuilder retToken) {
            User user = allUsers.get(username);
            if (user != null && BCrypt.checkpw(password, user.getPassword())) {
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

        private boolean isEmpty() {
            return queueHead == queueTail;
        }
    }

}