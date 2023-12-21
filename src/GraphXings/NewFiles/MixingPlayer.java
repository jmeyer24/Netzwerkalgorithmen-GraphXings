package GraphXings.NewFiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;

import com.github.davidmoten.rtree2.Iterables;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Data.*;

/**
 * A player performing random moves.
 */
public class MixingPlayer implements NewPlayer {
    /* ---------------- attributes -------------- */
    /**
     * The name of the Good palyer
     */
    private String name;
    /**
     * A random number generator.
     */
    private Random r;
    /**
     * The graph to be drawn.
     */
    private Graph g;
    /**
     * The current state of the game;
     */
    private GameState gs;
    /**
     * The this.width of the game board.
     */
    private int width;
    /**
     * The this.height of the game board.
     */
    private int height;
    /**
     * The Edge Crossing algorithm to be applied
     */
    private BetterEdgeCrossingRTree betterEdgeCrossingRTree;
    /**
     * The sample size of the brute force method
     */
    private int sampleSize;
    /*
     * The sample size of the vertecies
     */
    private int vertexSampleSize;
    /**
     * The percentage value with which to choose brute force over the mirror tactic
     */
    // TODO: the larger the board and number of vertices, the higher the percentage
    // TODO: description with strategies and percentages
    // for mirroring game (e.g. annealing)
    // TODO: make it so that this is calculated: percentage =
    // duration(BruteForceMove) / duration(MirroringMove)
    // TODO: alternatively: change the sample size of the brute force player part
    private double percentage;
    /**
     * Minimizer builds a tree, these are the open endpoints of this tree where we
     * can add an edge
     */
    private ArrayList<Vertex> openTreeEndpoints = new ArrayList<>();
    /**
     * The id of a vertex mapped to its vertex object
     */
    private HashMap<String, Vertex> mapIdToVertex = new HashMap<>();
    /**
     * The strategy used (see Strategy enum)
     */
    private Strategy strategy;
    /**
     * Check if we have a small game board and switch strategy
     */
    private boolean smallBoardStrategy;
    /**
     * maximium playing time in nanos
     */
    private long playingTime = 300000000000L;
    /**
     * The size of the circle to mirror to
     * ranges from 0 (center point) over 1 (this.width/this.height of game board) to
     * sqrt(2)
     * (diagonal of board)
     */
    private double relativeCircleSize;
    private ArrayList<ArrayList<Integer>> heatmap = new ArrayList<>();
    private int heatMapSize = 10;
    private int nMoveSize = 20;
    private ArrayList<Vertex> vertexHistory = new ArrayList<>();
    // private boolean enemyMirroredOnce = false;

    /* ---------------- constructor -------------- */
    /**
     * The default constructor for {@code MixingPlayer}
     */
    public MixingPlayer() {
        this.name = "Graph_Dracula";
        this.sampleSize = 30;
        this.vertexSampleSize = 1;
        this.percentage = 0.93;
        this.relativeCircleSize = 0.5;
        this.strategy = Strategy.Mirroring;
        this.r = new Random(this.name.hashCode());
    }

    /**
     * Constructs a {@code MixingPlayer} with the specified initial attribute
     * values. A name is chosen from these attributes. The Random object uses this
     * name hashed as seed.
     *
     * @param percentage         for specific strategies this gives the percentage
     *                           value (see class description)
     * @param relativeCircleSize gives the radius of the mirroring circle relative
     *                           to the this.width and this.height of the game
     *                           board, along the x- and y-axes respectively
     * @param sampleSize         the number of vertices we want to sample per
     *                           BruteForce player game move
     * @param vertexSampleSize   TODO: explain this
     * @param strategy           the strategy we choose for the player. This is
     *                           normally a combination/interpolation of mirroring
     *                           over the game board center and the brute force
     *                           method
     */
    public MixingPlayer(double percentage, double relativeCircleSize, int sampleSize, int vertexSampleSize,
            Strategy strategy) {
        // optimizable parameters
        this.percentage = percentage;
        this.relativeCircleSize = relativeCircleSize;
        this.sampleSize = sampleSize;
        this.vertexSampleSize = vertexSampleSize;
        this.strategy = strategy;

        // fixed attributes
        this.name = "Graph_Dracula_" + strategy + "_" + percentage + "_" + relativeCircleSize + "_" + sampleSize + "_"
                + vertexSampleSize;
        this.r = new Random(this.name.hashCode());
    }

