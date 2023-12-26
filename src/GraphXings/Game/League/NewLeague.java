package GraphXings.Game.League;

import GraphXings.Algorithms.NewPlayer;
import GraphXings.Game.GameInstance.GameInstanceFactory;
import GraphXings.Game.Match.NewMatch;
import GraphXings.Game.Match.NewMatchResult;

import java.util.HashMap;
import java.util.List;

/**
 * A class for managing a league of GraphXings!
 */
public class NewLeague {
	/**
	 * The participating players
	 */
	private List<NewPlayer> players;
	/**
	 * The number of games per match.
	 */
	private int bestOf;
	/**
	 * The time limit per game round.
	 */
	private long timeLimit;
	/**
	 * A factory for creating game instances.
	 */
	private GameInstanceFactory factory;
	/**
	 * The amount of points obtained for winning a game (3 by default).
	 */
	private int pointsPerGameWon;
	/**
	 * The amount of points obtained for scoring a tie (1 by default).
	 */
	private int pointsPerTie;
	/**
	 * If true, print all match and game results (true by default).
	 */
	private boolean isVerbose;

	/**
	 * The type of matches to be played in the league.
	 */
	private NewMatch.MatchType matchType;
	/**
	 * The seed to be used for calling constructors of matches.
	 */
	private long seed;
	/**
	 * Creates a new league object.
	 * 
	 * @param players A list of the participating players.
	 * @param bestOf  The number of games per match.
	 * @param factory The factory for creating game instances.
	 */
	public NewLeague(List<NewPlayer> players, int bestOf, GameInstanceFactory factory) {
		this.players = players;
		this.bestOf = bestOf;
		this.timeLimit = Long.MAX_VALUE;
		this.factory = factory;
		pointsPerGameWon = 3;
		pointsPerTie = 1;
		isVerbose = true;
		matchType = NewMatch.MatchType.CROSSING_NUMBER;
		this.seed = System.nanoTime();
	}

	/**
	 * Creates a new league object.
	 * 
	 * @param players   A list of the participating players.
	 * @param bestOf    The number of games per match.
	 * @param factory   The factory for creating game instances.
	 * @param timeLimit The time limit per game round.
	 */
	public NewLeague(List<NewPlayer> players, int bestOf, long timeLimit, GameInstanceFactory factory) {
		this.players = players;
		this.bestOf = bestOf;
		this.timeLimit = timeLimit;
		this.factory = factory;
		pointsPerGameWon = 3;
		pointsPerTie = 1;
		isVerbose = true;
		this.matchType = NewMatch.MatchType.CROSSING_NUMBER;
		this.seed = System.nanoTime();
	}

	/**
	 * Creates a new league object.
	 * @param players A list of the participating players.
	 * @param bestOf The number of games per match.
	 * @param factory The factory for creating game instances.
	 * @param timeLimit The time limit per game round.
	 * @param matchType The type of matches to be played in the league.
	 */
	public NewLeague(List<NewPlayer> players, int bestOf, long timeLimit, GameInstanceFactory factory, NewMatch.MatchType matchType)
	{
		this.players = players;
		this.bestOf = bestOf;
		this.timeLimit = timeLimit;
		this.factory = factory;
		pointsPerGameWon = 3;
		pointsPerTie = 1;
		isVerbose = true;
		this.matchType = matchType;
		this.seed = System.nanoTime();
	}

	/**
	 * Creates a new league object.
	 * @param players A list of the participating players.
	 * @param bestOf The number of games per match.
	 * @param factory The factory for creating game instances.
	 * @param timeLimit The time limit per game round.
	 * @param matchType The type of matches to be played in the league.
	 * @param seed The seed to be used by matches to select the game's objective if matchType is MIXED.
	 */
	public NewLeague(List<NewPlayer> players, int bestOf, long timeLimit, GameInstanceFactory factory, NewMatch.MatchType matchType, long seed)
	{
		this.players = players;
		this.bestOf = bestOf;
		this.timeLimit = timeLimit;
		this.factory = factory;
		pointsPerGameWon = 3;
		pointsPerTie = 1;
		isVerbose = true;
		this.matchType = matchType;
		this.seed = seed;
	}

	/**
	 * Runs all matches of the league.
	 * 
	 * @return An object storing the results of the league.
	 */
	public NewLeagueResult runLeague() {
		HashMap<NewPlayer, Integer> leaguePoints = new HashMap<>();
		for (int i = 0; i < players.size(); i++) {
			leaguePoints.put(players.get(i), 0);
		}
		int matchNumber = 1;
		for (int i = 0; i < players.size(); i++) {
			for (int j = 0; j < i; j++) {
				NewPlayer player1 = players.get(i);
				NewPlayer player2 = players.get(j);
				NewMatch m = new NewMatch(player1, player2, factory, bestOf, timeLimit,matchType,seed);
				m.setVerbose(isVerbose);
				if (isVerbose) {
					System.out.println("Match " + matchNumber + ": " + player1.getName() + " vs. " + player2.getName());
				}
				NewMatchResult mr = m.play();
				if (isVerbose) {
					System.out.println("Match " + matchNumber++ + ": " + mr.announceResult());
				}
				NewPlayer winner = mr.getWinner();
				if (winner != null) {
					leaguePoints.put(winner, leaguePoints.get(winner) + pointsPerGameWon);
				} else {
					leaguePoints.put(player1, leaguePoints.get(player1) + pointsPerTie);
					leaguePoints.put(player2, leaguePoints.get(player2) + pointsPerTie);
				}
			}
			// garbage collect, else we get a heap error
			// this is apparently not sure to work out
			System.gc();
		}
		return new NewLeagueResult(leaguePoints);
	}

	/**
	 * Sets how many league points should be awarded per game won.
	 * 
	 * @param points The number of points to be awarded per game won.
	 */
	public void setPointsPerGameWon(int points) {
		this.pointsPerGameWon = points;
	}

	/**
	 * Sets how many league points should be awarded per tie.
	 * 
	 * @param points The number of points to be awarded per tie.
	 */
	public void setPointsPerTie(int points) {
		this.pointsPerTie = points;
	}

	/**
	 * Sets whether or not all intermediate match and game results should be
	 * displayed.
	 * 
	 * @param verbose True, if intermediate results should be displayed, false
	 *                otherwise.
	 */
	public void setVerbose(boolean verbose) {
		this.isVerbose = verbose;
	}
}
