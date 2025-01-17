package GraphXings;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Algorithms.NewRandomPlayer;
import GraphXings.Game.GameInstance.*;
import GraphXings.Game.League.NewLeague;
import GraphXings.Game.League.NewLeagueResult;
import GraphXings.NewFiles.MixingPlayer;
import GraphXings.NewFiles.ConfigParameterOptimization;
import GraphXings.NewFiles.MixingPlayer.Strategy;

import java.io.File;
import java.io.IOException;
import GraphXings.Game.Match.NewMatch;
import GraphXings.Game.Match.NewMatchResult;
import java.util.ArrayList;

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

        ArrayList<NewPlayer> players = new ArrayList<>();
        ConfigParameterOptimization config = new ConfigParameterOptimization();
        // this instead gives us one single MixingPlayer with standard values
        // ConfigParameterOptimization config = new ConfigParameterOptimization(true);
        for (double relativeCircleSize : config.relativeCircleSizes) {
            for (int sampleSize : config.sampleSizes) {
                for (Strategy strategy : config.strategies) {
                    if (strategy.equals(Strategy.Mirroring)) {
                        players.add(new MixingPlayer(0.0, relativeCircleSize, sampleSize, 0, strategy));
                    } else {
                        for (int vertexSampleSize : config.vertexSampleSizes) {
                            if (strategy.equals(Strategy.BruteForce)) {
                                players.add(
                                        new MixingPlayer(0.0, relativeCircleSize, sampleSize, vertexSampleSize,
                                                strategy));
                            } else {
                                for (double percentage : config.percentages) {
                                    players.add(new MixingPlayer(percentage, relativeCircleSize, sampleSize,
                                            vertexSampleSize, strategy));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (players.size() < 2) {
            players.add(new NewRandomPlayer("RandomPlayer"));
            if (players.size() < 2) {
                players.add(new NewRandomPlayer("RandomPlayer2"));
            }
        }

        // run the league setup
        // league -> matches -> games -> minimize/maximizing
        // RandomCycleFactory factory = new RandomCycleFactory(102060351, true);
        // PlanarGameInstanceFactory factory2 = new
        // PlanarGameInstanceFactory(102060352);
        long timeLimit = 300000000000l;
        long seed = 27081883;
        int bestOf = 1;
        NewMatch.MatchType matchType = NewMatch.MatchType.CROSSING_ANGLE;
        PlanarGameInstanceFactory factory = new PlanarGameInstanceFactory(seed);
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
