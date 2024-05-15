package fe.up.pt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
    private List<User> allUsers = readUsers();
    private List<String> activeUsers = new ArrayList<>();

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
                new Thread(new ClientHandler()).start();
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

    private List<User> readUsers() {
        List<User> users = new ArrayList<>();
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
                users.add(new User(username, password, "", rank, null));
            }
        } catch (IOException ignored) {
        }

        return users;
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
                User client = null;

                switch (messageKey) {
                    case "LGN":
                        client = clientLogin(clientMessage[1], clientMessage[2], clientSocket);
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + client.getToken());
                        } else {
                            writeMessage(printWriter, "ERR:Invalid credentials!");
                        }
                        break;
                    case "REG":
                        client = clientRegister(clientMessage[1], clientMessage[2], clientSocket);
                        if (client != null) {
                            writeMessage(printWriter, "SUC:" + client.getToken());
                        } else {
                            writeMessage(printWriter, "ERR:Username already exists!");
                        }
                        break;
                    case "LGO":
                        if (clientLogout(clientMessage[1])){
                            writeMessage(printWriter, "SUC:Logged out successfully!");
                            return false;
                        } else {
                            writeMessage(printWriter, "ERR:Invalid token or user is not logged in!");
                        }
                        break;
                    default:
                        System.out.println("Unknown request received!: " + messageKey);
                        writeMessage(printWriter, "ERR:Unknown request!");
                        break;
                }

//                String outputMessage = "Hello from the server!";
 //               outputStream.write(outputMessage.getBytes(), 0, outputMessage.getBytes().length);
            } catch (IOException e) {
                System.out.println("Error while handling client data: " + e.getMessage());
                return false;
            }
            return true;
        }

        private boolean clientLogout(String token) {
            for (String userToken: activeUsers) {
                if (userToken.equals(token)) {
                    activeUsers.remove(token);
                    return true;
                }
            }
            return false;
        }

        private User clientRegister(String username, String password, Socket userSocket) {
            for (User user : allUsers) {
                if (user.getUsername().equals(username)) {
                    return null;
                }
            }
            String token = UUID.randomUUID().toString();
            User newUser = new User(username, password, token, 1000, userSocket);
            try {
                // Open the file
                FileWriter fileWriter = new FileWriter("src\\main\\java\\fe\\up\\pt\\users.csv", true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(newUser.getUsername() + "," + newUser.getPassword() + "," + newUser.getRank() + "\n");
                bufferedWriter.close();
            } catch (IOException ignored) {
                return null;
            }
            allUsers.add(newUser);
            activeUsers.add(token);

            return newUser;
        }

        private User clientLogin(String username, String password, Socket userSocket) {
            for (User user : allUsers) {
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                    System.out.println("User " + username + " logged in!");
                    String token = user.getToken();

                    if (token.isEmpty()) user.setToken(UUID.randomUUID().toString());
                    if (!activeUsers.contains(token)) activeUsers.add(token);

                    user.setSocket(userSocket);

                    return user;
                }
            }
            return null;
        }

        private boolean isEmpty() {
            return queueHead == queueTail;
        }
    }
}