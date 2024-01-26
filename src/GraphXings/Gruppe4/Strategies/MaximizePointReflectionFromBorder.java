package GraphXings.Gruppe4.Strategies;

import GraphXings.Data.Coordinate;
import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservations.SampleParameters;
import GraphXings.Gruppe4.Common.Helper;
import GraphXings.Gruppe4.MutableRTree;
import GraphXings.Gruppe4.StrategiesStopWatch;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaximizePointReflectionFromBorder extends StrategyClass{

    public MaximizePointReflectionFromBorder(Graph g, GameState gs, MutableRTree<Edge, LineFloat> tree, int width, int height, SampleParameters sampleParameters, StrategiesStopWatch strategiesStopWatch) {
        super(g, gs, tree, width, height, sampleParameters, strategiesStopWatch.getWatch(StrategyName.MaximizePointReflectionFromBorder));
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
     * Strategy: point reflection of one vertex at the border
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


        Vertex vertex = null;
        Coordinate vertexCoordinate = null;
        // choose a placed vertex at the border with free neighbours
        for (Vertex v : placedVertices){
             vertexCoordinate = vertexCoordinates.get(v);
            if (Helper.isAtBorder(vertexCoordinate, width-1, 0, 0, height-1) && Helper.numIncidentVertices(g, gs, v, true) > 0){
                vertex = v;
                break;
            }
        }

        //if no vertex at the border with free neighbours is found: place a free vertex at the border
        if (vertex == null) {
            //find free Coordinate at border
            int i = 0;
            Optional<List<Coordinate>> freeCoordinatesAtBorder;
            do {
                freeCoordinatesAtBorder = Helper.findFreeCoordinatesAtBorder(usedCoordinates, width-i-1, i, i, height-i-1, sampleParameters.samples());
                i++;
            } while (freeCoordinatesAtBorder.isEmpty());

            for (Vertex v : g.getVertices()){
                if (!placedVertices.contains(v)){
                    vertex = v;
                }
            }
            assert vertex != null;
            gameMove = chooseHighestIntersection(List.of(vertex), freeCoordinatesAtBorder.get());
        } else {

            //point reflection of the last coordinate
            Coordinate pointReflection_coordinate = new Coordinate(width - vertexCoordinate.getX()-1, height - vertexCoordinate.getY()-1);
            List<Coordinate> sampleList = new ArrayList<>();

            //Optional should be present since the vertex has at least one free neighbour
            var neighboursOptional = Helper.getAllNeighbourVertices(g, gs, vertex);
            
            //if point reflection coordinate is not free: sample around it
            if (!Helper.isCoordinateFree(usedCoordinates, pointReflection_coordinate)){
                var samples = Helper.randPickFreeCoordinatesPerimeter(usedCoordinates, pointReflection_coordinate, width / 3, height / 3, sampleParameters.samples());
                if (samples.isPresent()){
                    sampleList = samples.get();
                }
            } else {
                sampleList = List.of(pointReflection_coordinate);
            }

            gameMove = chooseHighestIntersection(neighboursOptional.get(), sampleList);

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
        return StrategyName.MaximizePointReflectionFromBorder;
    }
}
