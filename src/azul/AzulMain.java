package azul;

import main.FinalSelectionPolicy;
import main.MCTS;
import main.Move;
import main.Utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.List;

class AzulMain {

	private static final int RUNS = 0;
	private static final long MAX_TIME = 60_000L;
	private static boolean manuallySetFactories = true;
	private static boolean[] humanPlayer = {true, false, true};

	public static void main(String[] args) {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(0.36);
		mcts.setHeuristicWeight(1.0);
		mcts.setTimeDisplay(true);
		Move move;
		mcts.setOptimisticBias(0.0d);
		mcts.setPessimisticBias(0.0d);
		mcts.setHeuristicFunction(new AzulHeuristicFunction(0.36));
		mcts.setMoveSelectionPolicy(FinalSelectionPolicy.robustChild);
		int[] scores = new int[3];

		for (int i = 0; i < 100; i++) {
			Azul azul = new Azul(humanPlayer.length, false);
			while (!azul.gameOver()) {
				if (azul.getCurrentPlayer() < 0) {
					move = manuallySetFactories ? setFactories(azul) : mcts.selectRandom(azul);
				} else if (humanPlayer[azul.getCurrentPlayer()]) {
					move = getHumanMove(azul);
				} else {
					azul.bPrint();
					move = mcts.runMCTS_UCT(azul, RUNS, MAX_TIME, false);
				}
				azul.makeMove(move);
			}

			System.out.println("---");
			azul.bPrint();

			double[] scr = azul.getScore();
			if (scr[0] > 0.9) {
				scores[0]++; // player 1
			} else if (scr[1] > 0.9) {
				scores[1]++; // player 2
			} else {
				scores[2]++; // draw
			}

			System.out.println(Arrays.toString(azul.getPoints()));
			System.out.println(Arrays.toString(scr));
			System.out.println(Arrays.toString(scores));
		}
	}

	private static AzulSetupMove setFactories(Azul azul) {
		int numFactories = Azul.factoriesPerPlayer[humanPlayer.length];
		int[] selections = new int[numFactories * 4];
		int selectionCount = 0;
		List<Integer> tileBag = azul.getTileBag();
		List<Integer> tileBox = azul.getTileBox();
		for (int f = 0; f < numFactories; f++) {
			for (int i = 0; i < 4; i++) {
				int tile = getSelection(tileBag, tileBox, f, i);
				selections[selectionCount++] = tile;
			}
		}
		int nextPlayer;
		if (azul.getPlayFirstTile() == -1) {
			nextPlayer = readInt("Who goes first (0-" + (humanPlayer.length - 1) + "): ");
		}
		else {
			nextPlayer = azul.getPlayFirstTile();
		}
		return new AzulSetupMove(nextPlayer, selections);
	}

	private static int getSelection(List<Integer> tileBag, List<Integer> tileBox, int factory, int i) {
		while (true) {
			try {
				int color = readInt("Enter tile #" + (i + 1) + " color for factory " + (factory + 1) + " (1-blue 2-yellow 3-red 4-black 5-teal): ");
				return getTileNumber(tileBag, tileBox, color);
			}
			catch (RuntimeException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	static int getTileNumber(List<Integer> tileBag, List<Integer> tileBox, int color) {
		List<Integer> tiles = tileBag.isEmpty() ? tileBox : tileBag;
		for (int i = 0; i < tiles.size(); i++) {
			if (tiles.get(i) == color) {
				Utils.swapEndAndRemove(tiles, i);
				return i;
			}
		}
		throw new IllegalArgumentException("Tile bag does not contain tile " + color);
	}

	private static Move getHumanMove(final Azul azul) {
		do {
			azul.bPrint();
			System.out.println("Player " + azul.getCurrentPlayer() + "'s turn.");
			int factory = readInt("Enter factory (0-" + azul.getFactoryCount() + "): ");
			int color = readInt("Enter color (1-blue 2-yellow 3-red 4-black 5-teal): ");
			int line = readInt("Enter line (0 for floor, or 1-5): ");
			String error = azul.isMoveLegal(factory, color, line);
			if (error == null) {
				return new AzulPlayerMove(factory, color, line, azul.getTilesCount(factory, color));
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
}
