package GraphXings.Gruppe4.Strategies;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservations.SampleParameters;
import GraphXings.Gruppe4.Common.Helper;
import GraphXings.Gruppe4.MutableRTree;
import GraphXings.Gruppe4.StrategiesStopWatch;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;
import com.github.davidmoten.rtree2.geometry.internal.RectangleFloat;

import java.util.List;
import java.util.Optional;

public class RandomSampleMove extends StrategyClass {

    private NewPlayer.Role role;
    public RandomSampleMove(Graph g, GameState gs, MutableRTree<Edge, LineFloat> tree, int width, int height, NewPlayer.Role role, SampleParameters sampleParameters, StrategiesStopWatch strategiesStopWatch) {
        super(g, gs, tree, width, height, sampleParameters, strategiesStopWatch.getWatch(StrategyName.MaximizeRandomSampleMove));
        moveQuality = 0;
        this.role = role;
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
        executeStrategy(null);
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

        var samples = Helper.randPickFreeCoordinatesPerimeter(gs.getUsedCoordinates(), RectangleFloat.create(0, 0, width, height), sampleParameters.samples());
        for (var v : g.getVertices()) {
            if (!gs.getPlacedVertices().contains(v)) {
                if (role == NewPlayer.Role.MAX) {
                    samples.ifPresent(s -> gameMove = chooseHighestIntersection(List.of(v), s));
                } else {
                    samples.ifPresent(s -> gameMove = chooseLowestIntersection(List.of(v), s));
                }
                break;
            }
        }

        super.stopExecuteStrategy();
        return true;
    }


    /**
     * This should return a fixed strategy name
     * which is used by the GameObserver.
     *
     * @return A strategy name
     */
    @Override
    public StrategyName getStrategyName() {
        return (role == NewPlayer.Role.MAX) ? StrategyName.MaximizeRandomSampleMove : StrategyName.MinimizeRandomSampleMove;
    }
}
