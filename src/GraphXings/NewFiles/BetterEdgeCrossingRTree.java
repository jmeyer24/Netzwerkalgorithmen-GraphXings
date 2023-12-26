package GraphXings.NewFiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Rectangle;

import GraphXings.Data.Coordinate;
import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Data.Segment;
import GraphXings.Data.Vertex;

public class BetterEdgeCrossingRTree {
    /* ---------------- attributes -------------- */
    /**
     * The graph the {@code BetterEdgeCrossingRTree} works on.
     */
    private Graph g;
    /**
     * Keeps a list of vertices that are inserted in the RTree so far.
     */
    private List<Vertex> vertices;
    /**
     * The underlying RTree structure by David Moten
     * (com.github.davidmoten.rtree2.RTree)
     */
    private RTree<Edge, Geometry> rTree;

    /* ---------------- constructor -------------- */
    /**
     * Constructs an empty {@code BetterEdgeCrossingRTree} with the specified
     * initial graph.
     *
     * @param g the initial graph the RTree works on.
     */
    public BetterEdgeCrossingRTree(Graph g) {
        this.g = g;
        this.vertices = new ArrayList<Vertex>();
        this.rTree = RTree.create();
    }

    /* ---------------- public methods -------------- */
    /**
     * Add vertices from a hashset to this.vertices
     * 
     * @param vertices the vertices to add
     */
    public void insertAllCoordinates(HashSet<Vertex> vertices) {
        vertices.forEach(vertex -> {
            insertVertex(vertex);
        });
    }

    /**
     * Insert the given vertex in the RTree.
     * 
     * @param vertex the vertex we want to insert
     */
    public void insertVertex(Vertex vertex) {
        this.vertices.add(vertex);
    }

    /**
     * Remove the given vertex from this.vertices
     * 
     * @param vertex the vertex we remove
     */
    public void removeVertex(Vertex vertex) {
        this.vertices.remove(vertex);
    }

    /**
     * Tests how many crossings the vertex at the coordinate would generate.
     * 
     * @param vertex                the vertex we place and test for
     * @param coordinate            the coordinate the vertex is placed at
     * @param mapVertexToCoordinate the mapping from vertex to coordinate
     * @return number of crossings when adding the vertex at the coordinate
     */
    public int testCoordinate(Vertex vertex, Coordinate coordinate, HashMap<Vertex, Coordinate> mapVertexToCoordinate) {
        mapVertexToCoordinate.put(vertex, coordinate);
        insertVertex(vertex);
        int crossings = calculateCrossings(vertex, mapVertexToCoordinate);

        mapVertexToCoordinate.remove(vertex);
        removeVertex(vertex);
        return crossings;
    }

    /**
     * Use the RTree visualization on the this.rTree object.
     * Opens a new window.
     */
    public void createImg() {
        this.rTree.visualize(1000, 1000).save("./images/Graph_" + ".png");
    }

