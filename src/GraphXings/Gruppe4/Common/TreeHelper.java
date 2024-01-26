package GraphXings.Gruppe4.Common;

import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import com.github.davidmoten.rtree2.internal.EntryDefault;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import GraphXings.Data.Coordinate;


public class TreeHelper {

    /**
     * Create a list of lines which can be used by the R-Tree
     * from the edges
     * @param g The graph
     * @param vertexCoordinates Mapping between vertex and coordinates
     * @param placedVertices Already placed vertices
     * @return A list of generated lines
     */
    public static List<Entry<Edge, LineFloat>> createLinesFromPlacedEdges(Graph g, HashMap<Vertex, Coordinate> vertexCoordinates, HashSet<Vertex> placedVertices) {
        return StreamSupport.stream(g.getEdges().spliterator(), false)
                .filter((e) -> placedVertices.contains(e.getS()) && placedVertices.contains(e.getT()))
                .map((e) -> {
                    // We've found a placed edge -> create a line
                    var s = vertexCoordinates.get(e.getS());
                    var t = vertexCoordinates.get(e.getT());

                    // Create a line from coordinates
                    return EntryDefault.entry(e, LineFloat.create(s.getX(), s.getY(), t.getX(), t.getY()));
                })
                .collect(Collectors.toList());
    }

    /**
     * Incrementally build up the R-Tree. This is used to add the difference between the opponent move and create
     * a list to add the missing lines to the tree.
     * @param g
     * @param vertexCoordinates
     * @param lastMove
     * @return A list of Edge to LineFloat mappings
     */
    public static Optional<List<Entry<Edge, LineFloat>>> additionalLines(Graph g, HashMap<Vertex, Coordinate> vertexCoordinates, GameMove lastMove) {
        if (lastMove == null) {
            return Optional.empty();
        }
        var edgeEntries = new ArrayList<Edge>();
        var lineEntries = new ArrayList<LineFloat>();

        // Get adjacent vertices
        var adjacent = g.getIncidentEdges(lastMove.getVertex());

        // Create lines for all placed edges
        for (var a : adjacent) {
            var sourceCoord = vertexCoordinates.get(a.getS());
            var targetCoord = vertexCoordinates.get(a.getT());
            if (sourceCoord != null && targetCoord != null) {
                if (!edgeEntries.contains(a)) {
                    // Prevent creation of duplicate lines
                    var line = LineFloat.create(sourceCoord.getX(), sourceCoord.getY(), targetCoord.getX(), targetCoord.getY());
                    lineEntries.add(line);
                    edgeEntries.add(a);
                }
            }
        }

        if (edgeEntries.isEmpty()) {
            return Optional.empty();
        }

        // Create edge/line entry list
        List<Entry<Edge, LineFloat>> list = new ArrayList<>();
        for (int i = 0; i < edgeEntries.size(); i++) {
            list.add(new EntryDefault<>(edgeEntries.get(i), lineEntries.get(i)));
        }
        return Optional.of(list);
    }

    /**
     * Incrementally update the R-Tree vertex structure.
     * @param lastMove Non-null last game move
     * @return Tuple of vertex with a associated Point object.
     */
    public static Optional<Entry<Vertex, PointFloat>> additionalPoint(GameMove lastMove) {
        if (lastMove == null) {
            return Optional.empty();
        }
        var c = lastMove.getCoordinate();
        var point = PointFloat.create(c.getX(), c.getY());
        return Optional.of(EntryDefault.entry(lastMove.getVertex(), point));
    }

    public static int densityGridSize(GameState gs, int width, int height) {
        var placedNum = gs.getPlacedVertices().size();
        var amountCoordinates = width * height;

        // Split the canvas into 2x2 grid because it is too small
        if (amountCoordinates <= 10000) {
            return 2;
        }

        // Use a simple formula placedVertices/1000 and restrict it to the range 2-10.
        var gridSize = Math.min(10, Math.max(2, placedNum / 1000));

        // Split the canvas into max. 10x10 grid based on placedNum and amountCoordinates. The value should be between 2 and 10.
        // The more vertices we have the more we can split the canvas.
        //var gridSize = Math.min(10, Math.max(2, (int) Math.ceil(Math.sqrt((double) placedNum / (double) amountCoordinates) * 10)));

        return gridSize;
    }
}
