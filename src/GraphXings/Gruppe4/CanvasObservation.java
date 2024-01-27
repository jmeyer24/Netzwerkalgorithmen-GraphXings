package GraphXings.Gruppe4;

import GraphXings.Game.GameMove;
import GraphXings.Gruppe4.Strategies.StrategyName;

public interface CanvasObservation {

    /**
     * Calculate the observation strategy.
     * @param lastMove The last move of the game.
     */
    public void calculateObservation(GameMove lastMove);

    /**
     * Retrieve the observation number between 0-100.
     * This value represents the matching percentage.
     * @return An integer between 0-100
     */
    public int getObservation();

    /**
     * If a strategy was observed get a effective counter-attack.
     * @return a strategy name
     */
    public StrategyName getEffectiveCounterStrategy();
}
