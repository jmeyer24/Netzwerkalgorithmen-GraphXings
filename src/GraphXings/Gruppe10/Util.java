package GraphXings.Gruppe10;

import GraphXings.Data.*;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;

import java.util.*;

public class Util {
    /**
     * Computes a random valid move.
     *
     * @return A random valid move.
     */
    public static GameMove randomMove(Graph g, GameState gs, Random r, int width, int height) {
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
        }
        while (gs.getUsedCoordinates()[c.getX()][c.getY()] != 0);
        return new GameMove(v, c);
    }

    /**
     * Apply the last move by the opponent if there is one.
     *
     * @param lastMove the last move the opponent made
     */
    public static void applyLastMove(GameMove lastMove, GameState gs) {
        // First: Apply the last move by the opponent if there is one.
        if (lastMove != null) {
            gs.applyMove(lastMove);
        }
    }

    /**
     * Calculate the distance between two coordinates
     *
     * @param a coordinate a
     * @param b coordinate b
     * @return distance between a and b
     */
    public static double distance(Coordinate a, Coordinate b) {
        double ac = Math.abs(b.getY() - a.getY());
        double cb = Math.abs(b.getX() - a.getX());
        return Math.hypot(ac, cb);
    }

    /**
     * Gets any vertex that is unplaced and a neighbor of a placed neighbor
     *
     * @return a singleton map mapping any placed vertex to any of its free neighbors
     */
    public static Map<Vertex, Vertex> getAnyFreeNeighbor(Graph g, GameState gs) {
        try {
            Map.Entry<Vertex, HashSet<Vertex>> anyEntry = getFreeNeighborsOfPlacedVertices(g, gs).entrySet().iterator().next();
            Vertex placedVertex = anyEntry.getKey();
            Vertex freeNeighbor = anyEntry.getValue().iterator().next();
            return Collections.singletonMap(placedVertex, freeNeighbor);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Gets any free (unplaced) vertex.
     * @return any free vertex or null if there are no free vertices.
     */
    public static Vertex getAnyFreeVertex(Graph g, GameState gs) {
        for (Vertex v : g.getVertices()) {
            if (!gs.getPlacedVertices().contains(v)) {
                return v;
            }
        }
        return null;
    }

    /**
     * For all placed Vertices P get the Neighbourhood N(P) where n in N(P), but not in P
     *
     * @return Map every vertex p in P to its Neighbourhood N(p) if N(p) is not empty (List of Vertex)
     */
    public static HashMap<Vertex, HashSet<Vertex>> getFreeNeighborsOfPlacedVertices(Graph g, GameState gs) {
        HashMap<Vertex, HashSet<Vertex>> neighborsOfPlacedVertices = new HashMap<>();
        for (Vertex vertex : gs.getPlacedVertices()) {
            HashSet<Vertex> neighbors = getFreeNeighbors(vertex, g, gs);
            if (neighbors.isEmpty()) continue;
            neighborsOfPlacedVertices.put(vertex, neighbors);
        }
        return neighborsOfPlacedVertices;
    }

    /**
     * Gets all free neighbors of a vertex v
     */
    public static HashSet<Vertex> getFreeNeighbors(Vertex v, Graph g, GameState gs) {
        HashSet<Vertex> neighbors = new HashSet<>();
        for (Edge e : g.getIncidentEdges(v)) {
            Vertex n = (e.getS().getId().equals(v.getId())) ? e.getT() : e.getS();
            if (!gs.getPlacedVertices().contains(n)) neighbors.add(n);
        }
        return neighbors;
    }

    public static Vertex getAnyFreeNeighborOfVertexSet(Set<Vertex> vertices, Graph g, GameState gs) {
        try {
            for (Vertex v : vertices) {
                Vertex freeNeighbor = getFreeNeighbors(v, g, gs).iterator().next();
                if (freeNeighbor != null) {
                    return freeNeighbor;
                }
            }
        } catch (NoSuchElementException ignored) {
        }
        return null;
    }

    public static Coordinate findClosestUnusedCoordinateMiddle(GameState gs, Coordinate coordinate, int width, int height) {
        Coordinate middle = new Coordinate(width / 2, height / 2);
        Coordinate result = findClosestUnusedCoordinate(gs, coordinate, width, height);
        int[][] usedCoordinates = gs.getUsedCoordinates();

        Coordinate vectorToMiddle = new Coordinate(coordinate.getX() - middle.getX(), coordinate.getY() - middle.getY());
        double distanceToMiddle = distance(middle, coordinate);
        Coordinate normVectorToMiddle = new Coordinate((int) (vectorToMiddle.getX() / distanceToMiddle), (int) (vectorToMiddle.getY() / distanceToMiddle));
        int maxRange = Math.min(width, height) / 2;
        Coordinate currentCoordinate = coordinate;
        int newX = currentCoordinate.getX();
        int newY = currentCoordinate.getY();

        for (int i = 0; i <= maxRange; i++) {
            if (isValidCoordinate(currentCoordinate, width, height) && (usedCoordinates[newX][newY] == 0)) {
                return currentCoordinate;
            }
            newY = normVectorToMiddle.getY() + currentCoordinate.getY();
            newX = normVectorToMiddle.getX() + currentCoordinate.getX();
            currentCoordinate = new Coordinate(newX, newY);
        }
        // Fall-back: no unused coordinate found
        return result;
    }

    public static Coordinate findClosestUnusedCoordinate(GameState gs, Coordinate coordinate, int width, int height) {
        Coordinate result = new Coordinate(-1, -1);
        int[][] usedCoordinates = gs.getUsedCoordinates();
        int maxRange = Math.max(width, height);

        for (int i = 0; i <= maxRange; i++) {
            for (int j = -i; j <= i && j < width && j > -width; j++) {
                for (int k = -i; k <= i && k < height && k > -height; k++) {
                    int newX = coordinate.getX() + j;
                    int newY = coordinate.getY() + k;

                    Coordinate currentCoordinate = new Coordinate(newX, newY);

                    if (isValidCoordinate(currentCoordinate, width, height) && (usedCoordinates[newX][newY] == 0)) {
                        return currentCoordinate;
                    }
                }
            }
        }
        // Fall-back: no unused coordinate found
        return result;
    }

    /**
     * @param gs           game state
     * @param placedVertex vertex in whose close vicinity a free coordinate id to be found
     * @return coordinate that is one of the closest free coordinates to the given placed vertex
     */
    public static Coordinate findClosestUnusedCoordinate(GameState gs, Vertex placedVertex, int width, int height) {
        Coordinate placedVertexCoordinate = gs.getVertexCoordinates().get(placedVertex);
        return findClosestUnusedCoordinate(gs, placedVertexCoordinate, width, height);
    }

    public static boolean isValidCoordinate(Coordinate coordinate, int width, int height) {
        int x = coordinate.getX();
        int y = coordinate.getY();
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public static double rationalToDouble(Rational r) {
        return (double) r.getP() / r.getQ();
    }

    public static Coordinate nearestValidCoordinate(Rational x, Rational y, int width, int height) {
        int xRounded = (int) Math.round(rationalToDouble(x));
        if (xRounded < 0) xRounded = 0;
        else if (xRounded >= width) xRounded = width - 1;
        int xCoordinate = xRounded;

        int yRounded = (int) Math.round(rationalToDouble(y));
        if (yRounded < 0) yRounded = 0;
        else if (yRounded >= height) yRounded = height - 1;
        int yCoordinate = yRounded;

        return new Coordinate(xCoordinate, yCoordinate);
    }
}

