package dylanean;

import main.MCTS;
import main.Move;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;

class DCMain {

	private static final int RUNS = 0;
	private static final long MAX_TIME = 10_000L;
	private static boolean[] humanPlayer = {true, false};

	public static void main(String[] args) {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(0.36);
		mcts.setTimeDisplay(true);
		Move move;
		int[] scores = new int[3];

		DylaneanChess dc = new DylaneanChess();
		dc = newMidGame();
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
			String error;
			if (dc.isSetupPhase()) {
				int piece = readInt("Enter piece (1=soldier, 2=archer, 3=knight): ");
				if (piece < 1 || piece > 3) {
					System.out.println("Piece must be in range 1-3");
					continue;
				}
				int rank = Character.toLowerCase(readChar("Enter rank (A-L): ")) - 'a';
				if (rank < 0 || rank >= 12) {
					System.out.println("Rank must be in range A-L");
					continue;
				}
				int file = readInt("Enter file (0-5): ");
				if (file < 0 || file >= 6) {
					System.out.println("File must be in range 0-5");
					continue;
				}
				error = dc.isSetupMoveLegal(piece, rank, file);
				if (error == null) {
					return new DCSetupMove(piece, rank, file);
				}
			} else {
				int fromRank = Character.toLowerCase(readChar("Enter rank (A-L): ")) - 'a';
				if (fromRank < 0 || fromRank >= 12) {
					System.out.println("Rank must be in range A-L");
					continue;
				}
				int fromFile = readInt("Enter file (0-5): ");
				if (fromFile < 0 || fromFile >= 12) {
					System.out.println("File must be in range 0-5");
					continue;
				}
				int toRank = Character.toLowerCase(readChar("Enter target rank (A-L): ")) - 'a';
				if (toRank < 0 || toRank >= 12) {
					System.out.println("Rank must be in range A-L");
					continue;
				}
				int toFile = readInt("Enter target file (0-5): ");
				if (toFile < 0 || toFile >= 6) {
					System.out.println("File must be in range 0-5");
					continue;
				}
				error = dc.isMoveLegal(fromRank, fromFile, toRank, toFile);
				if (error == null) {
					return new DCMove(dc.getBoard()[fromRank][fromFile], fromRank, fromFile, toRank, toFile);
				}
			}
			System.out.println(error);
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

	private static DylaneanChess newMidGame() {
		int[][] board = new int[12][6];
		board[0][3] = -4;
		board[4][3] = -3;
		board[4][4] = -3;
		board[3][3] = -3;
		board[3][4] = -2;
		board[4][0] = -2;
		board[4][2] = -1;
		board[4][5] = -1;

		board[11][4] = 4;
		board[7][3] = 3;
		board[7][5] = 3;
		board[9][5] = 3;
		board[7][4] = 2;
		board[9][4] = 2;
		board[8][5] = 2;
		board[8][4] = 2;
		board[9][3] =1;
		board[9][2] =1;
		return new DylaneanChess(board, 0);
	}
}
