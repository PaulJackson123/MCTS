package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Node implements Comparable<Node> {
	public double[] score;
	public double games;
	public Move move;
	public ArrayList<Node> unvisitedChildren;
	public ArrayList<Node> children;
	public Node parent;
	public int player;
	public double[] pess;
	public double[] opti;
	public boolean pruned;

	/**
	 * This is a special Node constructor that merges
	 * multiple root nodes into a single main node.
	 */
	public Node(ArrayList<Node> rootNodes) {
		LinkedList<Node> childNodes = new LinkedList<>();

		for (Node n : rootNodes) {
			childNodes.addAll(n.children);
		}

		Collections.sort(childNodes);
		children = new ArrayList<>();

		while (!childNodes.isEmpty()) {
			LinkedList<Node> tempNodes = new LinkedList<>();
			Node curNode = childNodes.get(0);
			childNodes.remove(0);

			while (!childNodes.isEmpty() && childNodes.get(0).compareTo(curNode) == 0) {
				tempNodes.add(childNodes.get(0));
				childNodes.remove(0);
			}

			children.add(new Node(tempNodes));
		}

	}

	/**
	 * This is a Node constructor that constructs a
	 * new node by combining the stats for all nodes
	 * passed into it.
	 */
	private Node(LinkedList<Node> nodes) {
		move = nodes.get(0).move;
		score = new double[nodes.get(0).score.length];
		for (Node n : nodes) {
			games += n.games;
			for (int i = 0; i < score.length; i++)
				score[i] += n.score[i];
		}
	}

	/**
	 * This creates the root node
	 */
	public Node(Board b) {
		children = new ArrayList<>();
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
		children = new ArrayList<>();
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
	public double upperConfidenceBound(double c) {
		return score[parent.player] / games + c * Math.sqrt(Math.log(parent.games + 1) / games);
	}

	/**
	 * Update the tree with the new score.
	 */
	public void backPropagateScore(double[] scr) {
		this.games++;
		for (int i = 0; i < scr.length; i++)
			this.score[i] += scr[i];

		if (parent != null)
			parent.backPropagateScore(scr);
	}

	/**
	 * Expand this node by populating its list of unvisited child nodes.
	 */
	public void expandNode(Board currentBoard) {
		List<Move> legalMoves = currentBoard.getMoves(CallLocation.treePolicy);
		unvisitedChildren = new ArrayList<>();
		for (Move legalMove : legalMoves) {
			Node tempState = new Node(currentBoard, legalMove, this);
			unvisitedChildren.add(tempState);
		}
	}

	/**
	 * Set the bounds in the given node and propagate the values back up the
	 * tree.
	 */
	public void backPropagateBounds(double[] score) {
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
		if (!unvisitedChildren.isEmpty()) {
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
	public int randomSelect(Board board) {
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
		String[] scoreStrings = new String[score.length];
		for (int i = 0; i < score.length; i++) {
			scoreStrings[i] = String.valueOf(score[i]);
		}
		if (parent == null) {
			return "ROOT";
		} else if (parent.player < 0) {
			return "" + move + " " + String.join(", ", scoreStrings);
		} else {
			return "" + move + " " + String.join(", ", scoreStrings) + " " + parent.player + " wins " + (int) (score[parent.player] * 100 / games) + "% of " + games;
		}
	}
}