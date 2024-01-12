package GraphXings.Game;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import GraphXings.Algorithms.CrossingCalculator;
import GraphXings.Algorithms.NewPlayer;
import GraphXings.Data.Coordinate;
import GraphXings.Data.Graph;
import GraphXings.Legacy.Game.InvalidMoveException;

import java.util.Random;
import GraphXings.NewFiles.GraphPanel;

/**
 * A class for managing a game of GraphXings!
 */
public class NewGame {
	/**
	 * A set of objectives that a game may have.
	 * CROSSING_NUMBER - The objective function is the crossing number.
	 * CROSSING_ANGLE - The objective function is the sum of the squared cosines of
	 * the crossing angles.
	 */
	public enum Objective {
		CROSSING_NUMBER, CROSSING_ANGLE
	};

	/**
	 * The objective function of the game.
	 */
	private Objective objective;

	/**
	 * @true: Shows gui
	 * @false: does not show gui
	 */
	private boolean showGui = false;

	/**
	 * @true: a pause between each vertex placement
	 * @false: no pause
	 */
	private boolean timerOn = true;
	private int sleepTimer = 0; // in miliseconds
	private int pauseBetweenGames = 10000; // in milis
	/**
	 * @true: shows edges in gui
	 * @false: does not show edges in gui
	 */
	private boolean setEdges = true;

	private GraphPanel graphPanel;
	JFrame frame;
	ArrayList<Coordinate> coordinateList = new ArrayList<Coordinate>();

	/**
	 * The width of the game board.
	 */
	private int width;
	/**
	 * The height of the game board.
	 */
	private int height;
	/**
	 * The graph to be drawn.
	 */
	private Graph g;
	/**
	 * The first player.
	 */
	private NewPlayer player1;
	/**
	 * The second player.
	 */
	private NewPlayer player2;
	/**
	 * The time limit for players.
	 */
	private long timeLimit;

	/**
	 * Instantiates a game of GraphXings.
	 * 
	 * @param g       The graph to be drawn.
	 * @param width   The width of the game board.
	 * @param height  The height of the game board.
	 * @param player1 The first player. Plays as the maximizer in round one.
	 * @param player2 The second player. Plays as the minimizer in round one.
	 */
	public NewGame(Graph g, int width, int height, NewPlayer player1, NewPlayer player2) {
		this.g = g;
		this.width = width;
		this.height = height;
		this.player1 = player1;
		this.player2 = player2;
		this.timeLimit = Long.MAX_VALUE;
		this.objective = Objective.CROSSING_NUMBER;
	}

	/**
	 * Instantiates a game of GraphXings.
	 * 
	 * @param g         The graph to be drawn.
	 * @param width     The width of the game board.
	 * @param height    The height of the game board.
	 * @param player1   The first player. Plays as the maximizer in round one.
	 * @param player2   The second player. Plays as the minimizer in round one.
	 * @param timeLimit The time limit for players.
	 */
	public NewGame(Graph g, int width, int height, NewPlayer player1, NewPlayer player2, long timeLimit) {
		this.g = g;
		this.width = width;
		this.height = height;
		this.player1 = player1;
		this.player2 = player2;
		this.timeLimit = timeLimit;
		this.objective = Objective.CROSSING_NUMBER;
	}

	/**
	 * Instantiates a game of GraphXings.
	 * 
	 * @param g         The graph to be drawn.
	 * @param width     The width of the game board.
	 * @param height    The height of the game board.
	 * @param player1   The first player. Plays as the maximizer in round one.
	 * @param player2   The second player. Plays as the minimizer in round one.
	 * @param timeLimit The time limit for players.
	 * @param objective The objective of the current game.
	 */
	public NewGame(Graph g, int width, int height, NewPlayer player1, NewPlayer player2, Objective objective,
			long timeLimit) {
		this.g = g;
		this.width = width;
		this.height = height;
		this.player1 = player1;
		this.player2 = player2;
		this.timeLimit = timeLimit;
		this.objective = objective;
		if (showGui) {
			this.frame = new JFrame("Graph Panel");
			this.graphPanel = new GraphPanel();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JScrollPane scrollPane = new JScrollPane(graphPanel);
			scrollPane.setPreferredSize(new Dimension(700, 700)); // Set the initial size of the GraphPane
			frame.getContentPane().add(scrollPane);
			frame.pack();
			frame.setVisible(true);
		}
	}

	/**
	 * Runs the full game of GraphXings.
	 * 
	 * @return Provides a GameResult Object containing the game's results.
	 */
	public NewGameResult play() {
		Random r = new Random(System.nanoTime());
		if (r.nextBoolean()) {
			NewPlayer swap = player1;
			player1 = player2;
			player2 = swap;
		}
		if (showGui) {
			graphPanel.showEdges(setEdges);
			graphPanel.changeReadyState(false);
		}
		try {
			double crossingsGame1 = playRound(player1, player2);
			// Between Games
			if (showGui) {
				try {
					Thread.sleep(pauseBetweenGames);
					graphPanel.resetZoom();
					graphPanel.clearPanel();
					coordinateList.clear();

				} catch (InterruptedException e) {
					graphPanel.changeReadyState(false);
					e.printStackTrace();
				}
				graphPanel.changeReadyState(false);
			}
			double crossingsGame2 = playRound(player2, player1);
			return new NewGameResult(crossingsGame1, crossingsGame2, player1, player2, false, false, false, false,
					objective);
		} catch (NewInvalidMoveException ex) {
			System.out.println("E001:" + ex.getCheater().getName() + " cheated!");
			if (ex.getCheater().equals(player1)) {
				return new NewGameResult(0, 0, player1, player2, true, false, false, false, objective);
			} else if (ex.getCheater().equals(player2)) {
				return new NewGameResult(0, 0, player1, player2, false, true, false, false, objective);
			} else {
				return new NewGameResult(0, 0, player1, player2, false, false, false, false, objective);
			}
		} catch (NewTimeOutException ex) {
			System.out.println("E002:" + ex.getTimeOutPlayer().getName() + " ran out of time!");
			if (ex.getTimeOutPlayer().equals(player1)) {
				return new NewGameResult(0, 0, player1, player2, false, false, true, false, objective);
			} else if (ex.getTimeOutPlayer().equals(player2)) {
				return new NewGameResult(0, 0, player1, player2, false, false, false, true, objective);
			} else {
				return new NewGameResult(0, 0, player1, player2, false, false, false, false, objective);
			}

		}
	}

