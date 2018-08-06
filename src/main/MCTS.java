package main;

import main.support.HeuristicFunction;
import main.support.PlayoutSelection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

// TODO: Detect end of game, change choice to best score differential
public class MCTS {
	private static final Comparator<Node> NODE_PRINT_COMPARATOR = (o1, o2) -> o1.games == o2.games
			? Double.compare(o1.score[o1.parent.player], o2.score[o2.parent.player])
			: Double.compare(o1.games, o2.games);
	private Random random;
	private boolean rootParallelisation;
	private double explorationConstant = Math.sqrt(2.0);
	private double pessimisticBias = 0.0;
	private double optimisticBias = 0.0;
	private double heuristicWeight = 1.0;
	private boolean scoreBounds;
	private boolean trackTime; // display thinking time used
	private FinalSelectionPolicy finalSelectionPolicy = FinalSelectionPolicy.robustChild;
	private HeuristicFunction heuristic;
	private PlayoutSelection playoutPolicy;
	private int threads;
	private ExecutorService threadpool;
	private ArrayList<FutureTask<Node>> futures;
	private PrintWriter writer;

	public MCTS() {
		random = new Random();
		try {
			writer = new PrintWriter(new File("nodes"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
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
	 * @param bounds        enable or disable score bounds.  @return best move found
	 */
	public Move runMCTS_UCT(Board startingBoard, int runs, long maxTime, boolean bounds) {
		scoreBounds = bounds;
		Node rootNode = new Node(startingBoard);
		boolean pMode = rootParallelisation;
		Move bestMoveFound = null;

		long startTime = System.currentTimeMillis();
		if (this.trackTime) {
			System.out.println("Making choice for player: " + rootNode.player);
		}

		// No need to make multiple runs for moves that will be selected randomly
		int runs1 = startingBoard.getCurrentPlayer() < 0 ? 1 : runs;
		if (!pMode) {
			int i = 1;
			select(startingBoard.duplicate(), rootNode);
			if (rootNode.children.size() > 1) {
				while (rootNode.endScore == null &&
						(runs1 > 0 && i < runs1 || runs1 == 0 &&
								System.currentTimeMillis() - startTime < maxTime)) {
					select(startingBoard.duplicate(), rootNode);
					i++;
				}
			}
			if (rootNode.endScore != null) {
				System.out.println("Perfect play results in scores " + Arrays.toString(rootNode.endScore));
			}
			System.out.println("" + i + " trials run.");
			Node bestNodeFound = robustChild(rootNode);
			bestMoveFound = bestNodeFound.move;
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
		} else {

			for (int i = 0; i < threads; i++)
				futures.add((FutureTask<Node>) threadpool.submit(new MCTSTask(startingBoard, runs1)));

			try {
				ArrayList<Node> rootNodes = new ArrayList<>();

				// Collect all computed root nodes
				for (FutureTask<Node> f : futures)
					rootNodes.add(f.get());

				ArrayList<Move> moves = new ArrayList<>();

				for (Node n : rootNodes) {
					Node c = robustChild(n); // Select robust child
					moves.add(c.move);
				}

				bestMoveFound = vote(moves);

			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

			futures.clear();
		}

		long endTime = System.currentTimeMillis();

		if (this.trackTime) {
			System.out.println("Selected move: " + bestMoveFound);
			System.out.println("Thinking time in milliseconds: " + (endTime - startTime));
		}

		return bestMoveFound;
	}

	private String getChildrenPrintString(ArrayList<Node> children) {
		List<Node> nodes = children == null ? Collections.emptyList() : new ArrayList<>(children);
		nodes.sort(NODE_PRINT_COMPARATOR.reversed());
		return nodes.toString().replace(", AzulPlayerMove", ",\nAzulPlayerMove");
	}

	private Move vote(ArrayList<Move> moves) {
		Collections.sort(moves);
		ArrayList<Integer> counts = new ArrayList<>();
		ArrayList<Move> cMoves = new ArrayList<>();

		Move oMove = moves.get(0);
		int count = 0;
		for (Move m : moves) {
			if (oMove.compareTo(m) == 0) {
				count++;
			} else {
				cMoves.add(oMove);
				counts.add(count);
				oMove = m;
				count = 1;
			}
		}

		int mostVotes = 0;
		ArrayList<Move> mostVotedMove = new ArrayList<>();
		for (int i = 0; i < counts.size(); i++) {
			if (mostVotes < counts.get(i)) {
				mostVotes = counts.get(i);
				mostVotedMove.clear();
				mostVotedMove.add(cMoves.get(i));
			} else if (mostVotes == counts.get(i)) {
				mostVotedMove.add(cMoves.get(i));
			}
		}

		return mostVotedMove.get(random.nextInt(mostVotedMove.size()));
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
		if (scoreBounds) {
			n.backPropagateBounds(score);
		}
	}

	private BoardNodePair treePolicy(Board b, Node node) {
		boolean atLeaf = false;
		while (!b.gameOver() && !atLeaf && node.endScore == null) {
			atLeaf = node.children == null;
			if (atLeaf) {
				node.expandNode(b);
			}

			if (node.player >= 0) { // this is a regular node
				ArrayList<Node> bestNodes = findChildren(node, b, optimisticBias, pessimisticBias);
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

	/**
	 * This is the final step of the algorithm, to pick the best move to
	 * actually make.
	 *
	 * @param n this is the node whose children are considered
	 * @return the best Move the algorithm can find
	 */
	private Move finalMoveSelection(Node n) {
		Node r;

		switch (finalSelectionPolicy) {
			case maxChild:
				r = maxChild(n);
				break;
			case robustChild:
				r = robustChild(n);
				break;
			default:
				r = robustChild(n);
				break;
		}

		return r.move;
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

	/**
	 * Select the child node with the highest score
	 */
	private Node maxChild(Node n) {
		double bestValue = Double.NEGATIVE_INFINITY;
		double tempBest;
		ArrayList<Node> bestNodes = new ArrayList<>();

		for (Node s : n.children) {
			tempBest = s.score[n.player];
			tempBest += s.opti[n.player] * optimisticBias;
			tempBest += s.pess[n.player] * pessimisticBias;
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
            if (playoutPolicy == null) {
                moves = brd.getMoves(CallLocation.treePolicy);
	            if (brd.getCurrentPlayer() >= 0) {
                    // make random selection normally
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
            else {
                playoutPolicy.Process(board);
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
	private ArrayList<Node> findChildren(Node n, Board b, double optimisticBias, double pessimisticBias) {
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
					double tempBest = s.upperConfidenceBound(explorationConstant) + optimisticBias * s.opti[n.player] + pessimisticBias * s.pess[n.player];
					if (heuristic != null) {
						tempBest += heuristic.h(b, s.move) * heuristicWeight;
					}

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

	public void setHeuristicWeight(double heuristicWeight) {
		this.heuristicWeight = heuristicWeight;
	}

	public void setMoveSelectionPolicy(FinalSelectionPolicy policy) {
		finalSelectionPolicy = policy;
	}

	public void setHeuristicFunction(HeuristicFunction h) {
		heuristic = h;
	}

	public void setPlayoutSelection(PlayoutSelection p) {
		playoutPolicy = p;
	}

	/**
	 * This is multiplied by the pessimistic bounds of any considered move
	 * during selection.
	 */
	public void setPessimisticBias(double b) {
		pessimisticBias = b;
	}

	/**
	 * This is multiplied by the optimistic bounds of any considered move during
	 * selection.
	 */
	public void setOptimisticBias(double b) {
		optimisticBias = b;
	}

	public void setTimeDisplay(boolean displayTime) {
		this.trackTime = displayTime;
	}

	/**
	 * Switch on multi threading. The argument indicates
	 * how many threads you want in the thread pool.
	 */
	public void enableRootParallelisation(int threads) {
		rootParallelisation = true;
		this.threads = threads;

		threadpool = Executors.newFixedThreadPool(threads);
		futures = new ArrayList<>();
	}
	// Check if all threads are done

	/*
	 * This is a task for the thread pool.
	 */
	private class MCTSTask implements Callable<Node> {
		private int iterations;
		private Board board;

		MCTSTask(Board board, int iterations) {
			this.iterations = iterations;
			this.board = board;
		}

		@Override
		public Node call() throws Exception {
			Node root = new Node(board);

			for (int i = 0; i < iterations; i++) {
				select(board.duplicate(), root);
			}

			return root;
		}

	}

}
