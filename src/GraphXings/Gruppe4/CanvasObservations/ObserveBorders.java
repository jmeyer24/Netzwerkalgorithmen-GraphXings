package GraphXings.Gruppe4.CanvasObservations;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Data.Graph;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservation;
import GraphXings.Gruppe4.Common.Helper;
import GraphXings.Gruppe4.Strategies.StrategyName;

import java.util.ArrayList;
import java.util.List;

public class ObserveBorders implements CanvasObservation {

    private final Graph g;
    private final List<GameMove> ourMoves;
    private final List<GameMove> opponentMoves;
    private final NewPlayer.Role ourRole;
    private int width;
    private int height;
    private NewPlayer.Role[][] usedCoordinatesRole;
    private GameState gs;
    private int observation = 0;

    public ObserveBorders(Graph g, List<GameMove> ourMoves, List<GameMove> opponentMoves, NewPlayer.Role role, GameState gs, NewPlayer.Role[][] usedCoordinatesRole, int width, int height) {
        this.g = g;
        this.ourMoves = ourMoves;
        this.opponentMoves = opponentMoves;
        this.ourRole = role;
        this.width = width;
        this.height = height;
        this.gs = gs;
        this.usedCoordinatesRole = usedCoordinatesRole;
    }

    /**
     * Calculate the observation strategy.
     *
     * @param lastMove The last move of the game.
     */
    @Override
    public void calculateObservation(GameMove lastMove) {
        var opponentRole = (ourRole == NewPlayer.Role.MAX) ? NewPlayer.Role.MIN : NewPlayer.Role.MAX;

        int borderCount = 0;

        // Check north and south border
        for (int w = 0; w < width; w++) {
            var north = !Helper.isCoordinateFree(gs.getUsedCoordinates(), w, 0);
            var south = !Helper.isCoordinateFree(gs.getUsedCoordinates(), w, height - 1);

            if (north && usedCoordinatesRole[w][0] == opponentRole) {
                borderCount++;
            } else if (south && usedCoordinatesRole[w][height - 1] == opponentRole) {
                borderCount++;
            }
        }

        // Check east and west border
        for (int h = 0; h < height; h++) {
            var east = Helper.isCoordinateFree(gs.getUsedCoordinates(), 0, h);
            var west = Helper.isCoordinateFree(gs.getUsedCoordinates(), width - 1, h);

            if (east && usedCoordinatesRole[0][h] == opponentRole) {
                borderCount++;
            } else if (west && usedCoordinatesRole[width - 1][h] == opponentRole) {
                borderCount++;
            }
        }

        // Calculate percentage
        observation = (int)(borderCount / (double)((ArrayList<GameMove>) opponentMoves).size() * 100);
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
        // TODO: Return effective strategy
        if (ourRole == NewPlayer.Role.MAX) {
            return StrategyName.MaximizeRandomSampleMove;
        } else {
            return StrategyName.MinimizePlaceAtBorder;
        }
    }
}
