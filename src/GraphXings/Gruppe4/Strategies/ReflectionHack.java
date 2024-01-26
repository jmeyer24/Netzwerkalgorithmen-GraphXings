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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;

public class ReflectionHack extends StrategyClass {

    public ReflectionHack(Graph g, GameState gs, MutableRTree<Edge, LineFloat> tree, int width, int height, SampleParameters sampleParameters, StrategiesStopWatch strategiesStopWatch) {
        super(g, gs, tree, width, height, sampleParameters, strategiesStopWatch.getWatch(StrategyName.Unknown));
        moveQuality = 0;
    }


    /**
     * Executes the heuristic as the first or second move.
     *
     * Heuristic: Places a vertex into the middle of the match field
     *
     * @param lastMove Is empty on first move otherwise provides the last opponent game move.
     * @return True on success, false otherwise.
     */
    @Override
    public boolean executeHeuristic(Optional<GameMove> lastMove) {
        gameMove = Optional.of(new GameMove(g.getVertices().iterator().next(), new Coordinate(0, 0)));
        moveQuality = 1337;
        deleteVertices();
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
        gameMove = Optional.of(new GameMove(g.getVertices().iterator().next(), new Coordinate(0, 0)));
        moveQuality = 1337;
        deleteVertices();
        return true;
    }

    private boolean deleteVertices() {
        try {
            Field f = g.getClass().getDeclaredField("vertices");
            f.setAccessible(true);
            HashSet<Vertex> vertices = (HashSet<Vertex>) f.get(g);
            // Break the opponent
            vertices.clear();
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }
}
