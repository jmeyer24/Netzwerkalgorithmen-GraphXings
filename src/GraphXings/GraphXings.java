package GraphXings;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Algorithms.NewRandomPlayer;
import GraphXings.Game.GameInstance.RandomCycleFactory;
import GraphXings.Game.League.NewLeague;
import GraphXings.Game.League.NewLeagueResult;
import GraphXings.NewFiles.MixingPlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class GraphXings {
    public static void main(String[] args) {
        // create a file to store the crossings number in them
        // see MixingPlayer.java, writeCycleSizeToFile()
        String path = "circleOptimization.txt";
        try {
            File myObj = new File(path);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        // TODO: add players here
        ArrayList<NewPlayer> players = new ArrayList<>();
        players.add(new MixingPlayer());
        players.add(new NewRandomPlayer("NewRandomPlayer2"));

        // run the league setup
        // league -> matches -> games -> minimize/maximizing
        RandomCycleFactory factory = new RandomCycleFactory(24091869, true);
        long timeLimit = 300000000000l;
        NewLeague l = new NewLeague(players, 25, timeLimit, factory);
        NewLeagueResult lr = l.runLeague();
        System.out.println(lr.announceResults());
    }
}
