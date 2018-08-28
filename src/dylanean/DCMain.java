package dylanean;

import main.MCTS;
import main.Move;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;

class DCMain {

	private static final int RUNS = 0;
	private static final long MAX_TIME = 20_000L;
	private static boolean[] humanPlayer = {true, false};

	public static void main(String[] args) {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(0.36);
		mcts.setTimeDisplay(true);
		Move move;
		int[] scores = new int[3];

		DylaneanChess dc = new DylaneanChess();
		while (!dc.gameOver()) {
			if (humanPlayer[dc.getCurrentPlayer()]) {
				move = getHumanMove(dc);
			} else {
				dc.bPrint();
				move = mcts.runMCTS_UCT(dc, RUNS, MAX_TIME);
			}
			dc.makeMove(move);
		}

		System.out.println("---");
		dc.bPrint();

		double[] scr = dc.getScore();
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

	private static Move getHumanMove(final DylaneanChess dc) {
		do {
			dc.bPrint();
			System.out.println("Player " + dc.getCurrentPlayer() + "'s turn.");
			int piece = readInt("Enter piece (1=soldier, 2=archer, 3=knight, 4=king): ");
			int rank = Character.toLowerCase(readChar("Enter rank (A-L): ")) - 'a';
			int file = readInt("Enter file (0-5): ");
			String error = dc.isSetupMoveLegal(piece, rank, file);
			if (error == null) {
				return new DCSetupMove(piece, rank, file);
			} else {
				System.out.println(error);
			}
		} while (true);
	}

	private static int readInt(final String message) {
		int i = -1;
		while (i == -1) {
			try {
				System.out.print(message);
				LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in));
				i = Integer.parseInt(reader.readLine());
			} catch (NumberFormatException | IOException ignore) {
			}
		}
		return i;
	}

	private static char readChar(final String message) {
		char c = 0;
		while (c == 0) {
			try {
				System.out.print(message);
				LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in));
				String s = reader.readLine();
				if (s == null || s.length() != 1) {
					throw new IllegalArgumentException();
				}
				c = s.charAt(0);
			} catch (NumberFormatException | IOException ignore) {
			}
		}
		return c;
	}
}
