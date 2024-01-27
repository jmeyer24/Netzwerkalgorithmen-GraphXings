package GraphXings.Gruppe4.Common;

import GraphXings.Data.Coordinate;
import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameState;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Optional;

public class EdgeHelper {

    /**
     * Choose an edge and a vertex where the edge has one placed vertex
     * and one unplaced vertex.
     * This can be used to get a unplaced vertex where we can draw a line to another coordinate.
     * @param edges All edges of the graph
     * @param vertexCoordinates Already used coordinates
     * @return A tuple of edge and the unplaced vertex. If all vertices are placed return empty
     */
    public static Optional<SimpleEntry<Edge, Vertex>> getUnusedVertex(Iterable<Edge> edges, HashMap<Vertex, Coordinate> vertexCoordinates) {
        for (var e : edges) {
            var source = vertexCoordinates.get(e.getS());
            var target = vertexCoordinates.get(e.getT());
            // Check if only one vertex is placed
            if (source == null ^ target == null) {
                // Return the unplaced vertex
                return Optional.of(new SimpleEntry<>(e, (source == null) ? e.getS() : e.getT()));
            }
        }
        return Optional.empty();
    }

    /**
     * Choose an edge and a vertex where the edge has one placed vertex
     * and one unplaced vertex.
     * This can be used to get a unplaced vertex where we can draw a line to another coordinate.
     * @param edges All edges of the graph
     * @param vertexCoordinates Already used coordinates
     * @return A tuple of edge and the placed vertex. If all vertices are placed return empty
     */
    public static Optional<SimpleEntry<Edge, Vertex>> getUsedVertex(Iterable<Edge> edges, HashMap<Vertex, Coordinate> vertexCoordinates) {
        for (var e : edges) {
            var source = vertexCoordinates.get(e.getS());
            var target = vertexCoordinates.get(e.getT());
            // Check if only one vertex is placed
            if (source == null ^ target == null) {
                // Return the unplaced vertex
                return Optional.of(new SimpleEntry<>(e, (source != null) ? e.getS() : e.getT()));
            }
        }
        return Optional.empty();
    }

    public static double getSumEdgeLenths(Graph g, GameState gs, Vertex vertex, Coordinate coordinate){
        var incidentEdges = g.getIncidentEdges(vertex);
        var vertexCoordinates = gs.getVertexCoordinates();

        double sum_lenghts = 0;
        for (Edge edge : incidentEdges){
            double x = 0;
            double y = 0;
            if (edge.getS().equals(vertex) && vertexCoordinates.containsKey(edge.getT())){
                Coordinate target = vertexCoordinates.get(edge.getT());
                x = Math.abs(target.getX() - coordinate.getX());
                y = Math.abs(target.getY() - coordinate.getY());
            } else if (vertexCoordinates.containsKey(edge.getS())){
                Coordinate source = vertexCoordinates.get(edge.getS());
                x = Math.abs(source.getX() - coordinate.getX());
                y = Math.abs(source.getY() - coordinate.getY());
            }
            sum_lenghts += Math.sqrt(x*x + y*y);
        }
        return sum_lenghts;

    }
}
