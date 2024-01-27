package GraphXings.Gruppe4;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservations.*;
import GraphXings.Gruppe4.Strategies.StrategyName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GameObserver {

    private final int totalVerticesCount;
    private int currentVerticesCount = 0;
    private NewPlayer.Role ourRole;
    private int width;
    private int height;
    private final HashMap<StrategyName, Integer> strategyNamesCounts;
    private final ArrayList<StrategyName> strategyNamesList;
    private ArrayList<GameMove> ourMoves;
    private ArrayList<GameMove> opponentMoves;
    private NewPlayer.Role[][] usedCoordinatesRoles;
    private long startTime;
    private long totalElapsedTime = 0;
    private long currentGameMoveTime = 0;
    private SampleParameters sampleParameters = new SampleParameters(SampleSize.Keep, 10, 1);
    private StrategiesStopWatch strategiesStopWatch = new StrategiesStopWatch();
    private final long timeLimit = 300000000000L;

    // This is a safety measure, so we don't get a timeout
    private final long timeLimitBuffer = 50000000000L; // 50s

    public GameObserver(Graph g, NewPlayer.Role ourRole, int width, int height) {
        totalVerticesCount = ((HashSet<Vertex>) g.getVertices()).size();
        strategyNamesCounts = new HashMap<>(StrategyName.values().length);
        strategyNamesList = new ArrayList<>(totalVerticesCount);
        ourMoves = new ArrayList<>(totalVerticesCount / 2);
        opponentMoves = new ArrayList<>(totalVerticesCount / 2);
        this.width = width;
        this.height = height;

        usedCoordinatesRoles = new NewPlayer.Role[width][height];

        this.ourRole = ourRole;
    }

    public void addOwnGameMove(GameMove move, StrategyName strategyName) {
        currentVerticesCount += 1;

        // Update the roles data structure
        var coordinate = move.getCoordinate();
        usedCoordinatesRoles[coordinate.getX()][coordinate.getY()] = ourRole;

        incrementUsedStrategies(strategyName);
        ourMoves.add(move);
    }

    public void addOpponentGameMove(GameMove move) {
        currentVerticesCount += 1;

        // Update the roles data structure
        var coordinate = move.getCoordinate();
        usedCoordinatesRoles[coordinate.getX()][coordinate.getY()] = (ourRole == NewPlayer.Role.MAX) ? NewPlayer.Role.MIN : NewPlayer.Role.MAX;

        opponentMoves.add(move);
    }

    /**
     * Returns how many moves are left before the game ends
     * @return Amount of remaining moves
     */
    public int remainingMoves() {
        return totalVerticesCount - currentVerticesCount;
    }

    /**
     * Returns a double between 0-100% of already placed moves
     * @return A double
     */
    public double percentagePlacedMoves() {
        return currentVerticesCount / (double) totalVerticesCount * 100.0;
    }

    public void startTimer() {
        startTime = System.nanoTime();
    }

    public void stopTimer() {
       var stopTime = System.nanoTime();

       currentGameMoveTime = stopTime - startTime;

       totalElapsedTime += currentGameMoveTime;
    }

    public long getTotalElapsedTime() {
        return totalElapsedTime;
    }

    /**
     * Retrieve the optimal runtime of a single game move in nano seconds
     * @return Game move runtime
     */
    public long getSingleGameMoveTime() {
        // We divide our time limit by the amount of vertices we have to place
        //long gameMove = (timeLimit - timeLimitBuffer) / (totalVerticesCount / 2);
        long remainingVertices = ((totalVerticesCount / 2) - ourMoves.size());

        // Prevent division by zero
        if (remainingVertices <= 0) {
            remainingVertices = 1;
        }
        long gameMove = (timeLimit - timeLimitBuffer - getTotalElapsedTime()) / remainingVertices;

        // One game move should at least have 5ms time to compute
        // TODO: These are nano seconds...
        if (gameMove < 500) {
            return 500;
        }
        return gameMove;
    }

    public long getSingleGameMoveTimeThread() {

        return (timeLimit - timeLimitBuffer) / (totalVerticesCount / 2);
    }

    /**
     * Retrieve an estimation if the sample size should increase/decrease
     * @return
     */
    public SampleSize getSampleSizeAdjustment() {
        long timeDiff = getSingleGameMoveTime() - currentGameMoveTime;
        // If we have more than 1s to spend then increment sample size
        if (timeDiff > 2000000000) {
            return SampleSize.Increment;
        } else if (timeDiff < 100000000) { // 100ms
            // If difference is negative we would run too long
            return SampleSize.Decrement;
        }

        // We've found a nice sample size
        return SampleSize.Keep;
    }

    /**
     * Return a new SampleParameters record for the current round.
     * This will alter the sample parameters so only use once per round.
     * If you'd like to get the parameters again, use the get method instead.
     * @return A new SampleParameters object
     */
    public SampleParameters calculateSampleSizeParameters() {
        var sampleSize = getSampleSizeAdjustment();
        var samples = sampleParameters.samples();
        var perimeter = sampleParameters.perimeter();

        switch (sampleSize) {
            case Increment -> {
                samples++;
                //perimeter++;
            }
            case Decrement -> {
                samples--;
                //perimeter--;
            }
            case Keep -> {
                // Do nothing
            }
        }
        if (samples <= 0) {
            samples = 1;
        }
        if (perimeter <= 0) {
            perimeter = 1;
        }
        samples = 10;
        sampleParameters = new SampleParameters(sampleSize, samples, perimeter);
        return sampleParameters;
    }

    /**
     * Get the SampleParameters without triggering recalculation.
     * @return A SampleParameters record
     */
    public SampleParameters getSampleParameters() {
        return sampleParameters;
    }

    public StrategiesStopWatch getStrategiesStopWatch() {
        return strategiesStopWatch;
    }

    /**
     * Count how often we use our own strategies.
     * @param strategyName The strategy name which should get incremented
     */
    private void incrementUsedStrategies(StrategyName strategyName) {
        strategyNamesList.add(strategyName);
        if (strategyNamesCounts.containsKey(strategyName)) {
            // Counter value is already present. Get, increment, put
            var count = strategyNamesCounts.get(strategyName);
            count += 1;
            strategyNamesCounts.put(strategyName, count);
        } else {
            strategyNamesCounts.put(strategyName, 1);
        }
    }

    /**
     * Print all stats
     */
    public void report() {
        System.out.println("Used strategies counter:");
        System.out.println(reportUsedStrategiesCount());

        System.out.println("Used strategies steps:");
        System.out.println(reportUsedStrategiesSteps());

        System.out.println("Total elapsed time:" + totalElapsedTime + "ns (" + totalElapsedTime / (double)1000000 + "ms)");
        System.out.println("Last game move time: " + currentGameMoveTime + "ns (" + currentGameMoveTime / (double)1000000 + "ms)");
        System.out.println("Optimal single game move time: " + getSingleGameMoveTime() + "ns (" + getSingleGameMoveTime() / (double)1000000 + "ms)");
        System.out.println("Elapsed time per strategy:");
        System.out.println(reportElapsedTimePerStrategy());
    }

    /**
     * Report all strategies and their counters.
     * @return The report string
     */
    public String reportUsedStrategiesCount() {
        StringBuilder output = new StringBuilder();

        // Use a string builder for better performance
        for (Map.Entry<StrategyName, Integer> entry : strategyNamesCounts.entrySet()) {
            output.append(entry.getKey().name()).append(",").append(entry.getValue()).append("\n");
        }

        return output.toString();
    }

    public String reportUsedStrategiesSteps() {
        StringBuilder output = new StringBuilder();

        // Use a string builder for better performance
        int num = 0;
        for (var item : strategyNamesList) {
            output.append(num).append(",").append(item.name()).append("\n");
            num++;
        }

        return output.toString();
    }

    public String reportElapsedTimePerStrategy() {
        StringBuilder output = new StringBuilder();
        var strategyStopWatches = strategiesStopWatch.getStrategyStopWatches();

        for (Map.Entry<StrategyName, StopWatch> entry : strategyStopWatches.entrySet()) {
            var ms = entry.getValue().getTotalElapsedTime() / (double)1000000;
            output.append(entry.getKey()).append(": ").append(entry.getValue().getTotalElapsedTime()).append("ns (").append(ms).append("ms)\n");
        }

        return output.toString();
    }

    /**
     * This routine runs all CanvasObservations and calculates the best
     * strategy for our next move.
     * @return A counter-attack strategy
     */
    public StrategyName observationRunner(Graph g, GameState gs, GameMove lastMove) {
        // Define all available observers
        CanvasObservation[] observers = {
                new ObserveBorders(g, ourMoves, opponentMoves, ourRole, gs, usedCoordinatesRoles, width, height),
                new ObserveOpponentPlacesNeighbours(g, ourMoves, opponentMoves, ourRole),
                new ObserveOpponentPlacesNextToUs(g, ourMoves, opponentMoves, ourRole)
        };

        // Choose the observation with the best score (max. = 100)
        CanvasObservation bestChoice = null;
        int bestObservationScore = 0;
        for (var observer : observers) {
            observer.calculateObservation(lastMove);

            if (observer.getObservation() > bestObservationScore) {
                bestChoice = observer;
                bestObservationScore = observer.getObservation();
            }
        }

        // Return the best counter strategy
        if (bestChoice != null) {
            return bestChoice.getEffectiveCounterStrategy();
        }

        // Or return unknown if we don't know what to do :D
        return StrategyName.Unknown;
    }
}
