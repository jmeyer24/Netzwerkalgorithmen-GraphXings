package GraphXings.Game;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import GraphXings.Algorithms.CrossingCalculator;
import GraphXings.Algorithms.NewPlayer;
import GraphXings.Data.Coordinate;
import GraphXings.Data.Graph;
import GraphXings.NewFiles.GraphPanel;

/**
 * A class for managing a game of GraphXings!
 */
public class NewGame {

	/**
	 * True: Shows gui
	 * False: does not show gui
	 */
	private Boolean showGui = true;

	/**
	 * True: a pause between each vertex placement
	 * False: no pause
	 */
	private Boolean timerOn = false; 

	private GraphPanel graphPanel;
	JFrame frame = new JFrame("Graph Panel");
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
		if(showGui) {
			this.graphPanel = new GraphPanel();
			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JScrollPane scrollPane = new JScrollPane(graphPanel);
			scrollPane.setPreferredSize(new Dimension(700, 700));  // Set the initial size of the GraphPane
			
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
		graphPanel.changeReadyState(false);

		try {
			player1.initializeNextRound(g.copy(), width, height, NewPlayer.Role.MAX);
			player2.initializeNextRound(g.copy(), width, height, NewPlayer.Role.MIN);
			int crossingsGame1 = playRound(player1, player2);
			if(showGui) {
				try {
					graphPanel.clearPanel();
					coordinateList.clear();
					Thread.sleep(5000);
				
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			player1.initializeNextRound(g.copy(), width, height, NewPlayer.Role.MIN);
			player2.initializeNextRound(g.copy(), width, height, NewPlayer.Role.MAX);
			int crossingsGame2 = playRound(player2, player1);
			return new NewGameResult(crossingsGame1, crossingsGame2, player1, player2, false, false, false, false);
		} catch (NewInvalidMoveException ex) {
			System.err.println(ex.getCheater().getName() + " cheated!");
			if (ex.getCheater().equals(player1)) {
				return new NewGameResult(0, 0, player1, player2, true, false, false, false);
			} else if (ex.getCheater().equals(player2)) {
				return new NewGameResult(0, 0, player1, player2, false, true, false, false);
			} else {
				return new NewGameResult(0, 0, player1, player2, false, false, false, false);
			}
		} catch (NewTimeOutException ex) {
			System.err.println(ex.getTimeOutPlayer().getName() + " ran out of time!");
			if (ex.getTimeOutPlayer().equals(player1)) {
				return new NewGameResult(0, 0, player1, player2, false, false, true, false);
			} else if (ex.getTimeOutPlayer().equals(player2)) {
				return new NewGameResult(0, 0, player1, player2, false, false, false, true);
			} else {
				return new NewGameResult(0, 0, player1, player2, false, false, false, false);
			}

		}
	}

	/**
	 * Plays a single round of the game.
	 * 
	 * @param maximizer The player with the goal to maximize the number of
	 *                  crossings.
	 * @param minimizer The player with the goal to minimize the number of crossings
	 * @return The number of crossings yielded in the final drawing.
	 * @throws InvalidMoveException An exception caused by cheating.
	 */
	private int playRound(NewPlayer maximizer, NewPlayer minimizer)
			throws NewInvalidMoveException, NewTimeOutException {
		int turn = 0;
		GameState gs = new GameState(width, height);
		GameMove lastMove = null;
		long timeMaximizer = 0;
		long timeMinimizer = 0;
		long startTime = System.nanoTime();
		while (turn < g.getN()) {
			GameMove newMove;
			if (turn % 2 == 0) {
				long moveStartTime = System.nanoTime();
				newMove = maximizer.maximizeCrossings(lastMove);
				timeMaximizer += System.nanoTime() - moveStartTime;
				if (timeMaximizer > timeLimit) {
					throw new NewTimeOutException(maximizer);
				}
				if (!gs.checkMoveValidity(newMove)) {
					throw new NewInvalidMoveException(maximizer);
				}
			} else {
				long moveStartTime = System.nanoTime();
				newMove = minimizer.minimizeCrossings(lastMove);
				timeMinimizer += System.nanoTime() - moveStartTime;
				if (timeMinimizer > timeLimit) {
					throw new NewTimeOutException(minimizer);
				}
				if (!gs.checkMoveValidity(newMove)) {
					throw new NewInvalidMoveException(minimizer);
				}
			}

		

			gs.applyMove(newMove);
			if(showGui) {
				Coordinate c = new Coordinate(gs.getVertexCoordinates().get(newMove.getVertex()).getX(), gs.getVertexCoordinates().get(newMove.getVertex()).getY());
				coordinateList.add(c);
				graphPanel.setCoordinates(coordinateList);
				
				if(timerOn) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
			lastMove = newMove;
			turn++;
		}
		System.out.println((System.nanoTime() - startTime) / 1000000 + "ms");
		graphPanel.changeReadyState(true);
		CrossingCalculator cc = new CrossingCalculator(g, gs.getVertexCoordinates());
		
		return cc.computeCrossingNumber();
	}
}
