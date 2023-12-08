package GraphXings.NewFiles;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.Iterator;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Data.*;

/**
 * A player performing random moves.
 */
public class MixingPlayer implements NewPlayer {
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
     * The width of the game board.
     */
    private int width;
    /**
     * The height of the game board.
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
    /**
     * The percentage value with which to choose brute force over the mirror tactic
     */
    // TODO: the larger the field and number of vertices, the higher the percentage
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
    private HashMap<String, Vertex> mapIdToVertex = new HashMap<String, Vertex>();
    /**
     * The strategy used (see Strategy enum)
     */
    private Strategy strategy;
    /**
     * The size of the circle to mirror to
     * ranges from 0 (center point) over 1 (width/height of field) to sqrt(2)
     * (diagonal of field)
     */
    private double relativeCircleSize;

    private ArrayList<ArrayList<Integer>> heatMap = new ArrayList<ArrayList<Integer>>();;
    private int heatMapSize = 10;
    private int nMovesSize = 20;
    private ArrayList<Vertex> lastNVertices = new ArrayList<>();
    // private boolean enemyMirroredOnce = false;

    /**
     * Default constructor
     */
    public MixingPlayer() {
        this.name = "Graph_Dracula";
        this.sampleSize = 30;
        this.percentage = 0.93;
        this.relativeCircleSize = 0.5;
        this.strategy = Strategy.Mirroring;
        this.r = new Random(name.hashCode());
    }

    /**
     * optimizes the given parameters
     */
    public MixingPlayer(double percentage, double relativeCircleSize, int sampleSize, Strategy strategy,
            boolean writeToFile) {
        // optimizable parameters
        this.percentage = percentage;
        this.relativeCircleSize = relativeCircleSize;
        this.sampleSize = sampleSize;
        this.strategy = strategy;

        // fixed attributes
        this.name = "Graph_Dracula_" + strategy + percentage + "_" + relativeCircleSize + "_" + sampleSize;
        this.r = new Random(this.name.hashCode());
        if (writeToFile) {
            writeCircleSizeToFile();
        }
    }

    @Override
    public GameMove maximizeCrossings(GameMove lastMove) {
        return makeMove(lastMove, true);
    }

    @Override
    public GameMove minimizeCrossings(GameMove lastMove) {
        return makeMove(lastMove, false);
    }

    public void addToHeatmap(Coordinate coordinate) {
        int heatMapX = (int) ((double) coordinate.getX() / width * heatMapSize);
        int heatMapY = (int) ((double) coordinate.getY() / height * heatMapSize);
        heatMap.get(heatMapX).set(heatMapY, heatMap.get(heatMapX).get(heatMapY) + 1);
    }

    public GameMove makeMove(GameMove lastMove, boolean maximize) {
        // First: Apply the last move by the opponent to the local GameState (and the
        // Crossing Calculator)
        if (lastMove != null) {
            gs.applyMove(lastMove);
            addToHeatmap(lastMove.getCoordinate());
            updateFiFo(lastMove);
            betterEdgeCrossingRTree.insertVertex(lastMove.getVertex());
        }
        // Second: Compute the new move.
        GameMove newMove = getMove(lastMove, maximize);
        // Third: Apply the new move to the local GameState (and the Crossing
        // Calculator)
        gs.applyMove(newMove);
        betterEdgeCrossingRTree.insertVertex(newMove.getVertex());
        addToHeatmap(newMove.getCoordinate());
        updateFiFo(newMove);
        // Finally: Return the new move.
        return newMove;
    }