	/**
	 * Plays a single round of the game.
	 * 
	 * @param maximizer The player with the goal to maximize the number of
	 *                  crossings.
	 * @param minimizer The player with the goal to minimize the number of crossings
	 * @return The score yielded by the final drawing.
	 * @throws InvalidMoveException An exception caused by cheating.
	 */
	private double playRound(NewPlayer maximizer, NewPlayer minimizer)
			throws NewInvalidMoveException, NewTimeOutException {
		int turn = 0;
		GameState gs = new GameState(g, width, height);
		GameMove lastMove = null;
		long timeMaximizer = 0;
		long timeMinimizer = 0;
		long initStartTimeMax = System.nanoTime();
		NewPlayer.Role maximizerRole;
		NewPlayer.Role minimizerRole;
		if (objective.equals(Objective.CROSSING_NUMBER)) {
			maximizerRole = NewPlayer.Role.MAX;
			minimizerRole = NewPlayer.Role.MIN;
		} else {
			maximizerRole = NewPlayer.Role.MAX_ANGLE;
			minimizerRole = NewPlayer.Role.MIN_ANGLE;
		}
		maximizer.initializeNextRound(g.copy(), width, height, maximizerRole);
		timeMaximizer += System.nanoTime() - initStartTimeMax;
		if (timeMaximizer > timeLimit) {
			throw new NewTimeOutException(maximizer);
		}
		long initStartTimeMin = System.nanoTime();
		minimizer.initializeNextRound(g.copy(), width, height, minimizerRole);
		timeMinimizer += System.nanoTime() - initStartTimeMin;
		if (timeMinimizer > timeLimit) {
			throw new NewTimeOutException(minimizer);
		}
		while (turn < g.getN()) {
			GameMove newMove;
			if (turn % 2 == 0) {
				long moveStartTime = System.nanoTime();
				try {
					if (objective.equals(Objective.CROSSING_NUMBER)) {
						newMove = maximizer.maximizeCrossings(lastMove);
					} else {
						newMove = maximizer.maximizeCrossingAngles(lastMove);
					}
				} catch (Exception ex) {
					System.out.println("E003:" + maximizer.getName() + " threw a " + ex.getClass() + " exception!");
					throw new NewInvalidMoveException(maximizer);
				}
				timeMaximizer += System.nanoTime() - moveStartTime;
				if (timeMaximizer > timeLimit) {
					throw new NewTimeOutException(maximizer);
				}
				if (!gs.checkMoveValidity(newMove)) {
					throw new NewInvalidMoveException(maximizer);
				}
			} else {
				long moveStartTime = System.nanoTime();
				try {
					if (objective.equals(Objective.CROSSING_NUMBER)) {
						newMove = minimizer.minimizeCrossings(lastMove);
					} else {
						newMove = minimizer.minimizeCrossingAngles(lastMove);
					}
				} catch (Exception ex) {
					System.out.println("E004:" + minimizer.getName() + " threw a " + ex.getClass() + " exception!");
					throw new NewInvalidMoveException(minimizer);
				}
				timeMinimizer += System.nanoTime() - moveStartTime;
				if (timeMinimizer > timeLimit) {
					throw new NewTimeOutException(minimizer);
				}
				if (!gs.checkMoveValidity(newMove)) {
					throw new NewInvalidMoveException(minimizer);
				}
			}

			gs.applyMove(newMove);
			if (showGui == true) {
				Coordinate c = new Coordinate(gs.getVertexCoordinates().get(newMove.getVertex()).getX(),
						gs.getVertexCoordinates().get(newMove.getVertex()).getY());
				coordinateList.add(c);
				graphPanel.setCoordinates(coordinateList);

				if (setEdges == true) {
					graphPanel.setEdges(g, gs.getPlacedVertices(), gs.getVertexCoordinates());
				}

				if (timerOn) {
					try {
						Thread.sleep(sleepTimer);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
			lastMove = newMove;
			turn++;
		}
		// System.out.println((System.nanoTime() - startTime) / 1000000 + "ms");
		if (showGui == true) {
			graphPanel.changeReadyState(true);
		}
		CrossingCalculator cc = new CrossingCalculator(g, gs.getVertexCoordinates());
		if (objective.equals(Objective.CROSSING_NUMBER)) {
			return cc.computeCrossingNumber();
		} else {
			return cc.computeSumOfSquaredCosinesOfCrossingAngles();
		}
	}
}
