package GraphXings;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Algorithms.NewRandomPlayer;
import GraphXings.Game.GameInstance.PlanarGameInstanceFactory;
import GraphXings.Game.League.NewLeague;
import GraphXings.Game.League.NewLeagueResult;
import GraphXings.NewFiles.MixingPlayer;
import GraphXings.NewFiles.ConfigParameterOptimization;
import GraphXings.NewFiles.MixingPlayer.Strategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class NewGraphXings {
    public static void main(String[] args) {
        // create a file to store the crossings number in them
        // see MixingPlayer.java, writeCycleSizeToFile()
        ArrayList<NewPlayer> players = new ArrayList<>();
        players.add(new MixingPlayer());
        players.add(new MixingPlayer());

        // run the league setup
        // league -> matches -> games -> minimize/maximizing
        // RandomCycleFactory factory = new RandomCycleFactory(102060351, true);
        PlanarGameInstanceFactory factory2 = new PlanarGameInstanceFactory(102060352);
        long timeLimit = 300000000000l;
        NewLeague l = new NewLeague(players, 1, timeLimit, factory2);
        NewLeagueResult lr = l.runLeague();
        System.out.println(lr.announceResults());
    }
}
