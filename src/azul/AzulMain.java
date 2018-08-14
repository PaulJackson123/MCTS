package azul;

import main.MCTS;
import main.Move;
import main.Node;
import main.Utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class AzulMain {

	private static final int MAX_RUNS = 0;
	private static final long MAX_TIME = 20_000L;
	private static final boolean MANUALLY_SET_FACTORIES = false;
	private static boolean[] humanPlayer = {true, false};
	private static ExecutorService executorService = Executors.newFixedThreadPool(1);

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		Map<Integer, MCTS> mctses = new HashMap<>();
		for (int i = 0; i < humanPlayer.length; i++) {
			if (!humanPlayer[i]) {
				mctses.put(i, newMcts());
			}
		}
		int[] scores = new int[3];

		Azul azul = new Azul(humanPlayer.length, false);
		MCTS mcts = newMcts();
		MCTS bg = newMcts();
		Node bgNode = null;
		while (!azul.gameOver()) {
			Future<Node> future = null;
			Move move;
			if (azul.getCurrentPlayer() < 0) {
				//noinspection ConstantConditions
				move = MANUALLY_SET_FACTORIES ? setFactories(azul) : mcts.selectRandom(azul);
			} else {
				if (humanPlayer[azul.getCurrentPlayer()]) {
					// Think while human is making move
					Azul bgBoard = azul.duplicate();
					bg.setRequestCompletions(false);
					future = executorService.submit(() -> bg.runMCTS(bgBoard, 0, 0, new Node(bgBoard)));
					move = getHumanMove(azul);
				}
				else {
					azul.bPrint();
					Node rootNode = bgNode == null ? new Node(azul) : bgNode;
					move = mcts.runMctsAndGetBestNode(azul, MAX_RUNS, MAX_TIME, rootNode);
					System.out.println("" + rootNode.games + " trials run.");
				}
			}
			azul.makeMove(move);
			if (future != null) {
				bg.setRequestCompletions(true);
				// Retain portion of tree that still applies
				Node oldRootNode = future.get();
				Node child = oldRootNode.makeRootNode(move);
				if (child == null) {
					bgNode = new Node(azul);
				}
				else {
					System.out.println("Branch searched " + child.games + " out of " + oldRootNode.games + " times.");
					bgNode = child;
				}
			}
			else {
				bgNode = null;
			}
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

	static MCTS newMcts() {
		MCTS mcts = new MCTS();
		mcts.setExplorationConstant(0.36);
		mcts.setHeuristicWeight(1.0);
		mcts.setTimeDisplay(true);
		mcts.setHeuristicFunction(new AzulHeuristicFunction(0.36));
		return mcts;
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
