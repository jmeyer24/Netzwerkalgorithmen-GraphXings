package GraphXings;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Game.GameInstance.*;
import GraphXings.Game.League.NewLeague;
import GraphXings.Game.League.NewLeagueResult;

import java.io.File;
import java.io.IOException;
import GraphXings.Game.Match.NewMatch;
import GraphXings.Game.Match.NewMatchResult;
import java.util.ArrayList;

// import all the players
import GraphXings.NewFiles.*;

public class GraphXings {
    public static void main(String[] args) {
        // create a file to store the crossings number in them
        // see MixingPlayer.java, writeCycleSizeToFile()
        String path = "Statistics/Data/optimization.txt";
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

        // add players
        ArrayList<NewPlayer> players = new ArrayList<>();
        players.add(new Player04());
        players.add(new Player05());
        players.add(new Player06());
        players.add(new Player07());
        players.add(new Player09());
        players.add(new Player10());
        players.add(new Player11());
        players.add(new Player12());

        // run the league setup
        // league -> matches -> games -> minimize/maximizing
        // RandomCycleFactory factory = new RandomCycleFactory(102060351, true);
        // PlanarGameInstanceFactory factory2 = new
        // PlanarGameInstanceFactory(102060352);

        long timeLimit = 300000000000l;
        long seed = 27081883;
        int bestOf = 1;

        // ----------------------- game versions: objective --------------------
        // NewMatch.MatchType matchType = NewMatch.MatchType.CROSSING_NUMBER;
        NewMatch.MatchType matchType = NewMatch.MatchType.CROSSING_ANGLE;
        // ---------------------------------------------------------------------

        // ----------------------- game versions: graphs --------------------
        // only cycle
        // RandomCycleFactory factory = new RandomCycleFactory(102060351, false);
        // cycle and matching
        // RandomCycleFactory factory = new RandomCycleFactory(102060351, true);
        // planar
        PlanarGameInstanceFactory factory = new PlanarGameInstanceFactory(seed);
        // ------------------------------------------------------------------

        runLeague(players, bestOf, timeLimit, factory, matchType, seed);
        // runRemainingMatches(player,players,bestOf,timeLimit,factory);
    }

    public static void runLeague(ArrayList<NewPlayer> players, int bestOf, long timeLimit, GameInstanceFactory factory,
            NewMatch.MatchType matchType, long seed) {
        NewLeague l = new NewLeague(players, bestOf, timeLimit, factory, matchType, seed);
        // NewLeague l = new NewLeague(players, 1, timeLimit, factory2);
        NewLeagueResult lr = l.runLeague();
        System.out.println(lr.announceResults());
    }

    public static void runRemainingMatches(NewPlayer p1, ArrayList<NewPlayer> opponents, int bestOf, long timeLimit,
            GameInstanceFactory factory, NewMatch.MatchType matchType, long seed) {
        int i = 1;
        for (NewPlayer opponent : opponents) {
            NewMatch m = new NewMatch(p1, opponent, factory, bestOf, timeLimit, matchType, seed);
            NewMatchResult mr = m.play();
            System.out.println("Match " + i++ + ": " + mr.announceResult());
        }
    }
}
