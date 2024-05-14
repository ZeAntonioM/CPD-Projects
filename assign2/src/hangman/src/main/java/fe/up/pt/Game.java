package fe.up.pt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import static java.util.Collections.shuffle;

public class Game {
    private List<Client> players;
    private int numPlayers;
    private boolean ranked;

    private String theme;
    private String word;
    boolean running;
    private String guessedWord;

    private List<Integer> ranks;

    public Game(List<Client> players, boolean ranked, String theme, String word) {

        this.numPlayers = players.size();
        this.players = players;
        shuffle(this.players);
        this.ranked = ranked;
        this.theme = theme;
        this.word = word;
        this.running = true;
        this.guessedWord = this.word.replaceAll("[a-zA-Z]", "_");

        this.ranks = new ArrayList<Integer>();
        for (Client player : this.players) {
            this.ranks.add(player.getRank());
        }

    }

    public void start() {
        for (Client player : this.players) {
            player.sendGameStart(this.theme, this.guessedWord);
        }
    }

    public void end() {
        for (Client player : this.players) {
            player.sendGameEnd(this.word);
        }
    }

    public void sendGuess(String guess, int player) {
        if ((guess.length() > 1) && (guess.equals(word))) {
            if (this.ranked) this.updateRank(player, 20);

            this.guessedWord = this.word;
            this.running = false;
            this.ranks.get(player).sendGameCorrectWordGuess();

        }
        else if (guess.length() > 1) {
            if (this.ranked) this.updateRank(player, -10);

            this.ranks.get(player).sendGameWrongWordGuess();
        }
        else if (this.checkLetter(guess)){
            if (this.ranked) this.updateRank(player, 5);
            
            this.updateGuessedWord(guess);
            this.ranks.get(player).sendGameCorrectLetterGuess();
        }
        else {
            if (this.ranked) this.updateRank(player, -2);

            this.ranks.get(player).sendGameCorrectLetterGuess();
        }
    }

    private void updateRank(int player, int points) {
        this.ranks.set(player, this.ranks.get(player) + points);
    }

    private boolean checkLetter(String letter) {
        return this.word.contains(letter);
    }

    private void updateGuessedWord(String letter) {
        for (int i = 0; i < this.word.length(); i++) {
            if (this.word.charAt(i) == letter.charAt(0)) {
                this.guessedWord = this.guessedWord.substring(0, i) + letter + this.guessedWord.substring(i + 1);
            }
        }
    }

}
