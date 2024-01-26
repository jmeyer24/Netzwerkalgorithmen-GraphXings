package GraphXings;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Game.GameInstance.*;
import GraphXings.Game.League.NewLeague;
import GraphXings.Game.League.NewLeagueResult;

import GraphXings.Game.Match.NewMatch;
import GraphXings.Game.Match.NewMatchResult;
import java.util.ArrayList;

// import all the players
import GraphXings.NewFiles.*;

public class GraphXings {
    public static void main(String[] args) {

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

        long timeLimit = 300000000000l;
        long seed = 27081883;
        int bestOf = 5;

        // ----------------------- game versions: objective --------------------
        // NewMatch.MatchType matchType = NewMatch.MatchType.CROSSING_NUMBER;
        // NewMatch.MatchType matchType = NewMatch.MatchType.CROSSING_ANGLE;
        // ---------------------------------------------------------------------

        // ----------------------- game versions: graphs --------------------
        // only cycle
        RandomCycleFactory randomCycleFactory = new RandomCycleFactory(123456, false);
        // cycle and matching
        RandomCycleFactory randomCycleFactoryMatching = new RandomCycleFactory(123456, true);
        // planar
        PlanarGameInstanceFactory planarFactory = new PlanarGameInstanceFactory(seed);
        // ------------------------------------------------------------------

        System.out.println(
                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
        System.out.println("CYCLE WITH CROSSING ANGLES");
        runLeague(players, bestOf, timeLimit, randomCycleFactory, NewMatch.MatchType.CROSSING_ANGLE, seed);
        System.out.println(
                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
        System.out.println("CYCLE WITH CROSSING NUMBERS");
        runLeague(players, bestOf, timeLimit, randomCycleFactory, NewMatch.MatchType.CROSSING_NUMBER, seed);
        System.out.println(
                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
        System.out.println("MATCHING AND CROSSING ANGLES");
        runLeague(players, bestOf, timeLimit, randomCycleFactoryMatching, NewMatch.MatchType.CROSSING_ANGLE, seed);
        System.out.println(
                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
        System.out.println("MATCHING AND CROSSING NUMBERS");
        runLeague(players, bestOf, timeLimit, randomCycleFactoryMatching, NewMatch.MatchType.CROSSING_NUMBER, seed);
        System.out.println(
                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
        System.out.println("PLANAR AND CROSSING ANGLES");
        runLeague(players, bestOf, timeLimit, planarFactory, NewMatch.MatchType.CROSSING_ANGLE, seed);
        System.out.println(
                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
        System.out.println("PLANAR AND CROSSING NUMBERS");
        runLeague(players, bestOf, timeLimit, planarFactory, NewMatch.MatchType.CROSSING_NUMBER, seed);
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
