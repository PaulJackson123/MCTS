package main.support;

import main.Board;
import main.Move;

/**
 * Create a class implementing this interface and instantiate
 * it. Pass the instance to the MCTS instance using the
 * {@link main.MCTS#setHeuristicFunction(HeuristicFunction h) setHeuristicFunction} method.
 */
public interface HeuristicFunction {
	double h(Board board, Move move);
}
