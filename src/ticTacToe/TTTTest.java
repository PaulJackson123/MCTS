package ticTacToe;

import main.FinalSelectionPolicy;
import main.MCTS;
import main.Move;

class TTTTest {
	public static void main(String[] args) {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(0.2);
		mcts.setTimeDisplay(true);
		mcts.setOptimisticBias(0.0d);
		mcts.setPessimisticBias(0.0d);
		mcts.setMoveSelectionPolicy(FinalSelectionPolicy.robustChild);

		TicTacToe ttt = new TicTacToe();
		ttt.makeMove(new TicTacToeMove(2, 1));
		ttt.makeMove(new TicTacToeMove(1, 1));
		ttt.makeMove(new TicTacToeMove(0, 0));
		ttt.makeMove(new TicTacToeMove(1, 2));
		ttt.makeMove(new TicTacToeMove(1, 0));
		ttt.makeMove(new TicTacToeMove(0, 2));

		for (int i = 0; i < 100; i++) {
			TicTacToeMove move = (TicTacToeMove) mcts.runMCTS_UCT(ttt, 1000000, 0L, false);
			if (move.x != 2 || move.y != 0) {
				throw new IllegalStateException();
			}
		}
	}
}
