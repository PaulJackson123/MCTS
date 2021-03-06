package main;

import main.support.HeuristicFunction;

import javax.management.NotificationEmitter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.management.*;
import java.util.*;

public class MCTS {
	private static final Comparator<Node> NODE_PRINT_COMPARATOR = (o1, o2) -> o1.games == o2.games
			? Double.compare(o1.score[o1.parent.player], o2.score[o2.parent.player])
			: Double.compare(o1.games, o2.games);
	public static final int GAMES_SIMULATED_BY_HEURISTIC = 30;
	private final Random random;
	private double explorationConstant = Math.sqrt(2.0);
	private boolean trackTime; // display thinking time used
	private HeuristicFunction heuristic;
	private PrintWriter writer;
	private volatile boolean requestCompletion = false;
	private volatile boolean lowMemory = false;
	private volatile int runs;

	public MCTS() {
		random = new Random();
		try {
			writer = new PrintWriter(new File("nodes"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		initLowMemoryDetection();
	}

	private void initLowMemoryDetection() {
		// heuristic to find the tenured pool (largest heap) as seen on http://www.javaspecialists.eu/archive/Issue092.html
		MemoryPoolMXBean tenuredGenPool = null;
		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			if (pool.getType() == MemoryType.HEAP && pool.isUsageThresholdSupported()) {
				tenuredGenPool = pool;
			}
		}
		// we do something when we reached 90% of memory usage
		tenuredGenPool.setCollectionUsageThreshold((int) Math.floor(tenuredGenPool.getUsage().getMax() * 0.50));

		//set a listener
		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		NotificationEmitter emitter = (NotificationEmitter) mbean;
		emitter.addNotificationListener((n, hb) -> {
			if (n.getType().equals(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {
				// this is the signal => end the application early to avoid OOME
				setLowMemory(true);
			}
		}, null, null);
	}

	public Move selectRandom(Board startingBoard) {
		Node rootNode = new Node(startingBoard);
		if (rootNode.children == null) {
			rootNode.expandNode(startingBoard);
		}
		Node node = rootNode.children.get(random.nextInt(rootNode.children.size()));
		return node.move;
	}

	/**
	 * Run a UCT-MCTS simulation for a number of iterations.
	 *
	 * @param startingBoard starting board
	 * @param runs          how many iterations to think. Ignored if 0.
	 * @param maxTime       time (in ms) to spend thinking. Ignored unless runs == 0
	 */
	public Move runMCTS_UCT(Board startingBoard, int runs, long maxTime) {
		Node rootNode = new Node(startingBoard);
		return runMctsAndGetBestNode(startingBoard, runs, maxTime, rootNode);
	}

	public Move runMctsAndGetBestNode(Board startingBoard, int runs, long maxTime, Node rootNode) {
		long startTime = System.currentTimeMillis();
		if (this.trackTime) {
			System.out.println("Making choice for player: " + startingBoard.getCurrentPlayer());
		}

		runMCTS(startingBoard, runs, maxTime, rootNode);
		if (rootNode.endScore != null) {
			System.out.println("Perfect play results in scores " + Arrays.toString(rootNode.endScore));
		}
		Node bestNodeFound = rootNode.endScore == null ? robustChild(rootNode) : unprunedChild(rootNode);
		Move bestMoveFound = bestNodeFound.move;

		writer.print("Player " + startingBoard.getCurrentPlayer() + " chooses " + bestMoveFound);
		Node tempBest = bestNodeFound;
		while (tempBest.children != null && !tempBest.children.isEmpty()) {
			tempBest = robustChild(tempBest);
			writer.println("=>");
			writer.print("\tPlayer " + tempBest.parent.player + " chooses " + tempBest.move);
		}
		writer.println();
		writer.println("Choices\n" + getChildrenPrintString(rootNode.children));
		writer.println("Children\n" + getChildrenPrintString(bestNodeFound.children));
		writer.println();
		writer.flush();

		long endTime = System.currentTimeMillis();

		if (this.trackTime) {
			System.out.println("Selected move: " + bestMoveFound);
			System.out.println("" + this.runs + " runs in " + (endTime - startTime) + " ms.");
		}

		return bestMoveFound;
	}

	public Node runMCTS(Board startingBoard, int runs, long maxTime, Node rootNode) {
		try {
			long startTime1 = System.currentTimeMillis();
			// No need to make multiple runs for moves that will be selected randomly
			int maxRuns = startingBoard.getCurrentPlayer() < 0 ? 1 : runs;

			int i = 1;
			select(startingBoard.duplicate(), rootNode);
			// TODO: Do not skip this if this is bg thread
			if (rootNode.children.size() > 1) {
				while (shouldContinue(rootNode, maxRuns, maxTime, startTime1, i)) {
					select(startingBoard.duplicate(), rootNode);
					i++;
				}
			}
			this.runs = i;
			return rootNode;
		}
		catch (Error e) {
			System.out.println("Search process failed: " + e.getMessage());
			throw e;
		}
	}

	private boolean shouldContinue(Node rootNode, int maxRuns, long maxTime, long startTime, int runs) {
		if (rootNode.endScore != null) {
			return false;
		}
		else if (maxRuns > 0 ) {
			return runs < maxRuns;
		}
		else if (maxTime > 0 && getTimeSpent(startTime) > maxTime) {
			return false;
		}
		else if (lowMemory) {
			System.out.println("Halting search after " + runs +
					" runs and " + getTimeSpent(startTime) +
					" ms to preserve free memory");
			return false;
		}
		return !requestCompletion;
	}

	private long getTimeSpent(long startTime) {
		return System.currentTimeMillis() - startTime;
	}

	private String getChildrenPrintString(ArrayList<Node> children) {
		List<Node> nodes = children == null ? Collections.emptyList() : new ArrayList<>(children);
		nodes.sort(NODE_PRINT_COMPARATOR.reversed());
		return nodes.toString().replace(", AzulPlayerMove", ",\nAzulPlayerMove");
	}

	/**
	 * This represents the select stage, or default policy, of the algorithm.
	 * Traverse down to the bottom of the tree using the selection strategy
	 * until you find an unexpanded child node. Expand it. Run a random playout.
	 * Back propagate results of the playout.
	 *
	 * @param currentBoard Board state to work from.
	 * @param currentNode  Node from which to start selection
	 */
	private void select(Board currentBoard, Node currentNode) {
		// Begin tree policy. Traverse down the tree and expand. Return
		// the new node or the deepest node it could reach. Return too
		// a board matching the returned node.
		BoardNodePair data = treePolicy(currentBoard, currentNode);
		Board b = data.getBoard();
		Node n = data.getNode();

		// If playedToEnd get score from node.endScore, else, run a random playout
		double[] score = n.endScore == null ? playout(b) : n.endScore;

		// Back propagate results of playout.
		n.backPropagateScore(score, true);
	}

	private BoardNodePair treePolicy(Board b, Node node) {
		boolean atLeaf = false;
		while (!b.gameOver() && !atLeaf && node.endScore == null) {
			atLeaf = node.children == null;
			if (atLeaf) {
				node.expandNode(b);
				if (heuristic != null) {
					applyHeuristic(b, node);
				}
			}

			if (node.player >= 0) { // this is a regular node
				ArrayList<Node> bestNodes = findChildren(node, b);
				if (bestNodes.size() == 0) {
					// We have failed to find a single child to visit
					// from a non-terminal node. Maybe all nodes have been pruned,
					// or all nodes returned NaN for score, so we return a
					// random node.
					bestNodes = node.children;
				}
				node = bestNodes.get(random.nextInt(bestNodes.size()));
				b.makeMove(node.move);
			} else { // this is a random node

				// The tree policy for random nodes is different. We
				// ignore selection heuristics and pick one node at
				// random based on the weight vector.

				node = node.children.get(node.randomSelect(b));
				b.makeMove(node.move);
			}
		}

		if (b.gameOver()) {
			node.endScore = b.getScore();
		}

		return new BoardNodePair(b, node);
	}

	private void applyHeuristic(Board b, Node node) {
		int currentPlayer = b.getCurrentPlayer();
		if (currentPlayer < 0) {
			return;
		}
		int players = b.getQuantityOfPlayers();
		double meanWinRate = 1.0d / players;
		double meanLossRate = 1.0d - meanWinRate;
		for (Node child : node.children) {
			double h = heuristic.h(b, child.move);
			double boost = h > 0
					? (meanWinRate + h * meanLossRate) * GAMES_SIMULATED_BY_HEURISTIC
					: (meanWinRate + h * meanWinRate) * GAMES_SIMULATED_BY_HEURISTIC;
			double remainder = ((double) GAMES_SIMULATED_BY_HEURISTIC - boost) / (players - 1);
			for (int i = 0; i < child.score.length; i++) {
				child.score[i] = i == currentPlayer ? boost : remainder;
				child.games = GAMES_SIMULATED_BY_HEURISTIC;
			}
		}
	}

	/**
	 * Select the most visited child node
	 */
	private Node robustChild(Node n) {
		double bestValue = Double.NEGATIVE_INFINITY;
		double tempBest;
		ArrayList<Node> bestNodes = new ArrayList<>();

		for (Node s : n.children) {
			tempBest = s.games;
			if (tempBest > bestValue) {
				bestNodes.clear();
				bestNodes.add(s);
				bestValue = tempBest;
			} else if (tempBest == bestValue) {
				bestNodes.add(s);
			}
		}

		return bestNodes.get(random.nextInt(bestNodes.size()));
	}

	private Node unprunedChild(Node n) {
		for (Node s : n.children) {
			if (!s.pruned) {
				return s;
			}
		}
		throw new IllegalStateException("Cannot find unpruned node for perfect play.");
	}

	/**
	 * Playout function for MCTS
	 */
	private double[] playout(Board board) {
		List<Move> moves;
		if (board.gameOver()) {
			return board.getScore();
		}

		Board brd = board.duplicate();
		// Start playing random moves until the game is over
		do { // TODO: Alpha-go uses policy net to choose weighted
			moves = brd.getMoves();
			if (brd.getCurrentPlayer() >= 0) {
		        // make random selection normally
				if (moves.size() == 0) {
					throw new IllegalStateException("No legal moves for player " + brd.getCurrentPlayer());
				}
				brd.makeMove(moves.get(random.nextInt(moves.size())));
		    }
		    else {
				// This situation only occurs when a move
		        // is entirely random, for example a die
		        // roll. We must consider the random weights
		        // of the moves.

				brd.makeMove(getRandomMove(brd, moves));
		    }
		}
        while (!brd.gameOver());

		return brd.getScore();
	}

	private Move getRandomMove(Board board, List<Move> moves) {
		double[] weights = board.getMoveWeights();

		double totalWeight = 0.0d;
		for (double weight : weights) {
			totalWeight += weight;
		}

		int randomIndex = -1;
		double random = Math.random() * totalWeight;
		for (int i = 0; i < weights.length; ++i) {
			random -= weights[i];
			if (random <= 0.0d) {
				randomIndex = i;
				break;
			}
		}

		return moves.get(randomIndex);
	}

	/**
	 * Produce a list of viable nodes to visit. The actual selection is done in runMCTS
	 */
	private ArrayList<Node> findChildren(Node n, Board b) {
		double bestValue = Double.NEGATIVE_INFINITY;
		ArrayList<Node> bestNodes = new ArrayList<>();
		boolean foundNotAtEnd = false;
		for (Node s : n.children) {
			if (!s.pruned) {
				// Only consider nodes searched to end if no other nodes have been searched to the end
				if (!foundNotAtEnd || s.endScore == null) {
					if (!foundNotAtEnd && s.endScore == null) {
						// Reset search now that we've found a node that was NOT searched to the end
						foundNotAtEnd = true;
						bestValue = Double.NEGATIVE_INFINITY;
//						bestNodes.clear(); // This is unnecessary given clear below
					}
					double tempBest = s.upperConfidenceBound(explorationConstant);

					if (tempBest > bestValue) {
						// If we found a better node
						bestNodes.clear();
						bestNodes.add(s);
						bestValue = tempBest;
					} else if (tempBest == bestValue) {
						// If we found an equal node
						bestNodes.add(s);
					}
				}
			}
		}

		return bestNodes;
	}

	/**
	 * Sets the exploration constant for the algorithm. You will need to find
	 * the optimal value through testing. This can have a big impact on
	 * performance. Default value is sqrt(2)
	 */
	public void setExplorationConstant(double exp) {
		explorationConstant = exp;
	}

	public void setHeuristicFunction(HeuristicFunction h) {
		heuristic = h;
	}

	public void setTimeDisplay(boolean displayTime) {
		this.trackTime = displayTime;
	}

	public void setRequestCompletion(boolean value) {
		this.requestCompletion = value;
	}

	public void setLowMemory(boolean value) {
		this.lowMemory = value;
	}
}
