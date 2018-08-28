package dylanean;

import main.MCTS;
import main.Move;

public class DCTestEnd {
	public static void main(String[] args) {
		int[][] board = new int[12][6];
		board[11][5] = 4;
		board[9][3] = -3;
		board[8][4] = -4;
		DylaneanChess chess = new DylaneanChess(board, 0);

		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(0.36);
		mcts.setTimeDisplay(true);

		int total1 = 0, total2 = 0;

		for (int i = 0; i < 100; i++) {
			DCMove move = (DCMove) mcts.runMCTS_UCT(chess, 1000, 0);

			DCMove ok1 = new DCMove(3, 9, 3, 11, 3);
			DCMove ok2 = new DCMove(3, 9, 3, 9, 5);
			if (move.equals(ok1)) {
				total1++;
			} else if (move.equals(ok2)) {
				total2++;
			} else {
				throw new IllegalArgumentException("Unexpected move: " + move);
			}
		}

		System.out.println("Total1=" + total1 + ", Total2=" + total2);
	}
}