    public GameMove getMove(GameMove lastMove, boolean maximize) {
        if (maximize) {
            // double progress = (double) gs.getPlacedVertices().size() / g.getN();
            // System.out.print(progress);
            switch (strategy) {
                case BruteForce:
                    return getBruteForceMove(maximize, lastMove);
                case Mirroring:
                    return getMirroringMove(lastMove);
                case Percentage:
                    if (r.nextDouble() < percentage) {
                        return getBruteForceMove(maximize, lastMove);
                    } else {
                        return getMirroringMove(lastMove);
                    }
                case Annealing:
                    if ((double) gs.getPlacedVertices().size() / g.getN() < percentage) {
                        return getMirroringMove(lastMove);
                    } else {
                        return getBruteForceMove(maximize, lastMove);
                    }
                case AnnealingReverse:
                    if ((double) gs.getPlacedVertices().size() / g.getN() < percentage) {
                        return getBruteForceMove(maximize, lastMove);
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

    private void updateFiFo(GameMove moveToAdd) {
        if (lastNVertices.size() >= nMovesSize) {
            lastNVertices.remove(0);
        }
        lastNVertices.add(moveToAdd.getVertex());
    }

    private Boolean enemyStealsNeighbor() {
        int neighborsStolen = 0;
        for (Vertex vertex : lastNVertices) {
            if (g.getIncidentEdges(vertex) == null) {
                continue;
            }
            Iterator<Edge> edges = g.getIncidentEdges(vertex).iterator();
            while (edges.hasNext()) {
                Edge edge = edges.next();
                Vertex vertexToCheck = edge.getS() != vertex ? edge.getS() : edge.getT();
                if (lastNVertices.contains(vertexToCheck)) {
                    neighborsStolen++;
                }
            }
        }
        if (neighborsStolen > (nMovesSize / 1)) {
            return true;
        }
        return false;
    }

    public GameMove treeMinimizer(GameMove lastMove, Coordinate center, int treeWidth, int treeHeight) {
        Vertex vertexToPlace = null;
        GameMove newMove = null;
        int[][] usedCoordinates = gs.getUsedCoordinates();
        int minX = center.getX() - treeWidth / 2;
        if (minX < 0)
            minX = 0;
        int maxX = center.getX() + treeWidth / 2 + treeWidth % 2;
        if (maxX > width)
            maxX = width;
        int minY = center.getY() - treeHeight / 2;
        if (minY < 0)
            minY = 0;
        int maxY = center.getY() + treeHeight / 2 + treeHeight % 2;
        if (maxY > height)
            maxY = height;
        if (openTreeEndpoints.size() > 0) {
            // System.out.println("lastOwnMove != null");
            ArrayList<Vertex> unplacedNeighbors = new ArrayList<>();
            Vertex referenceVertex = null;
            ArrayList<Vertex> usedUpVertices = new ArrayList<>();
            for (Vertex referenceVertex_ : openTreeEndpoints) {
                unplacedNeighbors = getUnplacedNeighbors(referenceVertex_);
                if (unplacedNeighbors.size() > 0) {
                    referenceVertex = referenceVertex_;
                    break;
                } else {
                    usedUpVertices.add(referenceVertex_);
                }
            }
            for (Vertex vertexToRemove : usedUpVertices) {
                openTreeEndpoints.remove(vertexToRemove);
            }
            if (unplacedNeighbors.size() > 0) {
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
                            } else if (lastX < width - 1 && usedCoordinates[lastX + 1][lastY] == 0) {
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
                            if (lastX < width - 1 && usedCoordinates[lastX + 1][lastY] == 0) {
                                // System.out.println("if");
                                newMove = new GameMove(vertexToPlace, new Coordinate(lastX + 1, lastY));
                                break;
                            } else if (lastY < height - 1 && usedCoordinates[lastX][lastY + 1] == 0) {
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
                            if (lastY < height - 1 && usedCoordinates[lastX][lastY + 1] == 0) {
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
            // System.out.println("newMove == null");
            int midpointID = getLargestGapMidpointID();
            vertexToPlace = mapIdToVertex.get(Integer.toString(midpointID));
            int circumference = treeWidth * 2 + treeHeight * 2 - 4;
            int fieldID = midpointID % circumference; // Basically map a vertex to a distinct field vertex one
                                                      // placed on 0, 0. Vertex two placed on 0, 1. ....
            int dynamicTreeWidth = treeWidth;
            int dynamicTreeHeight = treeHeight;
            int x = 0;
            int y = 0;
            for (int idx = 0; idx < mapIdToVertex.size(); idx++) {
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
            // if (newMove != null && !gs.checkMoveValidity(newMove)) {
            // System.out.println("bad");
            // }
        }
        if (newMove != null && vertexToPlace != null) {
            // System.out.println("newMove != null");
            // System.out.println(newMove.getCoordinate().getX() + " " +
            // newMove.getCoordinate().getY());
            openTreeEndpoints.add(vertexToPlace);
            return newMove;
        }
        // System.out.println("getBruteForce");

        // Found no easy move, do some random stuff and try again
        return getBruteForceMove(false, lastMove); // Found no easy move, do some random stuff and try again
    }

    public boolean enemyMirrors() {
        int mirroredMoves = 0;
        ArrayList<Vertex> lastNCopy = lastNVertices;
        Vertex oldVertex = lastNCopy.remove(0);
        HashMap<Vertex, Coordinate> coordinates = gs.getVertexCoordinates();

        for (Vertex vertex : lastNVertices) {
            Coordinate oldCoordinate = coordinates.get(oldVertex);
            Coordinate newCoordinate = coordinates.get(vertex);
            double distance = Math.sqrt(Math.pow(oldCoordinate.getX() - newCoordinate.getX(), 2)
                    + Math.pow(oldCoordinate.getY() - newCoordinate.getY(), 2));
            if (distance > (height > width ? height / 2
                    : width / 2)) {
                mirroredMoves++;
            }
        }
        return (mirroredMoves > lastNVertices.size() / 2);
    }

    public ClosestBoardEdge findClosestBoardEdge(int x, int y) {
        if (x < y) {
            if (height - y < x)
                return ClosestBoardEdge.Bottom;
            return ClosestBoardEdge.Left;
        } else {
            if (y < width - x)
                return ClosestBoardEdge.Top;
            return ClosestBoardEdge.Right;
        }
    }

    public GameMove getMinimizingMove(GameMove lastMove) {
        // If the enemy tries to counter our method by always placing our neighbours we
        // return to random playing

        // TODO: Add check for enemyMirroredOnce if neccessary
        if (lastNVertices.size() >= 1 && enemyStealsNeighbor()) {
            if (enemyMirrors()) {
                openTreeEndpoints = new ArrayList<>();
                // enemyMirroredOnce = true;
                return treeMinimizer(lastMove, new Coordinate(width / 2, height / 2), width / 15, height / 15);
            }
            return getBruteForceMove(false, lastMove);
        }

        return treeMinimizer(lastMove, new Coordinate(width / 2, height / 2), width, height);

    }

    public GameMove getBruteForceMove(boolean maximize, GameMove lastMove) {
        // get the first vertex that is not yet placed
        Vertex v = null;
        ArrayList<Vertex> vertices = new ArrayList<>();
        if (lastMove != null)
            vertices = getUnplacedNeighbors(lastMove.getVertex());
        if (vertices.size() == 0) {

            for (Vertex v_ : g.getVertices()) {
                if (!gs.getPlacedVertices().contains(v_)) {
                    v = v_;
                    break;
                }
            }
        } else {
            v = vertices.get(0);
        }

        // Find optimal heatmap square
        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        int minRow = -1, minCol = -1, maxRow = -1, maxCol = -1;

        for (int i = 0; i < heatMap.size(); i++) {
            for (int j = 0; j < heatMap.get(i).size(); j++) {
                int val = heatMap.get(i).get(j);
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

        ArrayList<Integer> xPositions = new ArrayList<Integer>();
        ArrayList<Integer> yPositions = new ArrayList<Integer>();

        // Create sample set based on the lowest/highest vertex density area
        // Using abort in case of areas being completly filled, chose a reasonable(?)
        // value
        for (int sample = 0, abort = 0; sample < sampleSize / 2 && abort < 200; sample++, abort++) {
            int x = r.nextInt(width / heatMapSize) + bestSquareX * (width / heatMapSize);
            int y = r.nextInt(height / heatMapSize) + bestSquareY * (height / heatMapSize);
            if (gs.getUsedCoordinates()[x][y] != 0) {
                sample--;
            } else {
                xPositions.add(x);
                yPositions.add(y);
            }
        }

        // Create random sample set of possible placing positions of the current vertex
        // v
        for (int sample = 0; sample < Math.ceil(sampleSize / 2.0); sample++) {
            int x = r.nextInt(width);
            int y = r.nextInt(height);
            if (gs.getUsedCoordinates()[x][y] != 0) { // The random coordinate is already taken
                sample--;
            } else {
                xPositions.add(x);
                yPositions.add(y);
            }
        }

        // Number of crossings before we place the vertex
        int bestTotalCrossingsByVertex = maximize ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int bestSample = 0;
        // Find best position (maximizing crossings) we can place vertex v at
        for (int sample = 0; sample < sampleSize; sample++) {
            if (gs.getUsedCoordinates()[xPositions.get(sample)][yPositions.get(sample)] != 0)
                continue;
            Coordinate coordinateToAdd = new Coordinate(xPositions.get(sample), yPositions.get(sample));
            int crossingsAddedByVertex = betterEdgeCrossingRTree.testCoordinate(v, coordinateToAdd,
                    gs.getVertexCoordinates());
            if (maximize ? crossingsAddedByVertex > bestTotalCrossingsByVertex
                    : crossingsAddedByVertex < bestTotalCrossingsByVertex) {
                bestSample = sample;
                bestTotalCrossingsByVertex = crossingsAddedByVertex;
            }
        }

        Coordinate coordinateToAdd = new Coordinate(xPositions.get(bestSample), yPositions.get(bestSample));
        HashMap<Vertex, Coordinate> mapVertexToCoordinate = gs.getVertexCoordinates();
        mapVertexToCoordinate.put(v, coordinateToAdd);
        betterEdgeCrossingRTree.insertAllCoodinates(gs.getPlacedVertices());

        return new GameMove(v, coordinateToAdd);
    }

    public GameMove getMirroringMove(GameMove lastMove) {
        // place it mirrored around center point on the circle/ellipsis
        // whose size is given by this.relativeCircleSize

        // if it's the first move, take the first vertex and place it on the circle
        // right side, it's as good as any
        // TODO: is it if the field is not square??
        if (lastMove == null) {
            double x = width * (0.5 + relativeCircleSize);
            double y = height * 0.5;
            Coordinate c = getCoordinateClampedToPlayingField(x, y);
            return new GameMove(g.getVertices().iterator().next(), c);
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
        if (neighbors.size() == 0) {
            return getRandomMove();
        }
        // get the one with the most placed neighbors
        // it will draw more fixed edges and we should optimize this
        Vertex v = null;
        int maxNeighborsPlaced = -1;
        for (Vertex v_ : neighbors) {
            int n_ = (int) StreamSupport.stream(g.getIncidentEdges(v_).spliterator(), false).count()
                    - getUnplacedNeighbors(v_).size();
            if (n_ > maxNeighborsPlaced) {
                v = v_;
                maxNeighborsPlaced = n_;
            }
        }

        // mirror around the center onto the circle of highest probabilty
        // get the unit vector from the lastMove vertex towards the center
        int lastX = gs.getVertexCoordinates().get(lastMove.getVertex()).getX();
        int lastY = gs.getVertexCoordinates().get(lastMove.getVertex()).getY();
        UnitVector uvec = new UnitVector(width / 2.0 - lastX, height / 2.0 - lastY);
        // get the position on the highest probability circle
        double x = width * (0.5 + uvec.getX() * relativeCircleSize);
        double y = height * (0.5 + uvec.getY() * relativeCircleSize);
        return new GameMove(v, getNearestFreeCoordinate(getCoordinateClampedToPlayingField(x, y)));
    }

    public Coordinate getNearestFreeCoordinate(Coordinate c) {
        int x = c.getX();
        int y = c.getY();
        // if position is not taken yet, return it
        if (gs.getUsedCoordinates()[x][y] == 0) {
            return new Coordinate(x, y);
        }
        // else get a neighboring position
        int i = 1;
        while (true) {
            for (int j = -i; j <= i; j++) {
                if ((x + j) >= 0 && (x + j) < width) {
                    for (int k = -i; k <= i; k++) {
                        if (Math.abs(j) == i || Math.abs(k) == i) {
                            if ((y + k) >= 0 && (y + k) < height) {
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
     * Computes a random valid move.
     * 
     * @return A random valid move.
     */
    private GameMove getRandomMove() {
        int stillToBePlaced = g.getN() - gs.getPlacedVertices().size();
        int next = r.nextInt(stillToBePlaced);
        int skipped = 0;
        Vertex v = null;
        for (Vertex u : g.getVertices()) {
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
            c = new Coordinate(r.nextInt(width), r.nextInt(height));
        } while (gs.getUsedCoordinates()[c.getX()][c.getY()] != 0);
        return new GameMove(v, c);
    }

    public ArrayList<Vertex> getUnplacedNeighbors(Vertex v) {
        ArrayList<Vertex> neighbors = new ArrayList<>();
        if (g.getIncidentEdges(v) == null) {
            return neighbors;
        }
        for (Edge edge : g.getIncidentEdges(v)) {
            Vertex vertexToAdd = v.equals(edge.getS()) ? edge.getT() : edge.getS();
            if (!gs.getPlacedVertices().contains(vertexToAdd)) {
                neighbors.add(vertexToAdd);
            }
        }
        return neighbors;
    }

    public int getLargestGapMidpointID() {
        if (gs.getPlacedVertices().isEmpty()) {
            return g.getN() / 2;
        }

        // Convert the IDs to a list of integers and sort it
        List<Integer> ids = gs.getPlacedVertices().stream().map(vertex -> Integer.parseInt(vertex.getId())).sorted()
                .collect(Collectors.toList());

        // Initialize variables to track the largest gap and its midpoint
        int largestGap = 0;
        int largestGapMidpoint = 0;

        // Check the gap between each pair of consecutive IDs
        for (int i = 0; i < ids.size() - 1; i++) {
            int gap = ids.get(i + 1) - ids.get(i);
            if (gap > largestGap) {
                // System.out.println("gap " + gap + " with " + (i + 1) + " and id " + ids.get(i
                // + 1) + " and " + i
                // + " and id " + ids.get(i));
                largestGap = gap;
                largestGapMidpoint = ids.get(i) + gap / 2;
            }
        }

        // Check the gap between the last and first ID
        int finalGap = g.getN() + 1 - ids.get(ids.size() - 1);
        if (finalGap > largestGap) {
            // System.out.println("second");
            // System.out.println("gap " + finalGap + " with id 1 and " + (ids.size() - 1)
            // + " and id " + ids.get(ids.size() - 1));
            // System.out.println(ids);
            // System.out.println(ids.size());
            largestGapMidpoint = (ids.get(ids.size() - 1) + finalGap / 2) % g.getN();
        }
        // System.out.println(largestGapMidpoint);

        return largestGapMidpoint;
    }

    public Coordinate getCoordinateClampedToPlayingField(double x, double y) {
        // simple: make sure its in bounds of the playing field
        // int a = Math.max(Math.min(roundToClosestInteger(x), width-1), 0);
        // int b = Math.max(Math.min(roundToClosestInteger(y), height-1), 0);
        // advanced: do it in the direction of the center of the field
        if (!(x >= 0 && x < width && y >= 0 && y < height)) {
            UnitVector uvec = new UnitVector(width / 2.0 - x, height / 2.0 - y);
            double alpha = 0.0;
            if (x < 0) {
                alpha = -x / uvec.getX();
            } else if (x > width - 1) {
                // width-1 = x + c * uvec.getX()
                // c = (width-1 - x) / uvec.getX()
                alpha = (width - 1 - x) / uvec.getX();
            }
            x += alpha * uvec.getX();
            y += alpha * uvec.getY();

            if (y < 0) {
                alpha = -y / uvec.getY();
            } else if (y > height - 1) {
                alpha = (height - 1 - y) / uvec.getY();
            }
            x += alpha * uvec.getX();
            y += alpha * uvec.getY();
        }

        int a = roundToClosestInteger(x);
        int b = roundToClosestInteger(y);
        assert a >= 0 : "1";
        assert a < width : "2";
        assert b >= 0 : "3";
        assert b < height : "4";
        return new Coordinate(a, b);
    }

    public int roundToClosestInteger(double val) {
        if (val < 0) {
            return (int) Math.ceil(val);
        }
        return (int) Math.floor(val);
    }

    @Override
    public void initializeNextRound(Graph g, int width, int height, Role role) {
        this.g = g;
        this.width = width;
        this.height = height;
        if (height < this.heatMapSize || width < this.heatMapSize) {
            this.heatMapSize = 1;
        }
        this.gs = new GameState(g, width, height);
        this.betterEdgeCrossingRTree = new BetterEdgeCrossingRTree(g);
        heatMap = new ArrayList<ArrayList<Integer>>();
        lastNVertices = new ArrayList<>();
        for (int i = 0; i < heatMapSize; i++) {
            heatMap.add(new ArrayList<Integer>(Collections.nCopies(heatMapSize, 0)));
        }

        for (Vertex vertex : g.getVertices()) {
            this.mapIdToVertex.put(vertex.getId(), vertex);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    private void writeCircleSizeToFile() {
        // where and whom to store
        String path = "circleOptimization.txt";
        // store the crossings number of our player there
        try {
            FileWriter myWriter = new FileWriter(path, true);
            myWriter.write("\n\ncircleSize: " + this.relativeCircleSize);
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
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
     * An enum describing the closest edge to a vertex
     */
    public enum ClosestBoardEdge {
        Top, Right, Bottom, Left
    }
}
