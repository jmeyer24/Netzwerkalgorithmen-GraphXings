package GraphXings.Gruppe4.CanvasObservations;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Data.Graph;
import GraphXings.Game.GameMove;
import GraphXings.Gruppe4.CanvasObservation;
import GraphXings.Gruppe4.Strategies.StrategyName;

import java.util.List;

/**
 * This observation returns 100 if the opponent places a vertex which is connected
 * by an edge of our last placed vertex.
 * This metric doesn't take the position on the canvas into account.
 */
public class ObserveOpponentPlacesNeighbours implements CanvasObservation {

    private final Graph g;
    private final List<GameMove> ourMoves;
    private final List<GameMove> opponentMoves;
    private final NewPlayer.Role ourRole;

    private int observation = 0;

    public ObserveOpponentPlacesNeighbours(Graph g, List<GameMove> ourMoves, List<GameMove> opponentMoves, NewPlayer.Role role) {
        this.g = g;
        this.ourMoves = ourMoves;
        this.opponentMoves = opponentMoves;
        this.ourRole = role;
    }

    /**
     * Calculate the observation strategy.
     *
     * @param lastMove The last move of the game.
     */
    @Override
    public void calculateObservation(GameMove lastMove) {
        // Last move is the one from opponent
        var ourLastMove = ourMoves.getLast();

        var incidentEdges = g.getIncidentEdges(ourLastMove.getVertex());
        for (var e : incidentEdges) {
            if ((e.getS() != null && e.getS().equals(lastMove.getVertex())) || (e.getT() != null && e.getT().equals(lastMove.getVertex()))) {
                observation = 100;
                break;
            }
        }
    }

    /**
     * Retrieve the observation number between 0-100.
     * This value represents the matching percentage.
     *
     * @return An integer between 0-100
     */
    @Override
    public int getObservation() {
        return observation;
    }

    /**
     * If a strategy was observed get a effective counter-attack.
     *
     * @return a strategy name
     */
    @Override
    public StrategyName getEffectiveCounterStrategy() {
        // TODO: Evaluate which strategy is effective. This is just a random value
        return StrategyName.MaximizeDiagonalCrossing;
    }
}
