package fe.up.pt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import static java.util.Collections.shuffle;

public class Game {
    private List<String> playerList;
    private int players;
    private boolean ranked;

    private String theme;
    private String word;
    boolean running;
    private String guessedWord;

    public Game(int players, List<String> playerList, boolean ranked, String theme, String word) {


        this.playerList = playerList;
        shuffle(this.playerList);
        this.players = players;
        this.ranked = ranked;
        this.theme = theme;
        this.word = word;
        this.running = true;
        this.guessedWord = this.word.replaceAll("[a-zA-Z]", "_");

    }

    public void get_guessedWord() {
        System.out.println(this.guessedWord);
    }

    //TODO refactor code to only backend
    public void start() throws IOException {

        for (int i = 0; i < this.players; i++) {
            this.connections.get(i).sendGameStart();
        }
        System.out.println("Game started!");

        while(running) {

            Scanner scanner = new Scanner(System.in);
            String letter = scanner.nextLine();
            if (letter.length() > 1) {
                if (letter.equals(this.word)) {
                    for (int i = 0; i < this.players; i++) {
                        this.connections.get(i).sendGameEnd();
                    }
                    this.running = false;
                    break;
                }
            }
            this.guessedWord = this.word.replaceAll("[^" + letter + "]", "_");

            for (int i = 0; i < this.players; i++) {
                this.connections.get(i).sendGameLetter(letter);
            }

        }

    }

    public void addPlayer(String token){
        this.playerList.add(token);
    }

}
