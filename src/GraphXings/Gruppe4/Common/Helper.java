package GraphXings.Gruppe4.Common;

import GraphXings.Data.Coordinate;
import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservations.SampleParameters;
import GraphXings.Gruppe4.MutableRTree;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;

import javax.swing.plaf.metal.MetalBorders;
import java.util.*;

public class Helper {

    /**
     * Helper function to check whether a coordinate is free or in use.
     * @param usedCoordinates The usedCoordinates array.
     * @param coordinate A coordinate object to test for.
     * @return Returns true if free, false otherwise.
     */
    public static boolean isCoordinateFree(int[][] usedCoordinates, Coordinate coordinate) {
        return usedCoordinates[coordinate.getX()][coordinate.getY()] == 0;
    }

    /**
     * Helper function to check whether a coordinate is free or in use.
     * @param usedCoordinates The usedCoordinates array.
     * @return Returns true if free, false otherwise.
     */
    public static boolean isCoordinateFree(int[][] usedCoordinates, int x, int y) {
        return usedCoordinates[x][y] == 0;
    }

    /**
     * Helper fuction to check whether a coordinate is crossed by an edge
     * @param g
     * @param gs
     * @param coordinate
     * @return the Edge that crosses the coordinate if exists
     */
    public Optional<Edge> isCoordinateCrossed (Graph g, GameState gs, Coordinate coordinate){
        var vertexCoordinates = gs.getVertexCoordinates();
        var point = PointFloat.create(coordinate.getX(), coordinate.getY());

        var edges = g.getEdges();
        for (Edge e : edges){
            var line = LineFloat.create(vertexCoordinates.get(e.getS()).getX(),vertexCoordinates.get(e.getS()).getY(),vertexCoordinates.get(e.getT()).getX(),vertexCoordinates.get(e.getT()).getY());
            if (line.intersects(point)){
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }


    /**
     * Minimize crossings around a given vertex (coordX, coordY).
     * The given perimeter is the amount of the box checked around the given vertex.
     * @param usedCoordinates Array of the coordinates
     * @param tree the mutable rtree to check crossings
     * @param unplacedVertex the vertex which should be placed with this game move
     * @param coordX x coordinate of the given vertex
     * @param coordY y coordinate of the given vertex
     * @param perimeterX the x perimeter around the vertex -> 1 means one field left and right
     * @param perimeterY the y perimeter around the vertex
     * @return a game move if there's a free coordinate in the given perimeter
     */
    public static Optional<GameMove> minimizeBounds(int[][] usedCoordinates, MutableRTree<Edge, LineFloat> tree, Vertex unplacedVertex, int coordX, int coordY, int perimeterX, int perimeterY) {
        long minCrossings = Long.MAX_VALUE;
        int x = 0;
        int y = 0;
        for (int i = coordX - perimeterX; i <= coordX + perimeterX; i++) {
            for (int k = coordY - perimeterY; k <= coordY + perimeterY; k++) {
                // We're just lazy here. Don't check for bounds, just continue if we're out of bounds
                try {
                    if (isCoordinateFree(usedCoordinates, i, k)) {
                        // Found a free coordinate. Check if this minimizes crossings
                        var cross = tree.getIntersections(LineFloat.create(coordX, coordY, i, k));
                        if (cross < minCrossings) {
                            minCrossings = cross;
                            x = i;
                            y = k;
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    // TODO Switch the exception handling to array boundary checking
                    continue;
                }
            }
        }

        if (minCrossings == Long.MAX_VALUE) {
            // Edge case where no minimum was found
            return Optional.empty();
        } else {
            // Use the coordinates found
            return Optional.of(new GameMove(unplacedVertex, new Coordinate(x, y)));
        }
    }


    public static Optional<GameMove> minimizeBounds(int[][] usedCoordinates, MutableRTree<Edge, LineFloat> tree, Vertex unplacedVertex, Coordinate placedCoordinate, int perimeterX, int perimeterY) {
        return minimizeBounds(usedCoordinates, tree, unplacedVertex, placedCoordinate.getX(), placedCoordinate.getY(), perimeterX, perimeterY);
    }

    /**
     * Randomly pick some free coordinates in a given perimeter around a coordinate.
     * There's a limit how many samples should be collected. In case of a full grid/canvas,
     * there's a timeout and the list might be shorter or empty.
     * @param usedCoordinates
     * @param coordinate
     * @param perimeterX
     * @param perimeterY
     * @param amountSamples
     * @return
     */
    public static Optional<List<Coordinate>> randPickFreeCoordinatesPerimeter(int[][] usedCoordinates, Coordinate coordinate, int perimeterX, int perimeterY, int amountSamples) {
        if (perimeterX <= 0) {
            perimeterX = 1;
        }
        if (perimeterY <= 0) {
            perimeterY = 1;
        }
        var samples = new ArrayList<Coordinate>();
        Random rng = new Random();

        // If we have to test over 4 times the amount of samples then cancel the sampling.
        // This is necessary if (nearly) all coordinates in the given perimeter are used.
        // This would result in an infinite loop without a timeout.
        var timeout = amountSamples * 4;
        int i = 0;

        while (samples.size() < amountSamples && i < timeout) {
            var x = rng.nextInt(coordinate.getX() - perimeterX, coordinate.getX() + perimeterX);
            var y = rng.nextInt(coordinate.getY() - perimeterY, coordinate.getY() + perimeterY);

            // Increment the timeout
            i++;

            // We're lazy here. Just trial and error if this coordinate is within the canvas bounds
            try {
                if (isCoordinateFree(usedCoordinates, x, y)) {
                    samples.add(new Coordinate(x, y));
                }
            } catch (IndexOutOfBoundsException e) {
                // TODO Switch the exception handling to array boundary checking
                // In this case we just try again
                continue;
            }
        }

        if (samples.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(samples);
        }
    }

    /**
     * Randomly pick some free coordinates in a rectangle.
     * There's a limit how many samples should be collected. In case of a full grid/canvas,
     * there's a timeout and the list might be shorter or empty.
     * @param usedCoordinates
     * @param rect
     * @param amountSamples
     * @return
     */
    public static Optional<List<Coordinate>> randPickFreeCoordinatesPerimeter(int[][] usedCoordinates, Rectangle rect, int amountSamples) {
        // Calculate the perimeter by taking the length of x or y and divide it by 2 to get the middle of the rectangle.
        double perimeterX = (rect.x2() - rect.x1()) / 2;
        double perimeterY = (rect.y2() - rect.y1()) / 2;
        var coordinate = new Coordinate((int)(rect.x1() + perimeterX), (int)(rect.y1() + perimeterY));

        // Avoid an invalid perimeter.
        // This is caused by the integer cast later.
        if (perimeterX < 1) {
            perimeterX = 1;
        }
        if (perimeterY < 1) {
            perimeterY = 1;
        }
        return randPickFreeCoordinatesPerimeter(usedCoordinates, coordinate, (int)perimeterX, (int)perimeterY, amountSamples);
    }

    public static Optional<Vertex> pickIncidentVertex(Graph g, HashMap<Vertex, Coordinate> vertexCoordinates, GameMove lastGameMove) {
        return pickIncidentVertex(g, vertexCoordinates, lastGameMove.getVertex());

    }

    public static Optional<Vertex> pickIncidentVertex(Graph g, HashMap<Vertex, Coordinate> vertexCoordinates, Vertex vertex) {
        // Try to place the new vertex next to the last placed vertex.
        // This is only possible if one of the adjacent vertices is unplaced.
        var lastEdges = g.getIncidentEdges(vertex);
        Vertex unplacedVertex = null;
        for (var e : lastEdges) {
            var sourceCoord = vertexCoordinates.get(e.getS());
            var targetCoord = vertexCoordinates.get(e.getT());
            // Check if vertex is unplaced
            if (sourceCoord == null) {
                unplacedVertex = e.getS();
                break;
            } else if (targetCoord == null) {
                unplacedVertex = e.getT();
                break;
            }
        }

        // Return the unplaced vertex, or else an empty optional
        return Optional.ofNullable(unplacedVertex);
    }


    /**
     * Get all unplaced neighbour vertices of a given vertex which are connected by an edge.
     * @param g
     * @param gs
     * @param vertex
     * @return
     */
    public static Optional<List<Vertex>> getAllNeighbourVertices (Graph g, GameState gs, Vertex vertex){
        List<Vertex> neighbourVertices = new ArrayList<>();
        var incidentEdges = g.getIncidentEdges(vertex);
        var placedVertices = gs.getPlacedVertices();

        for (Edge e : incidentEdges){
            if (!e.getS().equals(vertex) && !placedVertices.contains(e.getS())){
                neighbourVertices.add(e.getS());
            } else if (!placedVertices.contains(e.getT())){
                neighbourVertices.add(e.getT());
            }
        }

        if (neighbourVertices.isEmpty()){
            return Optional.empty();
        } else {
            return Optional.of(neighbourVertices);
        }
    }



    /**
     * Check how many vertices are placed around a given vertex.
     * This function returns the amount of unplaced vertices which are connected via an edge to the given vertex.
     * @param g
     * @param gs
     * @param onlyFreeNeighbours should only the free neighbours be computed (true) or all (false)
     * @param vertex A given placed or unplaced vertex.
     * @return The amount of vertices.
     */
    public static int numIncidentVertices(Graph g, GameState gs, Vertex vertex, boolean onlyFreeNeighbours) {
        var incidentEdges = g.getIncidentEdges(vertex);

        // Use the underlying graph structure (HashSet) and avoid iteration over elements
        HashSet<Edge> edgeSet = new HashSet<>((HashSet<Edge>) incidentEdges);

        if (onlyFreeNeighbours){
            edgeSet.removeIf(edge -> !edge.getS().equals(vertex) && gs.getPlacedVertices().contains(edge.getS()));
            edgeSet.removeIf(edge -> !edge.getT().equals(vertex) && gs.getPlacedVertices().contains(edge.getT()));
        }

        return edgeSet.size();
    }


    /**
     * Computes a random valid move.
     * @param g The graph.
     * @param usedCoordinates The used coordinates.
     * @param placedVertices The already placed vertices.
     * @param width The width of the game board.
     * @param height The height of the game board.
     * @return A random valid move.
     */
    public static GameMove randomMove(Graph g, int[][] usedCoordinates, HashSet<Vertex> placedVertices, int width, int height)
    {
        int stillToBePlaced = g.getN()- placedVertices.size();
        Random r = new Random();
        int next = r.nextInt(stillToBePlaced);
        int skipped = 0;
        Vertex v=null;
        for (Vertex u : g.getVertices())
        {
            if (!placedVertices.contains(u))
            {
                if (skipped < next)
                {
                    skipped++;
                    continue;
                }
                v=u;
                break;
            }
        }
        Coordinate c = new Coordinate(0,0);
        do
        {
            c = new Coordinate(r.nextInt(width),r.nextInt(height));
        }
        while (usedCoordinates[c.getX()][c.getY()]!=0);
        return new GameMove(v,c);
    }

    /**
     * Checks if a given coordinate is at the border
     * @param coordinate to check
     * @return true or false
     */
    public static boolean isAtBorder(Coordinate coordinate, int rightBorder, int leftBorder, int topBorder, int bottomBorder){
        return coordinate.getX() == leftBorder || coordinate.getX() == rightBorder || coordinate.getY() == topBorder || coordinate.getY() == bottomBorder;
    }

    /**
     *Searches through the border which Coordinates are free
     *
     * @param usedCoordinates
     * @param rightBorder
     * @param leftBorder
     * @param topBorder
     * @param bottomBorder
     * @return List of free Coordinates
     */
    public static Optional<List<Coordinate>> findFreeCoordinatesAtBorder(int[][] usedCoordinates, int rightBorder, int leftBorder, int topBorder, int bottomBorder, int amountSamples) {
        List<Coordinate> freeCoordinates = new ArrayList<>();
        int counter = 0;

        // TODO: Create a timeout counter to avoid long runtimes and return empty instead
        // TODO: Reduce to one loop so we don't prefer top and bottom -> might be relevant in late game so we have more diversity
        // Another strategy would be to use something like "random binary search" (go left/right randomly) to find free coordinates faster and more diverse in region

        // search through top/bottom border
        for (int i = leftBorder; i <= rightBorder; i++){
            // top
            if (isCoordinateFree(usedCoordinates, i, topBorder)){
                counter++;
                freeCoordinates.add(new Coordinate(i, topBorder));
            }
            // bottom
            if (isCoordinateFree(usedCoordinates, i, bottomBorder)){
                counter++;
                freeCoordinates.add(new Coordinate(i, bottomBorder));
            }

            if (counter >= amountSamples) {
                // Strip to the right amount of samples
                return Optional.of(freeCoordinates.subList(0, amountSamples));
            }
        }

        //search through left/right border
        for (int i = topBorder; i <= bottomBorder; i++){
            // left
            if (isCoordinateFree(usedCoordinates, leftBorder, i)){
                counter++;
                freeCoordinates.add(new Coordinate(leftBorder, i));
            }
            // right
            if (isCoordinateFree(usedCoordinates, rightBorder, i)){
                counter++;
                freeCoordinates.add(new Coordinate(rightBorder, i));
            }

            if (counter >= amountSamples) {
                // Strip to the right amount of samples
                return Optional.of(freeCoordinates.subList(0, amountSamples));
            }
        }

        if (freeCoordinates.isEmpty()){
            return Optional.empty();
        } else {
            return Optional.of(freeCoordinates);
        }
    }

    /**
     * searches through a horizontal row of the field
     * to find its free coordinates
     * @param usedCoordinates
     * @param rightBorder
     * @param leftBorder
     * @param rowNumber
     * @return List of free Coordinates of the row
     */
    public static List<Coordinate> findFreeCoordinatesInHorizontalRow (int[][] usedCoordinates, int rightBorder, int leftBorder, int rowNumber){
        List<Coordinate> freeCoordinates = new ArrayList<>();

        for (int i = rightBorder; i <= leftBorder; i++){
            if (isCoordinateFree(usedCoordinates, i, rowNumber)){
                freeCoordinates.add(new Coordinate(i, rowNumber));
            }
        }

        return freeCoordinates;
    }


    /**
     * searches through a vertical row of the field
     * to find its free coordinates
     * @param usedCoordinates
     * @param topBorder
     * @param bottomBorder
     * @param rowNumber
     * @return List of free Coordinates of the row
     */
    public static List<Coordinate> findFreeCoordinatesInVerticalRow (int[][] usedCoordinates, int topBorder, int bottomBorder, int rowNumber){
        List<Coordinate> freeCoordinates = new ArrayList<>();

        for (int i = topBorder; i <= bottomBorder; i++){
            if (isCoordinateFree(usedCoordinates, rowNumber, i)){
                freeCoordinates.add(new Coordinate(rowNumber, i));
            }
        }

        return freeCoordinates;
    }



}
