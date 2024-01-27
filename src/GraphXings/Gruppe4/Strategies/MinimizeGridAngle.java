package GraphXings.Gruppe4.Strategies;

import GraphXings.Data.Coordinate;
import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservations.SampleParameters;
import GraphXings.Gruppe4.Common.EdgeHelper;
import GraphXings.Gruppe4.Common.Helper;
import GraphXings.Gruppe4.Common.TreeHelper;
import GraphXings.Gruppe4.MutableRTree;
import GraphXings.Gruppe4.StrategiesStopWatch;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;

import java.util.List;
import java.util.Optional;

/**
 * Try to build a small grid.
 * Use the vertex-rtree to find a low density region with at least one vertex.
 * Branch out from this vertex in horizontal/vertical direction to build a grid.
 * Edge should be as short as possible
 */
public class MinimizeGridAngle extends StrategyClass {

    private final MutableRTree<Vertex, PointFloat> vtree;
    public MinimizeGridAngle(Graph g, GameState gs, MutableRTree<Edge, LineFloat> tree, MutableRTree<Vertex, PointFloat> vtree, int width, int height, SampleParameters sampleParameters, StrategiesStopWatch strategiesStopWatch) {
        super(g, gs, tree, width, height, sampleParameters, strategiesStopWatch.getWatch(StrategyName.MinimizeGridAngle));
        this.vtree = vtree;
        moveQuality = 0;
    }


    /**
     * Puts a vertex at Coordinate (0,0)
     * this should be possible, because it is the first move in the game, since the maximizer begins
     *
     * @param lastMove Is empty on first move otherwise provides the last opponent game move.
     * @return True .
     */
    @Override
    public boolean executeHeuristic(Optional<GameMove> lastMove) {
        Vertex firstVertex = g.getVertices().iterator().next();
        gameMove = Optional.of(new GameMove(firstVertex, new Coordinate(0, 0)));
        return true;
    }

    /**
     * Execute the main strategy and calculate a game move.
     * The move must be stored and retrievable by getGameMove.
     *
     * @param lastMove The last opponent move.
     * @return True on success, false otherwise
     */
    @Override
    public boolean executeStrategy(GameMove lastMove) {
        super.startExecuteStrategy();

        var rectangle = vtree.findLowestDensity(TreeHelper.densityGridSize(gs, width, height));
        if (rectangle.isPresent()) {
            var entries = vtree.getElementsFromGeometry(rectangle.get());

            // Find the first entry with free edges and find a free vertical/horizontal neighbour coordinate
            for (var entry : entries) {
                var vertex = entry.value();
                var point = entry.geometry();

                var neighbours = Helper.getAllNeighbourVertices(g, gs, vertex);
                if (neighbours.isPresent()) {
                    // Vertex has free neighbour -> find free coordinate for it
                    var neighbour = neighbours.get().getFirst();
                    var gridCoordinate = getGridCoordinate((int) point.x(), (int) point.y());
                    if (gridCoordinate.isPresent()) {
                        gameMove = Optional.of(new GameMove(neighbour, gridCoordinate.get()));

                        // We're done here stop the timer
                        super.stopExecuteStrategy();
                        return true;
                    }

                }
            }
        }

        // If we don't find a rectangle, use a different strategy instead.

        super.stopExecuteStrategy();

        return gameMove.isPresent();
    }

    /**
     * Check around an origin vertex in a grid for a free coordinate.
     * This tries to get a near coordinate around the origin so the resulting edge will be as small as possible.
     * @param xOrigin x coordinate of the origin vertex.
     * @param yOrigin y coordinate of the origin vertex.
     * @return The first free coordinate
     */
    public Optional<Coordinate> getGridCoordinate(int xOrigin, int yOrigin) {
        boolean north = true;
        boolean south = true;
        boolean east = true;
        boolean west = true;

        for (int i = 1; i < width / 2; i++) {
            for (int k = 1; k < height / 2; k++) {
                try {
                    // Check north from origin
                    if (north && Helper.isCoordinateFree(gs.getUsedCoordinates(), xOrigin, yOrigin - k)) {
                        return Optional.of(new Coordinate(xOrigin, yOrigin - k));
                    }
                } catch (IndexOutOfBoundsException e) {
                    // We hit the border, just continue testing and disable this side
                    north = false;
                }

                try {
                    // Check south from origin
                    if (south && Helper.isCoordinateFree(gs.getUsedCoordinates(), xOrigin, yOrigin + k)) {
                        return Optional.of(new Coordinate(xOrigin, yOrigin + k));
                    }
                } catch (IndexOutOfBoundsException e) {
                    // We hit the border, just continue testing and disable this side
                    south = false;
                }

                try {
                    // Check east from origin
                    if (east && Helper.isCoordinateFree(gs.getUsedCoordinates(), xOrigin + i, yOrigin)) {
                        return Optional.of(new Coordinate(xOrigin + i, yOrigin));
                    }
                } catch (IndexOutOfBoundsException e) {
                    // We hit the border, just continue testing and disable this side
                    east = false;
                }

                try {
                    // Check west from origin
                    if (west && Helper.isCoordinateFree(gs.getUsedCoordinates(), xOrigin + i, yOrigin)) {
                        return Optional.of(new Coordinate(xOrigin - i, yOrigin));
                    }
                } catch (IndexOutOfBoundsException e) {
                    // We hit the border, just continue testing and disable this side
                    west = false;
                }
            }
        }
        return Optional.empty();
    }




    /**
     * This should return a fixed strategy name
     * which is used by the GameObserver.
     *
     * @return A strategy name
     */
    @Override
    public StrategyName getStrategyName() {
        return StrategyName.MinimizeGridAngle;
    }
}
