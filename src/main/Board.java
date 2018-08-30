package main;

import java.util.List;

public interface Board {

	/**
	 * Create one copy of the board. It is important that the copies do
	 * not store references to objects shared by other boards unless
	 * those objects are immutable.
	 */
	Board duplicate();

	/**
	 * Get a list of all available moves for the current state. MCTS
	 * calls this to know what actions are possible at that point.
	 * <p>
	 * The location parameter indicates from where in the algorithm
	 * the method was called. Can be either treePolicy or playout.
	 */
	List<Move> getMoves();

	/**
	 * Apply the move m to the current state of the board.
	 */
	void makeMove(Move m);

	/**
	 * Returns true if the game is over.
	 */
	boolean gameOver();

	/**
	 * Returns the player ID for the player whose turn is active. This method is
	 * called by the MCTS.
	 */
	int getCurrentPlayer();

	/**
	 * Returns the number of players.
	 */
	int getQuantityOfPlayers();

	/**
	 * Returns a score vector.
	 * [1.0, 0.0] indicates a win for player 0.
	 * [0.0, 1.0] indicates a win for player 1
	 * [0.5, 0.5] indicates a draw
	 *
	 * @return score array
	 */
	double[] getScore();

	/**
	 * Returns an array of probability weights
	 * for each move possible on this board. This
	 * is only relevant in board states where
	 * the choice to make is a random choice.
	 *
	 * @return array of weights
	 */
	double[] getMoveWeights();

	void bPrint();

	/**
	 * Return the player that would play next if
	 * the current player played the given move
	 *
	 * @param move A move by the current player. Games may ignore
	 *             this if the order of play is pre-determined
	 * @return The next player
	 */
	default int getNextPlayer(Move move) {
		Board tempBoard = duplicate();
		tempBoard.makeMove(move);
		return tempBoard.getCurrentPlayer();
	}
}