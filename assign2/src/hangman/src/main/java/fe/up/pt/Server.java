package fe.up.pt;

import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final Queue<Socket> clientQueue = new Queue<>();
    private final ReentrantLock accountLock = new ReentrantLock();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final String host;
    private int port = 12345;
    private HashMap<String, User> allUsers = readUsers();
    private final HashMap<String, User> activeUsers = new HashMap<>();
    private final Dictionary<String, List<String>> dictionaryWords = readWordList();
    private List<Game> activeGames = new ArrayList<>();
    private int gameID = 0;
    private List<UserQueue> userQueues = new ArrayList<>();
    private int basePort = 12346;
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
        Thread.ofVirtual().start(new QueueDispacher(true));
        while (true) {
            // Wait for a new connection
            Socket clientSocket = serverSocket.accept();

            // Acquire lock and add client to queue
            clientQueue.enqueue(clientSocket);
            Thread.ofVirtual().start(new ClientHandler());
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
                users.put(username, new User(username, password, null, rank, null));
            }
        } catch (IOException ignored) {
        }

        return users;
    }

    private Dictionary<String, List<String>> readWordList(){
        Dictionary<String, List<String>> wordList = new Hashtable<>();
        String line = "";

        try (BufferedReader br = new BufferedReader(new FileReader("src"+File.separator+"main"+File.separator+"java"+File.separator+"fe"+File.separator+"up"+File.separator+"pt"+File.separator+"word_list.csv"))) {
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

                String themeLower = theme.toLowerCase();
                String wordLower = word.toLowerCase();

                if (words == null) {
                    words = new ArrayList<>();
                    wordList.put(themeLower, words);
                }
                else {
                    words.add(wordLower);
                }

            }
        } catch (IOException ignored) {
        }

        return wordList;
    }
    private String[] getRandomThemeWord() {
        List<String> keys = Collections.list(dictionaryWords.keys());
        Random random = new Random();

        // Get a random key
        int randomKeyIndex = random.nextInt(keys.size());
        String randomKey = keys.get(randomKeyIndex);

        // Get a random value from the key
        List<String> values = dictionaryWords.get(randomKey);
        int randomValueIndex = random.nextInt(values.size());
        String randomValue = values.get(randomValueIndex);

        return new String[] {randomKey, randomValue};
    }

    private boolean validateRequest(String token) {
        try {
            accountLock.lock();
            boolean valid = false;
            for (User user : activeUsers.values()) {
                valid = valid || user.setActiveToken(token);
            }
            return valid;
        } finally {
            accountLock.unlock();
        }
    }

    private User getUserFromToken(String token) {
        try {
            accountLock.lock();
            for (User user : activeUsers.values()) {
                for (String userToken : user.getTokens()) {
                    if (userToken != null && userToken.equals(token)) {
                        return user;
                    }
                }
            }
            return null;
        } finally {
            accountLock.unlock();
        }
    }

    private void sortUserQueues(List<UserQueue> userQueues, int num) {
        userQueues.sort(Comparator.comparingInt(o -> Math.abs(o.getRank() - num)));
    }

    private void createQueue(User user, boolean ranked) {
        try{
            queueLock.lock();
            UserQueue userQueue = new UserQueue(ranked, user.getRank());
            userQueue.enqueue(user);
            userQueues.add(userQueue);
            userQueue.start();
        } catch (Exception e) {
            System.out.println("Error while creating queue: " + e.getMessage());
        } finally {
            queueLock.unlock();
        }
    }

    private boolean joinQueue(User user, UserQueue queue) {
        try{
            queueLock.lock();
            if (queue.ended) return false;
            queue.enqueue(user);
            queue.setMeanRank();
            System.out.println("User " + user.getUsername() + " joined queue!");
            System.out.println("Queue: " + queue.queue.toString());
            return true;
        } finally {
            queueLock.unlock();
        }
    }


    private class ClientHandler implements Runnable {
        private boolean inGame = false;
        @Override
        public void run() {
            while (true) {
                Socket clientSocket = clientQueue.dequeue();
                if (clientSocket == null) {
                    continue;
                }

                while (handleClientData(clientSocket));

                if (inGame){ //if client in game, lets not close socket and wait for the client to return from game
                    while(inGame);
                    continue;
                }

                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error while closing client socket: " + e.getMessage());
                }
            }
        }

        private boolean handleClientData(Socket clientSocket) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());


                String[] clientMessage = readMessage(bufferedReader);
                String messageKey = clientMessage[0];
                StringBuilder token = new StringBuilder();
                User client = null;
                System.out.println("Received: " + Arrays.toString(clientMessage));

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
                    case "EXT":
                        writeMessage(printWriter, "SUC");
                        return false;
                    case "LGO":
                        if (clientLogout(clientMessage[1])) {
                            writeMessage(printWriter, "SUC");
                        } else {
                            writeMessage(printWriter, "ERR:Invalid token or user is not logged in!");
                        }
                        break;
                    case "GAM":
                        if (!validateRequest(clientMessage[3])) {
                            writeMessage(printWriter, "ERR:Invalid token or user is not logged in!");
                            break;
                        }

                        User user = getUserFromToken(clientMessage[3]);
                        boolean result;

                        if (clientMessage[1].equals("create")) {
                            System.out.println("Creating game!");
                            createQueue(user, clientMessage[2].equals("rank"));
                            System.out.println("Game created!");
                            result = true;
                        } else result = clientSearchGame(user, clientMessage[2].equals("rank"), 50);
                        this.inGame = result;
                        writeMessage(printWriter, result ? "SUC" : "ERR:No games found!");
                        return !result;
                    case "GGS":
                        System.out.println("WTF");
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
            try {
                accountLock.lock();
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
            } finally {
                accountLock.unlock();
            }
        }

        private User clientRegister(String username, String password, Socket userSocket, StringBuilder retToken) {
            try {
                accountLock.lock();

                User user = allUsers.get(username);
                if (user != null) return null;

                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                String token = UUID.randomUUID().toString();
                User newUser = new User(username, hashedPassword, token, 1000, userSocket);
                newUser.addToken(token);
                // Open the file
                FileWriter fileWriter = new FileWriter("src"+File.separator+"main"+File.separator+"java"+File.separator+"fe"+File.separator+"up"+File.separator+"pt"+File.separator+"users.csv", true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(newUser.getUsername() + "," + newUser.getPassword() + "," + newUser.getRank() + "\n");
                bufferedWriter.close();


                allUsers.put(username, newUser);
                activeUsers.put(username, newUser);

                retToken.append(token);
                System.out.println("User " + username + " registered!");
                return newUser;
            } catch (IOException e) {
                return null;
            } finally {
                accountLock.unlock();
            }
        }

        private User clientLogin(String username, String password, Socket userSocket, StringBuilder retToken) {
            try {
                accountLock.lock();
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
            } finally {
                accountLock.unlock();
            }
        }
    private boolean clientSearchGame(User user, boolean ranked, int range) {
        if (ranked) {
            long endTime = System.currentTimeMillis() + 120000; //two minutes
            long interval = System.currentTimeMillis() + 10000; //ten seconds

            while (System.currentTimeMillis() < endTime) {
                var copyOfUserQueues = new ArrayList<>(userQueues);
                sortUserQueues(copyOfUserQueues, user.getRank());
                for (UserQueue userQueue : copyOfUserQueues) {
                    int difference = Math.abs(userQueue.getRank() - user.getRank());
                    if (difference > range) break;

                    if (userQueue.isRanked()) {
                        return joinQueue(user, userQueue);
                    }

                }

                if (System.currentTimeMillis() > interval){
                    range += 50;
                    interval = System.currentTimeMillis() + 10000;
                    System.out.println("Range: " + range);
                }

                copyOfUserQueues = null; //setting for garbage collection
            }
        } else {
            long endTime = System.currentTimeMillis() + 120000; //two minutes

            while (System.currentTimeMillis() < endTime) {
                for (UserQueue userQueue : userQueues) {
                    if (!userQueue.isRanked()) {
                        return joinQueue(user, userQueue);
                    }
                }
            }
        }
        return false;
    }
    }

    public HashMap<String, User> getUserTokens() {
        HashMap<String, User> userTokens = new HashMap<String, User>();
        for (User user : this.activeUsers.values()) {
            userTokens.put(user.getActiveToken(), user);
        }
        return userTokens;
    }

    private class QueueDispacher implements Runnable {
        private boolean ranked;
        public QueueDispacher(boolean ranked){
            this.ranked = ranked;
        }
        @Override
        public void run() {
            System.out.println("Queue dispatcher started!");
            while (true) {
                var localUserQueues = new ArrayList<>(userQueues);
                for (UserQueue userQueue : localUserQueues) {
                    if (userQueue.ended) {
                        String[] themeWord = getRandomThemeWord();
                        String theme = themeWord[0];
                        String word = themeWord[1];
                        HashMap<String, User> tokenUsers = getUserTokens();
                        Game game = new Game(basePort++, "localhost", tokenUsers, ranked, theme, word);
                        Thread.ofVirtual().start(game::run);

                        for (User user : userQueue.queue) {
                            PrintWriter printWriter = null;
                            try {
                                printWriter = new PrintWriter(user.getSocket().getOutputStream());
                                writeMessage(printWriter, "CON" + ":" + game.getPort() + ":" + game.getHost());
                            } catch (IOException e) {
                                System.out.println("SERVER: Error while writing to client: " + e.getMessage());
                            }
                        }

                        try {
                            queueLock.lock();
                            userQueues.remove(userQueue);
                        } finally {
                            queueLock.unlock();
                        }
                    }
                }
            }
        }
    }
}