    /**
     * Computes the number of crossings generated when inserting a vertex to the
     * current game state
     * 
     * @param vertex                calculate the number of crossings for this
     *                              vertex
     * @param mapVertexToCoordinate the mapping from vertex to coordinate, this
     *                              represents the current game state
     * @return number of crossings when adding the vertex at the coordinate
     * @apiNote The GameState, represented by mapVertexToCoordinate, and
     *          this.vertices do need to
     *          have inserted the vertex already.
     */
    public int calculateCrossings(Vertex vertex, HashMap<Vertex, Coordinate> mapVertexToCoordinate) {
        int crossings = 0;

        int x1 = 0;
        int y1 = 0;
        int x2 = 0;
        int y2 = 0;

        // iterate over the edges adjacent to this vertex
        Iterable<Edge> incidentEdges = this.g.getIncidentEdges(vertex);
        for (Edge edge : incidentEdges) {
            Coordinate s = mapVertexToCoordinate.get(edge.getS());
            Coordinate t = mapVertexToCoordinate.get(edge.getT());
            // continue, if the edge does not exist yet, e.g. one of the vertices is not
            // placed yet
            if (s == null || t == null) {
                continue;
            }

            // update the above-scope variables to the new mins/maxs
            x1 = Integer.min(x1, Integer.min(s.getX(), t.getX()));
            y1 = Integer.min(y1, Integer.min(s.getY(), t.getY()));
            x2 = Integer.max(x2, Integer.max(s.getX(), t.getX()));
            y2 = Integer.max(y2, Integer.max(s.getY(), t.getY()));

            this.rTree = this.rTree.add(edge, Geometries.line(s.getX(), s.getY(), t.getX(), t.getY()));
        }

        HashSet<Vertex> searchedVertices = new HashSet<>();

        Rectangle rectangle = Geometries.rectangle(x1, y1, x2, y2);
        Iterable<Entry<Edge, Geometry>> search = this.rTree.search(rectangle);
        for (Entry<Edge, Geometry> entry : search) {
            if (mapVertexToCoordinate.get(entry.value().getS()) == null
                    || mapVertexToCoordinate.get(entry.value().getT()) == null)
                continue;
            searchedVertices.add(entry.value().getS());
            searchedVertices.add(entry.value().getT());
        }

        // comparator: compares edges for the higher y-value in any of their two nodes
        // true if e1 has higher y-value, else false
        Comparator<Edge> comparator = (Edge e1, Edge e2) -> {
            return Integer.compare(
                    Math.max(mapVertexToCoordinate.get(e1.getS()).getY(), mapVertexToCoordinate.get(e1.getT()).getY()),
                    Math.max(mapVertexToCoordinate.get(e2.getS()).getY(), mapVertexToCoordinate.get(e2.getT()).getY()));
        };
        // create a TreeSet where you automatically sort the added edges by the y-value
        TreeSet<Edge> binarySearchTree = new TreeSet<Edge>(comparator);

        for (Vertex v : searchedVertices) {
            Iterable<Edge> adjacentEdges = this.g.getIncidentEdges(v);
            for (Edge edge : adjacentEdges) {
                // for the current edge get the neighbor
                Vertex neighbor = v == edge.getS() ? edge.getT() : edge.getS();
                // TODO only the placed vertex from which we get the searchedVertices is valid
                // skip this neighbor if it wasn't placed yet
                if (mapVertexToCoordinate.get(neighbor) == null) {
                    continue;
                }

                // create a segment from the current edge for the intersection check
                Segment thisSegment = new Segment(mapVertexToCoordinate.get(edge.getS()),
                        mapVertexToCoordinate.get(edge.getT()));

                // if the vertex is positioned left (x-axis) of its neighbor
                // compute whether there is a crossing
                if (mapVertexToCoordinate.get(v).getX() < mapVertexToCoordinate.get(neighbor).getX()) {
                    // (!) add the edge to the search tree if we sweep over the line from now on
                    binarySearchTree.add(edge);

                    // get the next higher edge (y-axis) after the current edge
                    Edge higherEdge = binarySearchTree.higher(edge);
                    // if that edge is valid (there is a higher edge one than the current edge)
                    if (higherEdge != null) {
                        // create another segment from this predicted edge
                        Segment higherSegment = new Segment(mapVertexToCoordinate.get(higherEdge.getS()),
                                mapVertexToCoordinate.get(higherEdge.getT()));
                        // if the segments intersect, we increment the crossing number and check for the
                        // next neighbor
                        if (Segment.intersect(thisSegment, higherSegment)) {
                            crossings++;
                            continue;
                        }
                    }
                    // analogous as for .higher() above, get the next lower edge (y-axis) before the
                    // current edge and check if there is a crossing
                    Edge lowerEdge = binarySearchTree.higher(edge);
                    if (lowerEdge != null) {
                        Segment lowerSegment = new Segment(mapVertexToCoordinate.get(lowerEdge.getS()),
                                mapVertexToCoordinate.get(lowerEdge.getT()));
                        if (Segment.intersect(thisSegment, lowerSegment)) {
                            crossings++;
                            continue;
                        }
                    }
                }
                // analogous to the other case, if the vertex is positioned right (x-axis) of
                // its neighbor
                else {
                    Edge higherEdge = binarySearchTree.higher(edge);
                    if (higherEdge != null) {
                        Segment higherSegment = new Segment(mapVertexToCoordinate.get(higherEdge.getS()),
                                mapVertexToCoordinate.get(higherEdge.getT()));
                        if (Segment.intersect(thisSegment, higherSegment)) {
                            crossings++;
                            continue;
                        }
                    }
                    Edge lowerEdge = binarySearchTree.lower(edge);
                    if (lowerEdge != null) {
                        Segment lowerSegment = new Segment(mapVertexToCoordinate.get(lowerEdge.getS()),
                                mapVertexToCoordinate.get(lowerEdge.getT()));
                        if (Segment.intersect(thisSegment, lowerSegment)) {
                            crossings++;
                            continue;
                        }
                    }

                    // (!) delete the neighbor from the search tree if we are finished sweeping over
                    // the edge
                    binarySearchTree.remove(edge);
                }
            }
        }
        // return the number of crossings with the given vertex in the current gamestate
        return crossings;
    }

}
