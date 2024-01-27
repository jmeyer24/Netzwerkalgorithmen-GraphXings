package GraphXings.Gruppe4;

import GraphXings.Game.GameMove;
import GraphXings.Gruppe4.Strategies.StrategyName;

import java.util.Optional;

public interface Strategy {

    /**
     * Executes the heuristic as the first or second move.
     * @param lastMove Is empty on first move otherwise provides the last opponent game move.
     * @return True on success, false otherwise.
     */
    public boolean executeHeuristic(Optional<GameMove> lastMove);

    /**
     * Execute the main strategy and calculate a game move.
     * The move must be stored and retrievable by getGameMove.
     * @param lastMove The last opponent move.
     * @return True on success, false otherwise
     */
    public boolean executeStrategy(GameMove lastMove);

    /**
     * Retrieve a calculated game move.
     * @return A game move. Empty if execution wasn't successful.
     */
    public Optional<GameMove> getGameMove();

    /**
     * Quality of the current game move.
     * This number represents how many crossings can be achieved by a game move.
     * For a maximizer this number should be large.
     * @return Number of crossings.
     */
    public long getGameMoveQuality();

    /**
     * This should return a fixed strategy name
     * which is used by the GameObserver.
     * @return A strategy name
     */
    public StrategyName getStrategyName();
}
