package GraphXings.Algorithms;

import GraphXings.Data.Graph;
import GraphXings.Game.GameMove;


public interface NewPlayer
{
	/**
	 * Tells the player to perform the next move where the objective is to create crossings! In a valid move, an unplaced vertex of g is positioned onto a free coordinate.
	 * @param lastMove The last move performed by the opposing player. Is null if it is the first move!
	 * @return The move to be performed.
	 */
	public GameMove maximizeCrossings(GameMove lastMove);
	/**
	 * Tells the player to perform the next move where the objective is to avoid crossings! In a valid move, an unplaced vertex of g is positioned onto a free coordinate.
	 * @param lastMove The last move performed by the opposing player. Is null if it is the first move!
	 * @return The move to be performed.
	 */
	public GameMove minimizeCrossings(GameMove lastMove);

	/**
	 * Tells the player to get ready for the next round.
	 * @param g The graph to be drawn in the next round.
	 * @param width The width of the game board.
	 * @param height The height of the game board.
	 * @param role The role in the next round, either MAX or MIN.
	 */
	public void initializeNextRound(Graph g, int width, int height, Role role);
	/**
	 * Gets the name of the player.
	 * @return The player's name.
	 */
	public String getName();

	/**
	 * An enum describing the role of the player in the next round.
	 */
	public enum Role {MAX,MIN};
}
