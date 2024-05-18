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
        String messageKey = message.split(":")[0];
        if (messageKey.equals("LGN") || messageKey.equals("REG")){
            this.setSessionToken(null);
        }

        String token = getSessionToken() != null ? ":" + getSessionToken() : "";
        printWriter.println(message + token);
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
                        client.writeMessage("LGO");
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
            System.out.println("Start a new Game [1], Join a Game [2] or Exit [3]?");
            message = reader.readLine();
            switch (message) {
                case "1":
                    gameMenu(true);
                    break;
                case "2":
                    gameMenu(false);
                    break;
                case "3":
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
            String response = message.equals("1") ? (isNewGame ? "GAM:create:rank" : "GAM:join:rank")
                    : message.equals("2") ? (isNewGame ? "GAM:create:normal" : "GAM:join:normal")
                    : message.equals("3") ? null : "Invalid option!";
            if (response == null) return;
            else if (response .equals("Invalid option!")) System.out.println(response);
            else {
                writeMessage(response);
                showMessageToClient(readMessage());
            }
        }
    }
}
