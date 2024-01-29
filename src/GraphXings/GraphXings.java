package GraphXings;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Game.GameInstance.*;
import GraphXings.Game.League.NewLeague;
import GraphXings.Game.League.NewLeagueResult;

import GraphXings.Game.Match.NewMatch;
import GraphXings.Game.Match.NewMatchResult;
import GraphXings.Game.Match.NewMatch.MatchType;

import java.util.ArrayList;

// import all the players
import GraphXings.NewFiles.*;
import GraphXings.NewFiles.GraphDracula.MaximizingStrategy;
import GraphXings.NewFiles.GraphDracula.MinimizingStrategy;

public class GraphXings {
        public static void main(String[] args) {

                // add players
                ArrayList<NewPlayer> players = new ArrayList<>();
                StrategyOptimization config = new StrategyOptimization();
                // for (MaximizingStrategy maxStrat : config.maxStrats) {
                // for (MinimizingStrategy minStrat : config.minStrats) {
                // players.add(new GraphDracula(maxStrat, minStrat));
                // }
                // }
                // players.add(new Player04());
                // players.add(new Player05());
                // players.add(new Player06());
                // players.add(new Player07());
                // players.add(new Player09());
                // players.add(new Player10());
                // players.add(new Player11());
                // players.add(new Player12());
                players.add(new GraphDracula_NichtPlaettbar());
                players.add(new GraphDracula_Plaettbar());

                long timeLimit = 300000000000l; // 5 mins?
                long seed = 29;
                int bestOf = 3;

                // ----------------------- game versions: objective --------------------
                // MatchType matchType = MatchType.CROSSING_NUMBER;
                // MatchType matchType = MatchType.CROSSING_ANGLE;
                // ---------------------------------------------------------------------

                // ----------------------- game versions: graphs --------------------
                // only cycle
                RandomCycleFactory randomCycleFactory = new RandomCycleFactory(seed, false);
                // cycle and matching
                RandomCycleFactory randomCycleFactoryMatching = new RandomCycleFactory(seed, true);
                // planar
                PlanarGameInstanceFactory planarFactory = new PlanarGameInstanceFactory(seed);
                // ------------------------------------------------------------------

                System.out.println(
                                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
                System.out.println("CYCLE WITH CROSSING ANGLES");
                runLeague(players, bestOf, timeLimit, randomCycleFactory, MatchType.CROSSING_ANGLE, seed);
                System.out.println(
                                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
                System.out.println("CYCLE WITH CROSSING NUMBERS");
                runLeague(players, bestOf, timeLimit, randomCycleFactory, MatchType.CROSSING_NUMBER, seed);
                System.out.println(
                                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
                System.out.println("MATCHING AND CROSSING ANGLES");
                runLeague(players, bestOf, timeLimit, randomCycleFactoryMatching, MatchType.CROSSING_ANGLE, seed);
                System.out.println(
                                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
                System.out.println("MATCHING AND CROSSING NUMBERS");
                runLeague(players, bestOf, timeLimit, randomCycleFactoryMatching, MatchType.CROSSING_NUMBER, seed);
                System.out.println(
                                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
                System.out.println("PLANAR AND CROSSING ANGLES");
                runLeague(players, bestOf, timeLimit, planarFactory, MatchType.CROSSING_ANGLE, seed);
                System.out.println(
                                "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
                System.out.println("PLANAR AND CROSSING NUMBERS");
                runLeague(players, bestOf, timeLimit, planarFactory, MatchType.CROSSING_NUMBER, seed);
        }

        public static void runLeague(ArrayList<NewPlayer> players, int bestOf, long timeLimit,
                        GameInstanceFactory factory,
                        MatchType matchType, long seed) {
                NewLeague l = new NewLeague(players, bestOf, timeLimit, factory, matchType, seed);
                // NewLeague l = new NewLeague(players, 1, timeLimit, factory2);
                NewLeagueResult lr = l.runLeague();
                System.out.println(lr.announceResults());
        }

        public static void runRemainingMatches(NewPlayer p1, ArrayList<NewPlayer> opponents, int bestOf, long timeLimit,
                        GameInstanceFactory factory, MatchType matchType, long seed) {
                int i = 1;
                for (NewPlayer opponent : opponents) {
                        NewMatch m = new NewMatch(p1, opponent, factory, bestOf, timeLimit, matchType, seed);
                        NewMatchResult mr = m.play();
                        System.out.println("Match " + i++ + ": " + mr.announceResult());
                }
        }
}