    /* -------- overrides of NewPlayer methods -------- */
    @Override
    public GameMove maximizeCrossings(GameMove lastMove) {
        return makeMove(lastMove, true);
    }

    @Override
    public GameMove minimizeCrossings(GameMove lastMove) {
        return makeMove(lastMove, false);
    }

    @Override
    public void initializeNextRound(Graph g, int width, int height, Role role) {
        this.g = g;
        this.width = width;
        this.height = height;
        if (this.height < this.heatMapSize || this.width < this.heatMapSize) {
            this.heatMapSize = 1;
        }
        this.gs = new GameState(g, this.width, this.height);
        this.betterEdgeCrossingRTree = new BetterEdgeCrossingRTree(g);
        this.heatmap = new ArrayList<ArrayList<Integer>>();
        this.vertexHistory = new ArrayList<>();
        for (int i = 0; i < heatMapSize; i++) {
            this.heatmap.add(new ArrayList<Integer>(Collections.nCopies(heatMapSize, 0)));
        }

        for (Vertex vertex : g.getVertices()) {
            this.mapIdToVertex.put(vertex.getId(), vertex);
        }
        if (this.width * this.height < 10000) {
            this.smallBoardStrategy = true;
            this.sampleSize = g.getN();
            this.vertexSampleSize = 3;
        } else {
            this.smallBoardStrategy = false;
            this.sampleSize = 500;
            this.vertexSampleSize = 1;
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    /* ---------------- public methods -------------- */
    /**
     * Return a valid game move. For that, apply the last game move by the opponent,
     * get a valid game move and finally apply that as well.
     * 
     * @param lastMove the last move made by the opponent, {@code null} if it is the
     *                 first move of the game
     * @param maximize boolean containing information about the player's objective
     *                 (minimizing or maximizing)
     * @return a valid game move
     * @apiNote Applying a game move incorporates adding it to the local game state
     *          of the player as well as its decision structures (heatmap, fifo).
     */
    public GameMove makeMove(GameMove lastMove, boolean maximize) {
        // First: Apply the last move by the opponent to the local GameState (and the
        // Crossing Calculator)
        if (lastMove != null) {
            gs.applyMove(lastMove);
            updateHeatmap(lastMove);
            updateVertexHistory(lastMove);
            this.betterEdgeCrossingRTree.insertVertex(lastMove.getVertex());
        }

        // Second: Compute the new move
        GameMove newMove = getMove(lastMove, maximize);

        // Third: Apply the new move to the local GameState (and the Crossing
        // Calculator)
        gs.applyMove(newMove);
        updateHeatmap(newMove);
        updateVertexHistory(newMove);
        this.betterEdgeCrossingRTree.insertVertex(newMove.getVertex());

        // Finally: Return the new move
        return newMove;
    }

    /**
     * Maintain the history of recent move vertices {@code this.vertexHistory} with
     * a maximal history size of {@code this.nMoveSize}
     * 
     * @param vertex the vertex to store in the queue
     */
    private void updateVertexHistory(GameMove move) {
        if (this.vertexHistory.size() >= this.nMoveSize) {
            this.vertexHistory.remove(0);
        }
        this.vertexHistory.add(move.getVertex());
    }

    /**
     * Add the (float) coordinate of the move to the (integer) heatmap by
     * incrementing at the respective index
     * 
     * @param move the move with the coordinate to increment the heatmap at
     */
    public void updateHeatmap(GameMove move) {
        int heatMapX = (int) ((double) move.getCoordinate().getX() / this.width * heatMapSize);
        int heatMapY = (int) ((double) move.getCoordinate().getY() / this.height * heatMapSize);
        this.heatmap.get(heatMapX).set(heatMapY, this.heatmap.get(heatMapX).get(heatMapY) + 1);
    }

    /**
     * Return a valid game move in the current game state.
     * See the {@code enum Strategies} for the list of maximizing strategies.
     * Minimizing is currently done by a single strategy, see
     * {@code getMinimizingMove()}.
     * 
     * @param lastMove the last move made by the opponent, {@code null} if it is the
     *                 first move of the game
     * @param maximize boolean containing information about the player's objective
     *                 (minimizing or maximizing)
     * @return a valid game move aiding the player objective
     */
    public GameMove getMove(GameMove lastMove, boolean maximize) {
        // if we have a small game board, just use the brute force method
        if (this.smallBoardStrategy) {
            return getBruteForceMove(lastMove, maximize);
        }
        // else get a move according to the current strategy
        if (maximize) {
            switch (strategy) {
                case BruteForce:
                    return getBruteForceMove(lastMove, maximize);
                case Mirroring:
                    return getMirroringMove(lastMove);
                case Percentage:
                    if (this.r.nextDouble() < percentage) {
                        return getBruteForceMove(lastMove, maximize);
                    } else {
                        return getMirroringMove(lastMove);
                    }
                case Annealing:
                    if ((double) gs.getPlacedVertices().size() / this.g.getN() < percentage) {
                        return getMirroringMove(lastMove);
                    } else {
                        return getBruteForceMove(lastMove, maximize);
                    }
                case AnnealingReverse:
                    if ((double) gs.getPlacedVertices().size() / this.g.getN() < percentage) {
                        return getBruteForceMove(lastMove, maximize);
                    } else {
                        return getMirroringMove(lastMove);
                    }
                default:
                    return getRandomMove();
            }
        } else {
            return getMinimizingMove(lastMove);
        }
    }

    /**
     * Computes a random valid move, e.g. an unplaced vertex at a free coordinate.
     * 
     * @return a random valid move
     */
    private GameMove getRandomMove() {
        int stillToBePlaced = this.g.getN() - gs.getPlacedVertices().size();
        int next = this.r.nextInt(stillToBePlaced);
        int skipped = 0;
        Vertex v = null;
        for (Vertex u : this.g.getVertices()) {
            if (!gs.getPlacedVertices().contains(u)) {
                if (skipped < next) {
                    skipped++;
                    continue;
                }
                v = u;
                break;
            }
        }
        Coordinate c;
        do {
            c = new Coordinate(this.r.nextInt(this.width), this.r.nextInt(this.height));
        } while (gs.getUsedCoordinates()[c.getX()][c.getY()] != 0);
        return new GameMove(v, c);
    }

    /**
     * Return a valid game move aiding the objective of minimizing.
     * 
     * @param lastMove the last move made by the opponent, {@code null} if it is the
     *                 first move of the game
     * @return a valid game move
     */
    public GameMove getMinimizingMove(GameMove lastMove) {
        // If the enemy tries to counter our method by always placing our neighbours we
        // return to random playing

        // TODO: Add check for enemyMirroredOnce if neccessary
        // if (!this.vertexHistory.isEmpty() && enemyStealsNeighbor()) {
        // if (enemyMirrors()) {
        // this.openTreeEndpoints = new ArrayList<>();
        // // enemyMirroredOnce = true;
        // return treeMinimizer(lastMove, new Coordinate(this.width / 2, this.height /
        // 2),
        // this.width /
        // 15, this.height / 15);
        // }
        // return getBruteForceMove(lastMove, false);
        // }

        Coordinate treeCenter = new Coordinate(this.width / 2, this.height / 2);
        return treeMinimizer(lastMove, treeCenter, this.width, this.height);
    }

    /**
     * Checks if the enemy does mirror around the center of the game board.
     * TODO: this needs to be redone!
     * 
     * @return {@code true} if the enemy mirrors, {@code false} else
     */
    public boolean enemyMirrors() {
        int mirroredMoves = 0;
        for (Vertex vertex : this.vertexHistory.subList(1, this.vertexHistory.size())) {
            Coordinate oldCoordinate = gs.getVertexCoordinates().get(this.vertexHistory.get(0));
            Coordinate newCoordinate = gs.getVertexCoordinates().get(vertex);
            double distance = Math.sqrt(Math.pow(oldCoordinate.getX() - newCoordinate.getX(), 2)
                    + Math.pow(oldCoordinate.getY() - newCoordinate.getY(), 2));
            if (distance > (this.height > this.width ? this.height / 2
                    : this.width / 2)) {
                mirroredMoves++;
            }
        }
        return (mirroredMoves > this.vertexHistory.size() / 2);
    }

    /**
     * Return a valid game move by using the strategy of building a tree structure
     * at the border of the game board. If this does not work out, fallback to brute
     * force method.
     * TODO: strategy explanation?
     * 
     * @param lastMove   the last move made by the opponent, {@code null} if it is
     *                   the first move of the game. Used for fallback brute force
     *                   method
     * @param treeWidth  the width the tree should be build with
     * @param treeHeight the height the tree should be build with
     * @return a valid game move
     */
    public GameMove treeMinimizer(GameMove lastMove, Coordinate center, int treeWidth, int treeHeight) {
        Vertex vertexToPlace = null;
        GameMove newMove = null;
        int[][] usedCoordinates = gs.getUsedCoordinates();

        int minX = center.getX() - treeWidth / 2;
        if (minX < 0)
            minX = 0;
        int maxX = center.getX() + treeWidth / 2 + treeWidth % 2;
        if (maxX > this.width)
            maxX = this.width;
        int minY = center.getY() - treeHeight / 2;
        if (minY < 0)
            minY = 0;
        int maxY = center.getY() + treeHeight / 2 + treeHeight % 2;
        if (maxY > this.height)
            maxY = this.height;

        if (!this.openTreeEndpoints.isEmpty()) {
            // System.out.println("lastOwnMove != null");
            ArrayList<Vertex> unplacedNeighbors = new ArrayList<>();
            Vertex referenceVertex = null;
            ArrayList<Vertex> usedUpVertices = new ArrayList<>();
            for (Vertex referenceVertex_ : this.openTreeEndpoints) {
                unplacedNeighbors = getUnplacedNeighbors(referenceVertex_);
                if (!unplacedNeighbors.isEmpty()) {
                    referenceVertex = referenceVertex_;
                    break;
                } else {
                    usedUpVertices.add(referenceVertex_);
                }
            }
            for (Vertex vertexToRemove : usedUpVertices) {
                this.openTreeEndpoints.remove(vertexToRemove);
            }
            if (!unplacedNeighbors.isEmpty()) {
                vertexToPlace = unplacedNeighbors.get(unplacedNeighbors.size() - 1);
                int lastX = gs.getVertexCoordinates().get(referenceVertex).getX();
                int lastY = gs.getVertexCoordinates().get(referenceVertex).getY();
                // System.out.println(lastX + " " + lastY);
                switch (findClosestBoardEdge(lastX, lastY)) {
                    case Bottom:
                        while (true) {
                            if (lastX > 0 && usedCoordinates[lastX - 1][lastY] == 0) {
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX - 1, lastY));
                                break;
                            } else if (lastY > 0 && usedCoordinates[lastX][lastY - 1] == 0) {
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX, lastY - 1));
                                break;
                            } else {
                                lastX--;
                                lastY--;
                                if (lastX < minX || lastY < minY)
                                    break;
                            }
                        }
                        break;
                    case Left:
                        while (true) {
                            if (lastY > 0 && usedCoordinates[lastX][lastY - 1] == 0) {
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX, lastY - 1));
                                break;
                            } else if (lastX < this.width - 1 && usedCoordinates[lastX + 1][lastY] == 0) {
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX + 1, lastY));
                                break;
                            } else {
                                lastX++;
                                lastY--;
                                if (lastX > maxX - 1 || lastY < minY)
                                    break;
                            }
                        }
                        break;
                    case Top:
                        while (true) {
                            // System.out.print(lastX + " " + lastY);
                            if (lastX < this.width - 1 && usedCoordinates[lastX + 1][lastY] == 0) {
                                // System.out.println("if");
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX + 1, lastY));
                                break;
                            } else if (lastY < this.height - 1 && usedCoordinates[lastX][lastY + 1] == 0) {
                                // System.out.println("elseif");
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX, lastY + 1));
                                break;
                            } else {
                                // System.out.println("else");
                                lastX++;
                                lastY++;
                                if (lastX > maxX - 1 || lastY > maxY - 1)
                                    break;
                            }
                        }
                        break;
                    case Right:
                        while (true) {
                            if (lastY < this.height - 1 && usedCoordinates[lastX][lastY + 1] == 0) {
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX, lastY + 1));
                                break;
                            } else if (lastX > 0 && usedCoordinates[lastX - 1][lastY] == 0) {
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX - 1, lastY));
                                break;
                            } else {
                                lastX--;
                                lastY++;
                                if (lastX < minX || lastY > maxY - 1)
                                    break;
                            }
                        }
                        break;
                }
            }
        }
        // We either have no real last move or no neighbor for our lastmove
        if (newMove == null) {
            for (Vertex vertex : g.getVertices()) {
                if (!gs.getPlacedVertices().contains(vertex)) {
                    vertexToPlace = vertex;
                    break; // There should always be a valid vertex here
                }
            }
            int circumference = treeWidth * 2 + treeHeight * 2 - 4;
            int fieldID = this.r.nextInt(circumference);
            int dynamicTreeWidth = treeWidth;
            int dynamicTreeHeight = treeHeight;
            int x = 0;
            int y = 0;
            for (int idx = 0; idx < g.getN() / 10; idx++) {// TODO /10 might need some adjustment
                // Place on top row (ID 0-9)
                // Place on right column (ID 10-18)
                // Place on bottom row (ID 19-27)
                // Place on left column (ID 28-35)
                if (fieldID < dynamicTreeWidth) {
                    x = fieldID + minX;
                    y = minY;
                } else if (fieldID < dynamicTreeWidth + dynamicTreeHeight - 1) {
                    x = maxX - 1;
                    y = minY + (fieldID - dynamicTreeWidth) + 1;
                } else if (fieldID < dynamicTreeWidth * 2 + dynamicTreeHeight - 2) {
                    x = maxX - 1 - (fieldID - dynamicTreeWidth - dynamicTreeHeight + 2);
                    y = maxY - 1;
                } else if (fieldID < circumference) {
                    x = minX;
                    y = maxY - 1 - (fieldID - 2 * dynamicTreeWidth - dynamicTreeHeight + 3);
                } else {
                    break;
                }
                if (x > usedCoordinates.length - 1 || y > usedCoordinates[0].length - 1 || x < 0 || y < 0)
                    break;
                // check if it is an unplaced position
                if (usedCoordinates[x][y] == 0) {
                    newMove = new GameMove(vertexToPlace, new Coordinate(x, y));
                    break;
                }
                fieldID++;
                if (fieldID >= circumference) {
                    fieldID = 0;
                    minX++;
                    minY++;
                    maxX--;
                    maxY--;
                    dynamicTreeHeight = dynamicTreeHeight - 2;
                    dynamicTreeWidth = dynamicTreeWidth - 2;
                    circumference = dynamicTreeHeight * 2 + dynamicTreeWidth * 2 - 4;
                }
            }
        }
        if (newMove != null && vertexToPlace != null) {
            this.openTreeEndpoints.add(vertexToPlace);
            return newMove;
        }

        // fallback: get a minimizing brute force move instead
        return getBruteForceMove(lastMove, false);
    }

    /**
     * Return the {@code enum BoardEdge} closest to the input position.
     * 
     * @param x position in x-direction
     * @param y position in y-direction
     * @return a value of the {@code enum BoardEdge}
     */
    public BoardEdge findClosestBoardEdge(int x, int y) {
        if (x < y) {
            if (this.height - y < x)
                return BoardEdge.Bottom;
            return BoardEdge.Left;
        } else {
            if (y < this.width - x)
                return BoardEdge.Top;
            return BoardEdge.Right;
        }
    }

    /**
     * Return a valid game move that was found by (semi) brute force.
     * 
     * @param lastMove the last move made by the opponent, {@code null} if it is the
     *                 first move of the game
     * @param maximize boolean containing information about the player's objective
     *                 (minimizing or maximizing)
     * @return a valid game move
     * @apiNote Sample a number of vertices from the neighborhood of the last placed
     *          vertex. For each of them check all free coordinates on the game
     *          board and calculate the number of crossings that vertex at this free
     *          coordinate would produce.
     */
    public GameMove getBruteForceMove(GameMove lastMove, boolean maximize) {
        long startTime = System.nanoTime();
        Vertex v = null;
        ArrayList<Vertex> vertices = new ArrayList<>();

        // get the last moves unplaced neighbors
        if (lastMove != null)
            vertices = getUnplacedNeighbors(lastMove.getVertex());
        // and (if there is at least one) take the one that has itself again the most
        // unplaced neighbors
        if (!vertices.isEmpty()) {
            v = findBestVertex(vertices, maximize);
        } else {
            for (Vertex v_ : this.g.getVertices()) {
                if (!gs.getPlacedVertices().contains(v_)) {
                    v = v_;
                    break;
                }
            }
        }

        // get random vertex samples and v in a list
        List<Vertex> trimmedVertices = new ArrayList<>();
        if (vertexSampleSize > 1) {
            List<Vertex> allVertices = new ArrayList<>();
            for (Vertex vertex : this.g.getVertices()) {
                if (!gs.getPlacedVertices().contains(vertex)) {
                    allVertices.add(vertex);
                }
            }
            Collections.shuffle(allVertices);
            trimmedVertices = new ArrayList<>(
                    allVertices.subList(0, Math.min(allVertices.size(), vertexSampleSize - 1)));
            trimmedVertices.add(v);
        }

        // Find optimal heatmap square
        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        int minRow = -1, minCol = -1, maxRow = -1, maxCol = -1;

        for (int i = 0; i < this.heatmap.size(); i++) {
            for (int j = 0; j < this.heatmap.get(i).size(); j++) {
                int val = this.heatmap.get(i).get(j);
                if (val < minVal) {
                    minVal = val;
                    minRow = j;
                    minCol = i;
                }
                if (val > maxVal) {
                    maxVal = val;
                    maxRow = j;
                    maxCol = i;
                }
            }
        }

        int bestSquareX = maximize ? minCol : maxCol;
        int bestSquareY = maximize ? minRow : maxRow;

        ArrayList<Integer> xPositions = new ArrayList<>();
        ArrayList<Integer> yPositions = new ArrayList<>();

        // Create sample set based on the lowest/highest vertex density area
        // Using abort in case of areas being completly filled, chose a reasonable(?)
        // value
        for (int sample = 0, abort = 0; sample < sampleSize / 2 && abort < 200; sample++, abort++) {
            int x = this.r.nextInt(this.width / heatMapSize) + bestSquareX * (this.width / heatMapSize);
            int y = this.r.nextInt(this.height / heatMapSize) + bestSquareY * (this.height / heatMapSize);
            if (gs.getUsedCoordinates()[x][y] != 0) {
                sample--;
            } else {
                xPositions.add(x);
                yPositions.add(y);
            }
        }

        // Create random sample set of possible placing positions of the current vertex
        // v
        int verticesToAdd = sampleSize - xPositions.size();
        for (int sample = 0; sample < verticesToAdd; sample++) {
            int x = this.r.nextInt(this.width);
            int y = this.r.nextInt(this.height);
            if (gs.getUsedCoordinates()[x][y] != 0) { // The random coordinate is already taken
                sample--;
            } else {
                xPositions.add(x);
                yPositions.add(y);
            }
        }

        // Number of crossings before we place the vertex
        int bestTotalCrossingsByVertex = maximize ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        VertexSamplePair bestPair = null;

        // Find the best position (maximizing/minimizing crossings) we can place vertex
        // v at
        for (int sample = 0; sample < sampleSize; sample++) {
            // System.out.println(sample);
            // Adding 10% to make sure we do not run out of time
            if (1.1 * (System.nanoTime() - startTime) > playingTime / (this.g.getN() / 2)) {
                // System.out.println("checked " + sample + " samples");
                break;
            }

            List<Vertex> verticesToSample = (vertexSampleSize == 1) ? Collections.singletonList(v) : trimmedVertices;

            for (Vertex vertexToSample : verticesToSample) {
                // Add vertex sampling here
                Coordinate coordinateToAdd = new Coordinate(xPositions.get(sample), yPositions.get(sample));
                int crossingsAddedByVertex = this.betterEdgeCrossingRTree.testCoordinate(vertexToSample,
                        coordinateToAdd,
                        gs.getVertexCoordinates());
                // ??????? Error wenn mehr als ein Vertex abgefragt wird.
                if (maximize ? crossingsAddedByVertex > bestTotalCrossingsByVertex
                        : crossingsAddedByVertex < bestTotalCrossingsByVertex) {
                    bestPair = new VertexSamplePair(vertexToSample, sample);
                    bestTotalCrossingsByVertex = crossingsAddedByVertex;
                }
            }
        }

        Coordinate coordinateToAdd = new Coordinate(xPositions.get(bestPair.getSample()),
                yPositions.get(bestPair.getSample()));
        HashMap<Vertex, Coordinate> mapVertexToCoordinate = gs.getVertexCoordinates();
        mapVertexToCoordinate.put(bestPair.vertex, coordinateToAdd);
        this.betterEdgeCrossingRTree.insertAllCoordinates(gs.getPlacedVertices());

        return new GameMove(v, coordinateToAdd);
    }

    /**
     * Return the vertex that has the best requirements for the player objective.
     * 
     * @param vertices a list of unplaced vertices from which we want to find the
     *                 one with most (un)placed neighbors
     * @param maximize boolean containing information about the player's objective
     *                 (minimizing or maximizing)
     * @return the best vertex
     * @apiNote 'Best' is here defined as having the most placed neighbors when
     *          maximizing and the most unplaced neighbors when minimizing.
     */
    public Vertex findBestVertex(List<Vertex> vertices, boolean maximize) {
        Vertex currentBest = vertices.remove(0);
        int p = getPlacedNeighbors(currentBest).size();
        int u = getUnplacedNeighbors(currentBest).size();
        assert u + p == Iterables.size(this.g.getIncidentEdges(currentBest));

        for (Vertex vertex : vertices) {
            int v = getUnplacedNeighbors(vertex).size();

            if (maximize) {
                // For maximize, find the vertex with the most placed neighbors (that's
                // different from least unplaced neighbors!)
                if (v > p) {
                    currentBest = vertex;
                    p = getPlacedNeighbors(currentBest).size();
                }
            } else {
                // For minimize, find the vertex with the most unplaced neighbors
                if (v > u) {
                    currentBest = vertex;
                    u = getUnplacedNeighbors(currentBest).size();
                }
            }
        }

        return currentBest;
    }

    /**
     * Return a valid game move that was found by mirroring around the center.
     * 
     * @param lastMove the last move made by the opponent, {@code null} if it is
     *                 the first move of the game.
     * @return a valid game move
     * @apiNote Place the vertex mirrored around the center point of the board on an
     *          ellipsis whose major and minor axes are are given by
     *          {@code this.relativeCircleSize * this.width} and
     *          {@code ... * this.height} respectively. If this coordinate is
     *          occupied, use a near neighboring coordinate instead.
     */
    public GameMove getMirroringMove(GameMove lastMove) {
        // if it's the first move, take the first vertex and place it on the circle
        // right side, it's as good as any
        // TODO: is it if the board is not square??
        if (lastMove == null) {
            double x = this.width * (0.5 + relativeCircleSize);
            double y = this.height * 0.5;
            Coordinate c = getCoordinateClampedToBoard(x, y);
            return new GameMove(this.g.getVertices().iterator().next(), c);
        }

        // TODO: alternatively: get a good vertex (disregard last move)
        // get a good vertex, either one with 2 or 3 fixed neighbors or one that might
        // enable such a 2, 3 fixed one for oneself not the enemy don't enable the enemy
        // by fixing a second or third neighbor
        // Vertex v = getGoodVertex();
        // TODO: optimize this
        // choose the best unplaced neighbor from the lastMove's vertex
        ArrayList<Vertex> neighbors = getUnplacedNeighbors(lastMove.getVertex());

        // return another random move if all neighbors are placed already
        if (neighbors.isEmpty()) {
            return getRandomMove();
        }
        // get the one with the most placed neighbors
        // it will draw more fixed edges and we should optimize this
        Vertex v = null;
        int maxNeighborsPlaced = -1;
        for (Vertex v_ : neighbors) {
            int n_ = (int) StreamSupport.stream(this.g.getIncidentEdges(v_).spliterator(), false).count()
                    - getUnplacedNeighbors(v_).size();
            if (n_ > maxNeighborsPlaced) {
                v = v_;
                maxNeighborsPlaced = n_;
            }
        }

        // mirror around the center onto the circle of highest probabilty
        // get the unit vector from the lastMove vertex towards the center
        UnitVector uvec = new UnitVector(this.width / 2.0 - lastMove.getCoordinate().getX(),
                this.height / 2.0 - lastMove.getCoordinate().getY());
        // get the position on the highest probability circle
        double x = this.width * (0.5 + uvec.getX() * relativeCircleSize);
        double y = this.height * (0.5 + uvec.getY() * relativeCircleSize);
        return new GameMove(v, getNearestFreeCoordinate(getCoordinateClampedToBoard(x, y)));
    }

    /**
     * Return a list of unplaced neighbors.
     * 
     * @param vertex the vertex to get the unplaced neighbors to
     * @return the list of unplaced neighbors
     */
    public ArrayList<Vertex> getUnplacedNeighbors(Vertex vertex) {
        ArrayList<Vertex> neighbors = new ArrayList<>();
        if (this.g.getIncidentEdges(vertex) == null) {
            return neighbors;
        }
        for (Edge edge : this.g.getIncidentEdges(vertex)) {
            Vertex vertexToAdd = vertex.equals(edge.getS()) ? edge.getT() : edge.getS();
            if (!gs.getPlacedVertices().contains(vertexToAdd)) {
                neighbors.add(vertexToAdd);
            }
        }
        return neighbors;
    }

    /**
     * Return a list of placed neighbors.
     * 
     * @param vertex the vertex to get the placed neighbors to
     * @return the list of placed neighbors
     */
    public ArrayList<Vertex> getPlacedNeighbors(Vertex vertex) {
        ArrayList<Vertex> neighbors = new ArrayList<>();
        if (this.g.getIncidentEdges(vertex) == null) {
            return neighbors;
        }
        for (Edge edge : this.g.getIncidentEdges(vertex)) {
            Vertex vertexToAdd = vertex.equals(edge.getS()) ? edge.getT() : edge.getS();
            if (gs.getPlacedVertices().contains(vertexToAdd)) {
                neighbors.add(vertexToAdd);
            }
        }
        return neighbors;
    }

    /**
     * Return a free coordinate given a possibly occupied coordinate.
     * 
     * @param coordinate the coordinate we want to get as close a free coordinate to
     * @return a free coordinate
     * @apiNote The nearest free coordinate is checked in a spiral fashion if the
     *          coordinates have the same distance.
     */
    public Coordinate getNearestFreeCoordinate(Coordinate coordinate) {
        int x = coordinate.getX();
        int y = coordinate.getY();
        // if position is not taken yet, return it
        if (gs.getUsedCoordinates()[x][y] == 0) {
            return new Coordinate(x, y);
        }
        // else get a neighboring position
        int i = 1;
        while (true) {
            for (int j = -i; j <= i; j++) {
                if ((x + j) >= 0 && (x + j) < this.width) {
                    for (int k = -i; k <= i; k++) {
                        if (Math.abs(j) == i || Math.abs(k) == i) {
                            if ((y + k) >= 0 && (y + k) < this.height) {
                                if (gs.getUsedCoordinates()[x + j][y + k] == 0) {
                                    return new Coordinate(x + j, y + k);
                                }
                            }
                        }
                    }
                }
            }
            i++;
        }
    }

    /**
     * Return a valid board coordinate.
     * 
     * @param x (double) position in x-direction
     * @param y (double) position in y-direction
     * @return a (integer) coordinate on the game board
     */
    public Coordinate getCoordinateClampedToBoard(double x, double y) {
        // simple: make sure its in bounds of the game board
        // int a = Math.max(Math.min(roundToClosestInteger(x), this.width-1), 0);
        // int b = Math.max(Math.min(roundToClosestInteger(y), this.height-1), 0);
        // advanced: do it in the direction of the center of the board
        if (!(x >= 0 && x < this.width && y >= 0 && y < this.height)) {
            UnitVector uvec = new UnitVector(this.width / 2.0 - x, this.height / 2.0 - y);
            double alpha = 0.0;
            if (x < 0) {
                alpha = -x / uvec.getX();
            } else if (x > this.width - 1) {
                // this.width-1 = x + c * uvec.getX()
                // c = (this.width-1 - x) / uvec.getX()
                alpha = (this.width - 1 - x) / uvec.getX();
            }
            x += alpha * uvec.getX();
            y += alpha * uvec.getY();

            if (y < 0) {
                alpha = -y / uvec.getY();
            } else if (y > this.height - 1) {
                alpha = (this.height - 1 - y) / uvec.getY();
            }
            x += alpha * uvec.getX();
            y += alpha * uvec.getY();
        }

        int a = roundToClosestInteger(x);
        int b = roundToClosestInteger(y);

        // check if the coordinate is actually on the board
        assert a >= 0 : "1";
        assert a < this.width : "2";
        assert b >= 0 : "3";
        assert b < this.height : "4";

        return new Coordinate(a, b);
    }

    /**
     * Round the double value to the closest integer. This works for negative values
     * as well.
     * 
     * @param value double value to round
     * @return the closest integer value
     */
    public int roundToClosestInteger(double value) {
        if (value < 0) {
            return (int) Math.ceil(value);
        }
        return (int) Math.floor(value);
    }

    /**
     * An enum describing the strategy used
     * 
     * BruteForce: always choose the BruteForceMove
     * Mirroring: always choose the MirroringMove
     * Percentage: given a percentage choose the getBruteForceMove randomly
     * Annealing: given a percentage choose the first turns to be getMirroringMove
     * (e.g. span lines across center first, then getBruteForceMove choice)
     * AnnealingReverse: given a percentage choose the first turns to be
     * getBruteForceMove
     */
    public enum Strategy {
        BruteForce, Mirroring, Percentage, Annealing, AnnealingReverse
    };

    /**
     * An enum describing the four edges/borders of the game board
     */
    public enum BoardEdge {
        Top, Right, Bottom, Left
    }
}
