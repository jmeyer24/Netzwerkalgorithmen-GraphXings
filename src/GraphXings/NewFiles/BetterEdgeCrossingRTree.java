package GraphXings.NewFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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

        int x1 = Integer.MAX_VALUE;
        int y1 = Integer.MAX_VALUE;
        int x2 = Integer.MIN_VALUE;
        int y2 = Integer.MIN_VALUE;

        // iterate over the edges adjacent to this vertex, e.g. the ones that get drawn
        ArrayList<Edge> createdEdges = new ArrayList<>();
        Iterable<Edge> incidentEdges = this.g.getIncidentEdges(vertex);
        for (Edge edge : incidentEdges) {
            Coordinate s = mapVertexToCoordinate.get(edge.getS());
            Coordinate t = mapVertexToCoordinate.get(edge.getT());
            // continue, if the edge does not exist yet, e.g. one of the vertices is not
            // placed yet
            if (s == null || t == null) {
                continue;
            }

            // add the edge to check possible resulting crossings
            createdEdges.add(edge);

            // update the above-scope variables to the new mins/maxs
            x1 = Integer.min(x1, Integer.min(s.getX(), t.getX()));
            y1 = Integer.min(y1, Integer.min(s.getY(), t.getY()));
            x2 = Integer.max(x2, Integer.max(s.getX(), t.getX()));
            y2 = Integer.max(y2, Integer.max(s.getY(), t.getY()));

            this.rTree = this.rTree.add(edge, Geometries.line(s.getX(), s.getY(), t.getX(), t.getY()));
        }

        // in the case that we do not have any incident edges that are created
        // (therefore the x1, ... are not changed), we return 0 crossings!
        // note: we could check for the size of incidentEdges, but as this is an
        // Iterable and we would have to work with another variable or StreamSupport,
        // this solution seems faster
        if (x1 > x2) {
            return crossings;
        }

        // search all edges in the rTree in the rectangle that encloses all the newly
        // created edges from the placed vertex
        // (!) we know the created ones don't self intersect, so we keep them separate
        Rectangle rectangle = Geometries.rectangle(x1, y1, x2, y2);
        Iterable<Entry<Edge, Geometry>> search = this.rTree.search(rectangle);
        HashSet<Edge> foundEdges = new HashSet<>();
        for (Entry<Edge, Geometry> entry : search) {
            if (!createdEdges.contains(entry.value())) {
                foundEdges.add(entry.value());
            }
        }

        // every (distinct) found edge is checked for crossings
        for (Edge foundEdge : foundEdges) {
            Coordinate s_coord = mapVertexToCoordinate.get(foundEdge.getS());
            Coordinate t_coord = mapVertexToCoordinate.get(foundEdge.getT());
            Segment foundSegment = new Segment(s_coord, t_coord);

            // with each of the newly created edges
            for (Edge createdEdge : createdEdges) {
                if (!foundEdge.equals(createdEdge)) {
                    Segment createdSegment = new Segment(mapVertexToCoordinate.get(createdEdge.getS()),
                            mapVertexToCoordinate.get(createdEdge.getT()));
                    if (Segment.intersect(foundSegment, createdSegment)) {
                        crossings++;
                    }
                }
            }
        }

        // clean the rTree from the temporarily created edges
        for (Edge createdEdge : createdEdges) {
            Coordinate s = mapVertexToCoordinate.get(createdEdge.getS());
            Coordinate t = mapVertexToCoordinate.get(createdEdge.getT());
            this.rTree = this.rTree.delete(createdEdge, Geometries.line(s.getX(), s.getY(), t.getX(), t.getY()));
        }

        // return the number of crossings with the given vertex in the current gamestate
        return crossings;
    }
}