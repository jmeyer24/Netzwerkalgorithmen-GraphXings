package GraphXings.Gruppe4.Strategies;

import GraphXings.Data.Coordinate;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaximizePointReflection extends StrategyClass{

    public MaximizePointReflection(Graph g, GameState gs, MutableRTree<Edge, LineFloat> tree, int width, int height, SampleParameters sampleParameters, StrategiesStopWatch strategiesStopWatch) {
        super(g, gs, tree, width, height, sampleParameters, strategiesStopWatch.getWatch(StrategyName.MaximizePointReflection));
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
     * Strategy: point reflection of the last move vertex from the opponent
     *
     * @param lastMove The last opponent move.
     * @return True on success, false otherwise
     */
    @Override
    public boolean executeStrategy(GameMove lastMove) {
        super.startExecuteStrategy();

        var usedCoordinates = gs.getUsedCoordinates();
        var vertexCoordinates = gs.getVertexCoordinates();
        var placedVertices = gs.getPlacedVertices();

        //get vertex and coordinate of the last move
        Vertex lastVertex = lastMove.getVertex();
        Coordinate lastCoordinate = lastMove.getCoordinate();

        //get all neighbour vertices of the last vertex
        var neighboursOptional = Helper.getAllNeighbourVertices(g, gs, lastVertex);
        if (neighboursOptional.isEmpty()){
            return gameMove.isPresent();
        }

        var neighbours = neighboursOptional.get();
        //remove all neighbours which are already placed
        neighbours.removeIf(placedVertices::contains);

        //if vertex has no free neighbours, choose a random placed vertex with free neighbours
        if (neighbours.isEmpty()){
            for (Vertex v : placedVertices){
                if (Helper.numIncidentVertices(g, gs, v, true) > 0){
                   lastCoordinate = vertexCoordinates.get(v);
                   //should be possible since the vertex v has at least one free neighbour
                   neighbours = Helper.getAllNeighbourVertices(g, gs, v).get();
                   neighbours.removeIf(placedVertices::contains);

                    break;
                }
            }
        }

        //point reflection of the last coordinate
        Coordinate pointReflection_coordinate = new Coordinate(width - lastCoordinate.getX()-1, height - lastCoordinate.getY()-1);


        List<Coordinate> sampleList = new ArrayList<>();

        //if point reflection coordinate is not free: sample around it
        if (!Helper.isCoordinateFree(usedCoordinates, pointReflection_coordinate)){
            var samples = Helper.randPickFreeCoordinatesPerimeter(usedCoordinates, pointReflection_coordinate, width / 3, height / 3, sampleParameters.samples());
            if (samples.isPresent()){
                sampleList = samples.get();
            }
        } else {
            sampleList = List.of(pointReflection_coordinate);
        }

        gameMove = chooseHighestIntersection(neighbours, sampleList);

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
        return StrategyName.MaximizePointReflection;
    }
}
