package fe.up.pt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private final String address;
    private final int port;
    private Socket socket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private String sessionToken;
    private boolean inGame = false;

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
        String messageKey = message.split(":")[0];
        if (messageKey.equals("LGN") || messageKey.equals("REG") || messageKey.equals("EXT")){
            this.setSessionToken(null);
        }

        String token = getSessionToken() != null ? ":" + getSessionToken() : "";
        printWriter.println(message + token);
        System.out.println("Sent: " + message + token);
        printWriter.flush();
    }

    private String readMessage() throws IOException {
        return bufferedReader.readLine();
    }

    //TODO: Refactor this method to make sense, super spaguetti
    private void showMessageToClient(String message){
        String[] data = message.split(":");
        if (data[0].equals("ERR")){
            System.out.println("Error: " + data[1] + "\n");
        }
        else if (data[0].equals("SUC") && data.length > 1) {
            System.out.println("Success!\n");
            this.setSessionToken(data[1]);
            try {
                mainMenu(this);
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (data[0].equals("CON")) {
            String host = data[2];
            int port = Integer.parseInt(data[1]);
            connectToGame(host, port);
        } else if (data[0].equals("GAM")) {
            switch (data[1]) {
                case "wait" -> System.out.println("Waiting for players...\n");
                case "start" -> System.out.println("Game started!\n");
                case "end" -> System.out.println("Game ended!\n");
                default -> System.out.println("Invalid option!\n");
            }
        } else if (data[0].equals("SUC")) {
            System.out.println("Success!\n");
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
            String message;

            while (!client.inGame) {
                System.out.println("Login [1], Register [2] or Exit [3]?");
                message = reader.readLine();
                String username;
                String password;
                switch (message) {
                    case "1":
                        System.out.println("Enter username: ");
                        username = reader.readLine();
                        System.out.println("Enter password: ");
                        password = reader.readLine();
                        client.writeMessage("LGN:" + username + ":" + password);
                        client.showMessageToClient(client.readMessage());
                        break;
                    case "2":
                        System.out.println("Enter username: ");
                        username = reader.readLine();
                        System.out.println("Enter password: ");
                        password = reader.readLine();
                        client.writeMessage("REG:" + username + ":" + password);
                        client.showMessageToClient(client.readMessage());
                        break;
                    case "3":
                        client.writeMessage("EXT");
                        client.showMessageToClient(client.readMessage());
                        client.getSocket().close();
                        return;
                    default:
                        System.out.println("Invalid option!");
                        break;
                }
            }
            //TODO: perceber a logica do game loop e implementar um menu de jeito actually funcional
            while (client.inGame){
                // GAME LOOP GUI
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            System.out.println("Closing connection...");
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

            writeMessage("GIN:"+getSessionToken());
            showMessageToClient(readMessage());
            inGame = true;

        } catch (IOException e) {
            e.printStackTrace();  // Print the stack trace of the exception
            System.out.println("Error while connecting to the game: " + e.getMessage());
        }
    }


    public void mainMenu(Client client) throws IOException{
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String message;

        while (!inGame) {
            System.out.println("Start a new Game [1], Join a Game [2] or Log Out [3]?");
            message = reader.readLine();
            switch (message) {
                case "1":
                    gameMenu(true, client);
                    break;
                case "2":
                    gameMenu(false, client);
                    break;
                case "3":
                    client.writeMessage("LGO");
                    client.showMessageToClient(client.readMessage());
                    return;
                default:
                    System.out.println("Invalid option!");
                    break;
            }
        }
    }


    public void gameMenu(boolean isNewGame, Client client) throws IOException {
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String message;
        while (true) {
            System.out.println("Ranked [1], Normal [2] or Back [3]?");
            message = reader.readLine();
            String response = message.equals("1") ? (isNewGame ? "GAM:create:rank" : "GAM:join:rank")
                    : message.equals("2") ? (isNewGame ? "GAM:create:normal" : "GAM:join:normal")
                    : message.equals("3") ? null : "Invalid option!";
            System.out.println(response);
            if (response == null) return;
            else if (response.equals("Invalid option!")) System.out.println(response);
            else {
                client.writeMessage(response);
                client.showMessageToClient(client.readMessage());
                if (client.socket.isClosed()) {
                    System.out.println("Connection closed!");
                    return;
                }

                while(true) {
                    String msg = client.readMessage();
                    if (client.socket.isClosed()) {
                        System.out.println("Connection closed!");
                        return;
                    }
                    if (msg != null) client.showMessageToClient(msg);
                }
            }
        }
    }

}
