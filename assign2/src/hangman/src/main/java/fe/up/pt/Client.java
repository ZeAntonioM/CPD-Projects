package fe.up.pt;

import java.io.*;
import java.net.Socket;

public class Client {

    private final String address;
    private final int port;
    private Socket socket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private String sessionToken;

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
        printWriter.println(message);
        printWriter.flush();
    }

    private String readMessage() throws IOException {
        return bufferedReader.readLine();
    }

    private void showMessageToClient(String message){
        String[] data = message.split(":");
        if (data[0].equals("ERR")){
            System.out.println("Error: " + data[1] + "\n");
        }
        else if (data[0].equals("SUC") && data.length > 1) {
            System.out.println("Success!\n");
            this.setSessionToken(data[1]);
            try {
                mainMenu();
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
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

            while (true) {
                System.out.println("Login [1], Register [2] or Logout [3]?");
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
                        System.out.println(client.getSessionToken());
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
                        if (client.getSessionToken() == null){
                            System.out.println("You are not logged in!");
                            break;
                        }
                        System.out.println(client.getSessionToken());
                        client.writeMessage("LGO:" + client.getSessionToken());
                        client.showMessageToClient(client.readMessage());
                        client.getSocket().close();
                        return;
                    default:
                        System.out.println("Invalid option!");
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            System.out.println("Closing connection...");
        }
    }

    public void mainMenu() throws IOException{
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String message;

        while (true) {
            System.out.println("Start a new Game [1], Join a Game [2] or Logout [3]?");
            message = reader.readLine();
            switch (message) {
                case "1":
                    gameMenu(true);
                    break;
                case "2":
                    gameMenu(false);
                    break;
                case "3":
                    if (getSessionToken() == null){
                        System.out.println("You are not logged in!");
                        break;
                    }
                    System.out.println(getSessionToken());
                    writeMessage("LGO:" + getSessionToken());
                    showMessageToClient(readMessage());
                    getSocket().close();
                    return;
                default:
                    System.out.println("Invalid option!");
                    break;
            }
        }
    }

    public void gameMenu(boolean isNewGame) throws IOException {
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String message;
        while (true) {
            System.out.println("Ranked [1], Normal [2] or Back [3]?");
            message = reader.readLine();
            switch (message) {
                case "1":
                    if (isNewGame){
                        // Handle new game start
                        writeMessage("GAM"); // Send message to server
                    } else {
                        // Handle game join
                        writeMessage("GAM"); // Send message to server
                    }
                    break;
                case "2":
                    if (isNewGame){
                        // Handle new game start
                        writeMessage("GAM"); // Send message to server
                    } else {
                        // Handle game join
                        writeMessage("GAM"); // Send message to server
                    }
                    break;
                case "3":
                    mainMenu();
                    return;
                default:
                    System.out.println("Invalid option!");
                    break;
            }
        }
    }
}
