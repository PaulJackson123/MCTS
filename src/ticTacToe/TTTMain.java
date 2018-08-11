package ticTacToe;

import main.FinalSelectionPolicy;
import main.MCTS;
import main.Move;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;

class TTTMain {

	private static boolean[] humanPlayer = {false, true};

	public static void main(String[] args) {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(0.2);
		mcts.setTimeDisplay(true);
		Move move;
		mcts.setOptimisticBias(0.0d);
		mcts.setPessimisticBias(0.0d);
		mcts.setMoveSelectionPolicy(FinalSelectionPolicy.robustChild);
		int[] scores = new int[3];

		for (int i = 0; i < 100; i++) {
			TicTacToe ttt = new TicTacToe();
			while (!ttt.gameOver()) {
				if (humanPlayer[ttt.currentPlayer]) {
					move = getHumanMove(ttt);
				} else {
					move = mcts.runMCTS_UCT(ttt, 1000000, 0L, false);
				}
				ttt.makeMove(move);
			}

			System.out.println("---");
			ttt.bPrint();

			double[] scr = ttt.getScore();
			if (scr[0] > 0.9) {
				scores[0]++; // player 1
			} else if (scr[1] > 0.9) {
				scores[1]++; // player 2
			} else {
				scores[2]++; // draw
			}

			System.out.println(Arrays.toString(scr));
			System.out.println(Arrays.toString(scores));
		}
	}

	private static Move getHumanMove(final TicTacToe ttt) {
		System.out.println(ttt);
		int x = readInt("Enter column (0-2): ");
		int y = readInt("Enter row (0-2): ");
		return new TicTacToeMove(x, y);
	}

	private static int readInt(final String message) {
		int i = -1;
		while (i == -1) {
			try {
				System.out.print(message);
				LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in));
				i = Integer.parseInt(reader.readLine());
			} catch (IOException ignore) {
			}
		}
		return i;
	}
}
