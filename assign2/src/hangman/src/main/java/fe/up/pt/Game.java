package fe.up.pt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.shuffle;

public class Game {

    private int gameID;
    private List<User> players;
    private int numPlayers;
    private boolean ranked;
    private String theme;
    private String word;
    private boolean running;
    private String guessedWord;
    private List<Integer> ranks;
    public Game(int gameID, List<User> players, boolean ranked, String theme, String word) {

        this.gameID = gameID;
        this.numPlayers = players.size();
        this.players = players;
        shuffle(this.players);
        this.ranked = ranked;
        this.theme = theme;
        this.word = word;
        this.running = true;
        this.guessedWord = this.word.replaceAll("[a-zA-Z]", "_");
        this.ranks = new ArrayList<Integer>();
        for (User player : this.players) {
            this.ranks.add(player.getRank());
        }


    }

    public void start() {
        for (User player : this.players) {
            this.sendGameStart(player, this.theme, this.guessedWord);
        }
    }

    public void end() {
        for (User player : this.players) {
            this.sendGameEnd(player, this.word);
        }
    }

    public void sendAskForGuess(User player) {
        try {
            //this.validateToken(player);
            PrintWriter printWriter = new PrintWriter(player.getSocket().getOutputStream(), true);
            player.getSocket().getOutputStream().write(("GAG:" + player.getActiveToken() + "\n").getBytes());
        } catch (IOException e) {
            System.out.println("Error sending ask for guess message: " + e.getMessage());
        }
    }

    public void receiveGuess(String guess, int num_player) {
        User player = this.players.get(num_player);
        if ((guess.length() > 1) && (guess.equals(word))) {
            if (this.ranked) this.updateRank(num_player, 20);

            this.guessedWord = this.word;
            this.running = false;
            this.sendCorrectGuess(player, guess);

        }
        else if (guess.length() > 1) {
            if (this.ranked) this.updateRank(num_player, -10);

            this.sendWrongGuess(player, guess);
        }
        else if (this.checkLetter(guess)){
            if (this.ranked) this.updateRank(num_player, 5);

            this.updateGuessedWord(guess);
            this.sendCorrectGuess(player,guess);
        }
        else {
            if (this.ranked) this.updateRank(num_player, -2);

            this.sendWrongGuess(player, guess);
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

    private void sendGameStart(User player, String theme, String guessedWord) {
        try {
            //this.validateToken(player);
            player.getSocket().getOutputStream().write(("GST:" + gameID + ":" + player.getActiveToken() + ":" + theme + ":" + guessedWord + "\n").getBytes());
        } catch (IOException e) {
            System.out.println("Error sending game start message: " + e.getMessage());
        }
    }

    private void sendGameEnd(User player, String word) {
        try {
            //this.validateToken(player);
            player.getSocket().getOutputStream().write(("GEN:" + gameID + ":" + player.getActiveToken() + ":" + word + ":" + "\n").getBytes());
        } catch (IOException e) {
            System.out.println("Error sending game end message: " + e.getMessage());
        }
    }

    private void sendCorrectGuess(User player, String guess) {
        try {
            //this.validateToken(player);
            player.getSocket().getOutputStream().write(("GCG:" + player.getActiveToken() + ":" + guess + "\n").getBytes());
        } catch (IOException e) {
            System.out.println("Error sending correct guess message: " + e.getMessage());
        }
    }

    private void sendWrongGuess(User player, String guess) {
        try {
            //this.validateToken(player);
            player.getSocket().getOutputStream().write(("GWG:" + player.getActiveToken() + ":" + guess + "\n").getBytes());
        } catch (IOException e) {
            System.out.println("Error sending wrong guess message: " + e.getMessage());
        }
    }




    public List<User> getPlayers() {
        return players;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public boolean isRanked() {
        return ranked;
    }

    public String getWord() {
        return word;
    }

    public boolean isRunning() {
        return running;
    }

    public String getGuessedWord() {
        return guessedWord;
    }

    public List<Integer> getRanks() {
        return ranks;
    }

}
