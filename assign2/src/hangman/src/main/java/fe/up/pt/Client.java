package fe.up.pt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class Client {

    private String address;
    private int port;
    private Socket socket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private String sessionToken;
    private boolean inGame = false;
    public String state = "loginMenu"; // "mainMenu", "gameMenu", "game"

    public Client(String address, int port) throws IOException{
        this.address = address;
        this.port = port;
        this.socket = new Socket(address, port);
        this.printWriter = new PrintWriter(socket.getOutputStream(), true);
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public Socket getSocket(){
        return this.socket;
    }

    public String getSessionToken(){
        return this.sessionToken;
    }

    public void setSessionToken(String sessionToken){
        this.sessionToken = sessionToken;
    }

    private void writeMessage(String message) throws IOException {
        int retryCount = 0;
        String messageKey = message.split(":")[0];
        if (messageKey.equals("LGN") || messageKey.equals("REG") || messageKey.equals("EXT")){
            this.setSessionToken(null);
        }

        String token = getSessionToken() != null ? ":" + getSessionToken() : "";
        printWriter.println(message + token);
        printWriter.flush();
    }

    private String readMessage() throws IOException {
        String message = bufferedReader.readLine();

        return message;
    }

    //TODO: Refactor this method to make sense, super spaguetti
    private void showMessageToClient(String message, String state){
        String[] data = message.split(":");
        System.out.println("Received message: " + Arrays.toString(data) + "\n");
        if (data[0].equals("ERR")){
            System.out.println("Error: " + data[1] + "\n");
            this.state = state;
        }
        else if (data[0].equals("SUC") && data.length > 1) {
            System.out.println("Success!\n");
            this.setSessionToken(data[1]);
            this.state = "mainMenu";

        } else if (data[0].equals("CON")) {
            System.out.println("Connected to game!\n");
            this.address = data[2];
            this.port = Integer.parseInt(data[1]);
            this.state = "connectToGame";


        } else if (data[0].equals("GAM")) {
            switch (data[1]) {
                case "wait" :
                    System.out.println("Waiting for players...\n");
                    break;
                case "start":
                    System.out.println("Game started!\n");
                    break;
                case "end":
                    System.out.println("Game ended!\n");
                    this.inGame = false;
                    this.state = "mainMenu";
                    break;
                default:
                    System.out.println("Invalid option!\n");
                    break;
            }
        } else if (data[0].equals("SUC")) {
            System.out.println("Success!\n");
        } else if (data[0].equals("ACK")) {
            System.out.println("ACK received!\n");
        }
        else {
            System.out.println("Unknown response received!: " + data[0] + "\n");
        }
    }


    public static void main(String[] args) throws IOException{
        String serverAddress = "localhost"; // Replace with server IP or hostname
        int port = 12345; // Replace with server port

        try{
            Client client = new Client(serverAddress, port);
            System.out.println("Connected to server!");

            // Client CLI
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                if (!client.inGame) {
                    switch (client.state) {
                        case "loginMenu":
                            client.loginMenu(reader);
                            break;
                        case "mainMenu":
                            client.mainMenu(reader);
                            break;
                        case "newGame":
                            client.gameMenu(true, reader);
                            break;
                        case "joinGame":
                            client.gameMenu(false, reader);
                            break;
                        case "connectToGame":
                            client.showMessageToClient(client.readMessage(), client.state);
                            client.connectToGame(client.address, client.port);
                            break;
                        case "close":
                            return;
                        default:
                            System.out.println("Invalid state!");
                            break;
                    }
                } else {
                    switch (client.state) {
                        case "waitingForPlayers":
                            client.showMessageToClient(client.readMessage(), client.state);
                            break;
                        case "game":


                            break;
                        default:
                            System.out.println("Invalid state!");
                            break;


                    }



                }
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            System.out.println("Closing connection...");
        }
    }

    private void loginMenu(BufferedReader reader) throws IOException {
        Client client = this;
        System.out.println("Login [1], Register [2] or Exit [3]?");
        String message = reader.readLine();
        String username;
        String password;
        String prevState;
        switch (message) {
            case "1":
                System.out.println("Enter username: ");
                username = reader.readLine();
                System.out.println("Enter password: ");
                password = reader.readLine();

                prevState = client.state;
                client.state = "mainMenu";

                client.writeMessage("LGN:" + username + ":" + password);
                client.showMessageToClient(client.readMessage(), prevState);
                return;
            case "2":
                System.out.println("Enter username: ");
                username = reader.readLine();
                System.out.println("Enter password: ");
                password = reader.readLine();

                prevState = client.state;
                client.state = "mainMenu";

                client.writeMessage("REG:" + username + ":" + password);
                client.showMessageToClient(client.readMessage(), prevState);
                return;
            case "3":
                prevState = client.state;
                client.state = "close";

                client.writeMessage("EXT");
                client.showMessageToClient(client.readMessage(), prevState);
                client.getSocket().close();
                return;
            default:
                System.out.println("Invalid option!");
                break;
        }
    }
    public void connectToGame(String host, int port) {
        try {
            // Close the old socket
            if (this.socket != null) {
                this.socket.close();
            }

            this.socket = new Socket(host, port);
            this.printWriter = new PrintWriter(socket.getOutputStream(), true);
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writeMessage("GIN");
            inGame = true;
            this.state = "waitingForPlayers";

        } catch (IOException e) {
            e.printStackTrace();  // Print the stack trace of the exception
            System.out.println("Error while connecting to the game: " + e.getMessage());
        }
    }



    public void mainMenu(BufferedReader reader) throws IOException {
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        String message;
        Client client = this;

        while (!inGame) {
            System.out.println("Start a new Game [1], Join a Game [2] or Log Out [3]?");
            message = reader.readLine();
            switch (message) {
                case "1":
                    client.state = "newGame";
                    return;
                case "2":
                    client.state = "joinGame";
                    return;
                case "3":
                    String prevState = client.state;
                    client.state = "loginMenu";
                    client.writeMessage("LGO");
                    client.showMessageToClient(client.readMessage(), prevState);
                    return;
                default:
                    System.out.println("Invalid option!");
                    break;
            }
        }
    }


    public void gameMenu(boolean isNewGame, BufferedReader reader) throws IOException {
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        String message;
        Client client = this;
        while (true) {
            System.out.println("Ranked [1], Normal [2] or Back [3]?");
            message = reader.readLine();
            String response = message.equals("1") ? (isNewGame ? "GAM:create:rank" : "GAM:join:rank")
                    : message.equals("2") ? (isNewGame ? "GAM:create:normal" : "GAM:join:normal")
                    : message.equals("3") ? null : "Invalid option!";

            switch (response){
                case null:
                    client.state = "mainMenu";
                    return;
                case "Invalid option!":
                    System.out.println(response);
                    break;
                default:
                    String prevState = client.state;
                    client.state = "connectToGame";
                    client.writeMessage(response);
                    client.showMessageToClient(client.readMessage(), prevState);
                    return;
            }
        }
    }

}


