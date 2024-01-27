package GraphXings.Gruppe4;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Data.Coordinate;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservations.SampleParameters;
import GraphXings.Gruppe4.CanvasObservations.SampleSize;
import GraphXings.Gruppe4.Common.Helper;
import GraphXings.Gruppe4.Common.TreeHelper;
import GraphXings.Gruppe4.Strategies.*;
import GraphXings.Gruppe4.Gui.GuiExport;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;

import java.io.IOException;
import java.util.*;
import GraphXings.Data.Edge;



public class RTreePlayer implements NewPlayer {

    /**
     * The name of the random player.
     */
    private String name;

    /**
     * The mutable R-Tree structure. Filled with edges.
     */
    private MutableRTree<Edge, LineFloat> tree;

    /**
     * The mutable R-Tree structure filled with the placed vertices.
     */
    private MutableRTree<Vertex, PointFloat> vertexTree;

    /**
     * The current game state
     */
    private GameState gs;

    private int width;

    private int height;

    private Graph g;

    public GuiExport getGuiExport() {
        return guiExport;
    }

    private GuiExport guiExport;

    private GameObserver gameObserver;

    // Set to true if you'd like to export data
    private boolean enableExport = false;

    /**
     * Creates a random player with the assigned name.
     * @param name
     */
    public RTreePlayer(String name)
    {
        this.name = name;
    }

