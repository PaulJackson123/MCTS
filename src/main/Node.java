package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Node implements Comparable<Node> {
	public double[] score;
	public double games;
	public Move move;
	public ArrayList<Node> children;
	public Node parent;
	public int player;
	public double[] pess;
	public double[] opti;
	public boolean pruned;
	public double[] endScore = null;
	/**
	 * This creates the root node
	 */
	Node(Board b) {
		player = b.getCurrentPlayer();
		score = new double[b.getQuantityOfPlayers()];
		pess = new double[b.getQuantityOfPlayers()];
		opti = new double[b.getQuantityOfPlayers()];
		for (int i = 0; i < b.getQuantityOfPlayers(); i++)
			opti[i] = 1;
	}

	/**
	 * This creates non-root nodes
	 */
	private Node(Board b, Move m, Node parent) {
		this.parent = parent;
		move = m;
		Board tempBoard = b.duplicate();
		tempBoard.makeMove(m);
		player = tempBoard.getCurrentPlayer();
		score = new double[b.getQuantityOfPlayers()];
		pess = new double[b.getQuantityOfPlayers()];
		opti = new double[b.getQuantityOfPlayers()];
		for (int i = 0; i < b.getQuantityOfPlayers(); i++)
			opti[i] = 1;
	}

	/**
	 * Return the upper confidence bound of this state
	 *
	 * @param c typically sqrt(2). Increase to emphasize exploration. Decrease
	 *          to increment exploitation
	 */
	double upperConfidenceBound(double c) {
		double score = this.score[parent.player];
		double exploitation = safeDivision(score, games);
		double exploration = c * Math.sqrt(safeDivision(Math.log(parent.games + 1), games));
		return exploitation + exploration;
	}

	private double safeDivision(double numerator, double denominator) {
		return games == 0 ? numerator == 0 ? 1.0 : Double.POSITIVE_INFINITY : numerator / denominator;
	}

	/**
	 * Update the tree with the new score.
	 */
	void backPropagateScore(double[] score, boolean prune) {
		games++;
		for (int i = 0; i < score.length; i++) {
			this.score[i] += score[i];
		}

		if (prune && children != null) {
			boolean allPlayedToEnd = true;
			Node bestChild = null;
			double bestScore = -1.0;
			for (Iterator<Node> iterator = children.iterator(); bestScore != 1.0 && iterator.hasNext(); ) {
				Node child = iterator.next();
				allPlayedToEnd = allPlayedToEnd && child.endScore != null;
				if (child.endScore != null && child.endScore[player] > bestScore) {
					bestScore = child.endScore[player];
					bestChild = child;
				}
			}
			// TODO: Can child be null here?
			if (bestScore == 1.0 || allPlayedToEnd) {
				pruneAllBut(bestChild);
				// TODO: Should scores be adjusted the rest of the way up the tree?
				for (int i = 0; i < score.length; i++) {
					this.score[i] = score[i] * games;
				}
			}
			else {
				// No point pruning at parent level if no pruning done at child
				prune = false;
			}
		}

		if (parent != null) {
			parent.backPropagateScore(score, prune);
		}
	}

	/**
	 * Call this when best play is known to be this node to prune all other
	 * nodes and adjust scores accordingly. A win is a win.
	 *
	 * @param node the child that is best play for current player
	 */
	private void pruneAllBut(Node node) {
		for (Node child : children) {
			if (child != node) {
				child.pruned = true;
			}
		}
		assert node.endScore != null; // TODO
		endScore = node.endScore;
	}

	/**
	 * Expand this node by populating its list of unvisited child nodes.
	 */
	void expandNode(Board currentBoard) {
		List<Move> legalMoves = currentBoard.getMoves(CallLocation.treePolicy);
//		unvisitedChildren = new ArrayList<>();
//		for (Move legalMove : legalMoves) {
//			Node tempState = new Node(currentBoard, legalMove, this);
//			unvisitedChildren.add(tempState);
//		}
		children = new ArrayList<>();
		for (Move legalMove : legalMoves) {
			Node tempState = new Node(currentBoard, legalMove, this);
			children.add(tempState);
		}
	}

	/**
	 * Set the bounds in the given node and propagate the values back up the
	 * tree.
	 */
	void backPropagateBounds(double[] score) {
		for (int i = 0; i < score.length; i++) {
			opti[i] = score[i];
			pess[i] = score[i];
		}

		if (parent != null)
			parent.backPropagateBoundsHelper();
	}

	private void backPropagateBoundsHelper() {
		for (int i = 0; i < opti.length; i++) {
			if (player != -1) {
				if (i == player) {
					opti[i] = Integer.MIN_VALUE;
					pess[i] = Integer.MIN_VALUE;
				} else {
					opti[i] = Integer.MAX_VALUE;
					pess[i] = Integer.MAX_VALUE;
				}
			} else {
				// This is a random/environment node
				opti[i] = Integer.MIN_VALUE;
				pess[i] = Integer.MAX_VALUE;
			}
		}

		for (int i = 0; i < opti.length; i++) {
			for (Node c : children) {
				if (player != -1) {
					if (i == player) {
						if (opti[i] < c.opti[i])
							opti[i] = c.opti[i];
						if (pess[i] < c.pess[i])
							pess[i] = c.pess[i];
					} else {
						if (opti[i] > c.opti[i])
							opti[i] = c.opti[i];
						if (pess[i] > c.pess[i])
							pess[i] = c.pess[i];
					}
				} else {
					// This is a random/environment node
					if (opti[i] < c.opti[i])
						opti[i] = c.opti[i];
					if (pess[i] > c.pess[i])
						pess[i] = c.pess[i];
				}
			}
		}

		// This compares against a dummy node with bounds 1 0
		// if not all children have been explored
		if (false) {
			for (int i = 0; i < opti.length; i++) {
				if (i == player) {
					opti[i] = 1;
				} else {
					pess[i] = 0;
				}
			}
		}

		pruneBranches();
		if (parent != null)
			parent.backPropagateBoundsHelper();
	}

	private void pruneBranches() {
		for (Node s : children) {
			if (pess[player] >= s.opti[player]) {
				s.pruned = true;
			}
		}
	}

	/**
	 * Select a child node at random and return it.
	 */
	int randomSelect(Board board) {
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

		return randomIndex;
	}

	@Override
	public int compareTo(Node o) {
		return move.compareTo(o.move);
	}

	@Override
	public String toString() {
		String result;
		String[] scoreStrings = new String[score.length];
		for (int i = 0; i < score.length; i++) {
			scoreStrings[i] = String.valueOf(score[i]);
		}
		if (parent == null) {
			result = "ROOT";
		} else if (parent.player < 0) {
			result = "" + move + " " + String.join(", ", scoreStrings);
		} else {
			result = "" + move + " " + String.join(", ", scoreStrings) + " " + parent.player + " wins " + (int) (score[parent.player] * 100 / games) + "% of " + games;
		}
		if (endScore != null) {
			result = result + " end score" + Arrays.toString(endScore);
		}
		if (pruned) {
			result = result + " PRUNED";
		}
		return result;
	}

}