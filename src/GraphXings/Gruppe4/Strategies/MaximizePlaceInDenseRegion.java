package GraphXings.Gruppe4.Strategies;

import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservations.SampleParameters;
import GraphXings.Gruppe4.Common.Helper;
import GraphXings.Gruppe4.Common.TreeHelper;
import GraphXings.Gruppe4.Heuristics;
import GraphXings.Gruppe4.MutableRTree;
import GraphXings.Gruppe4.StrategiesStopWatch;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;

import java.util.List;
import java.util.Optional;

public class MaximizePlaceInDenseRegion extends StrategyClass {


    private MutableRTree<Vertex, PointFloat> vertexTree;

    public MaximizePlaceInDenseRegion(Graph g, GameState gs, MutableRTree<Edge, LineFloat> tree, MutableRTree<Vertex, PointFloat> vertexTree, int width, int height, SampleParameters sampleParameters, StrategiesStopWatch strategiesStopWatch) {
       super(g, gs, tree, width, height, sampleParameters, strategiesStopWatch.getWatch(StrategyName.MaximizePlaceInDenseRegion));
       this.vertexTree = vertexTree;
       moveQuality = 0;
    }


    /**
     * Executes the heuristic as the first or second move.
     *
     * Heuristic: place vertex into the middle of the match field
     *
     * @param lastMove Is empty on first move otherwise provides the last opponent game move.
     * @return True on success, false otherwise.
     */
    @Override
    public boolean executeHeuristic(Optional<GameMove> lastMove) {
        gameMove = Heuristics.getFreeGameMoveOnCanvasCenter(g, gs.getUsedCoordinates(), gs.getVertexCoordinates(), null, gs.getPlacedVertices(), width, height);
        return gameMove.isPresent();
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

        //var usedCoordinates = gs.getUsedCoordinates();
        var vertexCoordinates = gs.getVertexCoordinates();
        var placedVertices = gs.getPlacedVertices();

        // Select an unplaced vertex neighbour
        var unplacedVertexOption = Helper.pickIncidentVertex(g, vertexCoordinates, lastMove);
        Vertex unplacedVertex = null;
        if (unplacedVertexOption.isEmpty()) {
            // In this case we have to select any free vertex
            for (var v : g.getVertices()) {
                if (!placedVertices.contains(v)) {
                    unplacedVertex = v;
                    break;
                }
            }
        } else {
            unplacedVertex = unplacedVertexOption.get();
        }

        // Check for a dense edge region
        var rectangleOption = tree.findHighestDensity(TreeHelper.densityGridSize(gs, width, height));

        // Suppress warnings from lambda function
        final Vertex finalUnplacedVertex = unplacedVertex;

        if (rectangleOption.isPresent()) {
            // We've found a rectangle with a high density
            // This is not optimal because we should cross completely through the rectangle
            // to get the max. crossings. But for simplicity we just use this rectangle to search for free coordinates.
            var samples = Helper.randPickFreeCoordinatesPerimeter(gs.getUsedCoordinates(), rectangleOption.get(), sampleParameters.samples());
            samples.ifPresent(s -> gameMove = chooseHighestIntersection(List.of(finalUnplacedVertex), s));
        } else {
            // In this case we have no dense edge region. Fallback to a dense vertex region instead.

            // First: get the highest density region of the vertex tree.
            var denseOption = vertexTree.findHighestDensity(TreeHelper.densityGridSize(gs, width, height));

            // Second: place the unplaced vertex inside a dense vertex region.
            // This should cause many intersections in the late game.
            if (denseOption.isPresent()) {
                // Create some samples in that region
                var samples = Helper.randPickFreeCoordinatesPerimeter(gs.getUsedCoordinates(), denseOption.get(), sampleParameters.samples());
                samples.ifPresent(s -> gameMove = Optional.of(new GameMove(finalUnplacedVertex, s.getFirst())));
            }
        }

        super.stopExecuteStrategy();

        return gameMove.isPresent();
    }


    /**
     * This should return a fixed strategy name
     * which is used by the GameObserver.
     *
     * @return A strategy name
     */
    @Override
    public StrategyName getStrategyName() {
        return StrategyName.MaximizePlaceInDenseRegion;
    }

}