    @Override
    public GameMove maximizeCrossings(GameMove lastMove)
    {

        // Get the estimated SampleParameters
        var sampleParameters = gameObserver.calculateSampleSizeParameters();

        // Instantiate the strategies
        Strategy[] maximizer = {
                new MaximizePlaceVertexOnEdge(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new MaximizePlaceInDenseRegion(g, gs, tree, vertexTree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new MaximizeDiagonalCrossing(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new MaximizePointReflection(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                //new MaximizePointReflectionFromBorder(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                //new MaximizeGrid(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new RandomSampleMove(g, gs, tree, width, height, Role.MAX, sampleParameters, gameObserver.getStrategiesStopWatch()),
        };
        return calculateCrossings(lastMove, Role.MAX, maximizer);
    }

    @Override
    public GameMove minimizeCrossings(GameMove lastMove)
    {
        // Get the estimated SampleParameters
        var sampleParameters = gameObserver.calculateSampleSizeParameters();

        // Instantiate the strategies
        Strategy[] minimizer = {
                //new MinimizePlaceNextToOpponent(g, gs, tree, width, height, sampleParameters),
                //new MinimizePlaceAtBorder(g, gs, tree, width, height, sampleParameters),
                new RandomSampleMove(g, gs, tree, width, height, Role.MIN, sampleParameters, gameObserver.getStrategiesStopWatch()),
        };

        return calculateCrossings(lastMove, Role.MIN, minimizer);
    }

    @Override
    public void initializeNextRound(Graph g, int width, int height, Role role)
    {
        var edges = (HashSet<Edge>) g.getEdges();
        // If we have <10k Edges use the normal R-Tree.
        // Otherwise, use the R*-Tree heuristic.
        if (edges.size() < 10000) {
            tree = new MutableRTree<>(MutableRTree.TreeSetup.SMALL, width, height);
        } else {
            tree = new MutableRTree<>(MutableRTree.TreeSetup.BIG, width, height);
        }

        // Initialize the vertex tree
        var vertices = (HashSet<Vertex>) g.getVertices();
        vertexTree = new MutableRTree<>((vertices.size() < 10000) ? MutableRTree.TreeSetup.SMALL : MutableRTree.TreeSetup.BIG, width, height);

        this.g = g;
        this.width = width;
        this.height = height;
        gs = new GameState(g, width, height);

        this.gameObserver = new GameObserver(g, role, width, height);

        if (enableExport) {
            try {
                if (guiExport != null) {
                    guiExport.close();
                }
                guiExport = new GuiExport();

                // Export the initial graph
                guiExport.exportGraphStructure(g, role, name);
            } catch (IOException e) {
                enableExport = false;
            }
        }
    }

    public GameMove maximizeCrossingAngles(GameMove lastMove) {
        // TODO
        //return maximizeCrossings(lastMove);
        var sampleParameters = new SampleParameters(SampleSize.Keep, 10, 1);
        Strategy[] strategies = {
                //new MaximizePlaceVertexOnEdge(g, gs, tree, width, height, sampleParameters),
                //new MaximizePlaceInDenseRegion(g, gs, tree, vertexTree, width, height, sampleParameters),
                //new MaximizeDiagonalCrossing(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new MaximizePointReflection(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new MaximizePointReflectionFromBorder(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new MaximizeDiagonalCrossingAngle(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                //new MaximizeGrid(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new RandomSampleMove(g, gs, tree, width, height, Role.MAX, sampleParameters, gameObserver.getStrategiesStopWatch()),
        };
        return calculateCrossingsSequential(lastMove, Role.MAX, strategies);
    }

    public GameMove minimizeCrossingAngles(GameMove lastMove) {
        // TODO
        var sampleParameters = new SampleParameters(SampleSize.Keep, 10, 1);
        Strategy[] strategies = {
                //new MinimizePlaceNextToOpponent(g, gs, tree, width, height, sampleParameters),
                //new MinimizePlaceAtBorder(g, gs, tree, width, height, sampleParameters),
                new MinimizeGridAngle(g, gs, tree, vertexTree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch()),
                new RandomSampleMove(g, gs, tree, width, height, Role.MIN, sampleParameters, gameObserver.getStrategiesStopWatch()),
        };

        //return minimizeCrossings(lastMove);
        return calculateCrossingsSequential(lastMove, Role.MIN, strategies);
    }

    /**
     * This calculates the crossings based on the input role parallelized.
     *
     * @param lastMove Last GameMove
     * @param role Minimize or Maximize
     * @param strategies The strategies which should be calculated
     * @return A calculated game move
     */
    private GameMove calculateCrossings(GameMove lastMove, Role role, Strategy[] strategies) {
        gameObserver.startTimer();
        if (lastMove != null) {
            gs.applyMove(lastMove);
            gameObserver.addOpponentGameMove(lastMove);

            // Last move must have been a maximize move.
            // Therefore, export the move from opponent.
            if (enableExport) {
                try {
                    guiExport.exportVertexPlacement(lastMove, Role.MAX);
                } catch (IOException e) {
                    enableExport = false;
                }
            }
        }

        // Add lines to tree by observing last game move if not empty.
        TreeHelper.additionalLines(g, gs.getVertexCoordinates(), lastMove).ifPresent(lines -> tree.addAll(lines));

        // Add point to the vertex tree by converting the last game move
        TreeHelper.additionalPoint(lastMove).ifPresent(entry -> vertexTree.add(entry));

        // Get the estimated SampleParameters
        var sampleParameters = gameObserver.calculateSampleSizeParameters();


        var threads = new ArrayList<Thread>(4);
        for (var strat : strategies) {
            threads.add(Thread.ofVirtual().start(() -> {
                // Check if we've got the first move and must execute the heuristic
                if (gs.getPlacedVertices().isEmpty()) {
                    strat.executeHeuristic(Optional.ofNullable(lastMove));
                } else {
                    strat.executeStrategy(lastMove);
                }
            }));
        }

        // This is our fallback. If our strategy fails, return a random move
        var randomMove = new RandomMove(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch());
        randomMove.executeHeuristic(Optional.ofNullable(lastMove));

        // Calculate the game move.
        Optional<GameMove> move = randomMove.getGameMove();
        long moveQuality = randomMove.getGameMoveQuality();
        StrategyName usedStrategy = randomMove.getStrategyName();

        // Join the threads
        for (var t : threads) {
            try {
                t.join(gameObserver.getSingleGameMoveTimeThread());
            } catch (InterruptedException e) {
                // TODO Notify the game observer
            }
        }

        // Calculate best move
        for (var strat : strategies) {
            // Check the quality
            var currentMove = strat.getGameMove();
            var currentQuality = strat.getGameMoveQuality();

            if (role == Role.MAX && currentMove.isPresent() && currentQuality > moveQuality) {
                // Maximize
                moveQuality = currentQuality;
                move = currentMove;
                usedStrategy = strat.getStrategyName();
            } else if (role == Role.MIN && currentMove.isPresent() && currentQuality < moveQuality) {
                // Minimize
                moveQuality = currentQuality;
                move = currentMove;
                usedStrategy = strat.getStrategyName();
            }

        }

        gs.applyMove(move.get());
        gameObserver.addOwnGameMove(move.get(), usedStrategy);

        if (enableExport) {
            try {
                guiExport.exportVertexPlacement(move.get(), Role.MIN);
            } catch (IOException e) {
                enableExport = false;
            }
        }

        // Add our own move to the trees
        // Add lines to tree by observing last game move if not empty.
        TreeHelper.additionalLines(g, gs.getVertexCoordinates(), lastMove).ifPresent(lines -> tree.addAll(lines));

        // Add point to the vertex tree by converting the last game move
        TreeHelper.additionalPoint(lastMove).ifPresent(entry -> vertexTree.add(entry));

        gameObserver.stopTimer();

        return move.get();
    }


    /**
     * Calculate the crossings based on the given role in a sequence.
     * @param lastMove The last game move
     * @param role Minimize/Maximize
     * @param strategies Calculate the given strategies
     * @return A game move
     */
    private GameMove calculateCrossingsSequential(GameMove lastMove, Role role, Strategy[] strategies) {
        gameObserver.startTimer();
        if (lastMove != null) {
            gs.applyMove(lastMove);
            gameObserver.addOpponentGameMove(lastMove);

            // Last move must have been a maximize move.
            // Therefore, export the move from opponent.
            if (enableExport) {
                try {
                    guiExport.exportVertexPlacement(lastMove, Role.MAX);
                } catch (IOException e) {
                    enableExport = false;
                }
            }
        }

        // Add lines to tree by observing last game move if not empty.
        TreeHelper.additionalLines(g, gs.getVertexCoordinates(), lastMove).ifPresent(lines -> tree.addAll(lines));

        // Add point to the vertex tree by converting the last game move
        TreeHelper.additionalPoint(lastMove).ifPresent(entry -> vertexTree.add(entry));

        // Get the estimated SampleParameters
        var sampleParameters = gameObserver.calculateSampleSizeParameters();

        // This is our fallback. If our strategy fails, return a random move
        var randomMove = new RandomMove(g, gs, tree, width, height, sampleParameters, gameObserver.getStrategiesStopWatch());
        randomMove.executeHeuristic(Optional.ofNullable(lastMove));

        // Calculate the game move.
        Optional<GameMove> move = randomMove.getGameMove();
        long moveQuality = randomMove.getGameMoveQuality();
        StrategyName usedStrategy = randomMove.getStrategyName();

        // Calculate best move
        for (var strat : strategies) {
            // Check if we've got the first move and must execute the heuristic
            if (gs.getPlacedVertices().isEmpty()) {
                strat.executeHeuristic(Optional.ofNullable(lastMove));
            } else {
                strat.executeStrategy(lastMove);
            }

            // Check the quality
            var currentMove = strat.getGameMove();
            var currentQuality = strat.getGameMoveQuality();

            if (role == Role.MAX && currentMove.isPresent() && currentQuality > moveQuality) {
                // Maximize
                moveQuality = currentQuality;
                move = currentMove;
                usedStrategy = strat.getStrategyName();
            } else if (role == Role.MIN && currentMove.isPresent() && currentQuality < moveQuality) {
                // Minimize
                moveQuality = currentQuality;
                move = currentMove;
                usedStrategy = strat.getStrategyName();
            }

        }

        gs.applyMove(move.get());
        gameObserver.addOwnGameMove(move.get(), usedStrategy);

        if (enableExport) {
            try {
                guiExport.exportVertexPlacement(move.get(), Role.MIN);
            } catch (IOException e) {
                enableExport = false;
            }
        }

        // Add our own move to the trees
        // Add lines to tree by observing last game move if not empty.
        TreeHelper.additionalLines(g, gs.getVertexCoordinates(), lastMove).ifPresent(lines -> tree.addAll(lines));

        // Add point to the vertex tree by converting the last game move
        TreeHelper.additionalPoint(lastMove).ifPresent(entry -> vertexTree.add(entry));

        gameObserver.stopTimer();

        return move.get();
    }

    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Get the GameObserver instance
     * @return GameObserver instance
     */
    public GameObserver getGameObserver() {
        return gameObserver;
    }

}
