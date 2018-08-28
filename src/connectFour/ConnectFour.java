package connectFour;

import main.Board;
import main.Move;

import java.util.ArrayList;

public class ConnectFour implements Board {

	private int[][] board; // The actual game board data
	int currentPlayer = 0;
	private int[] freeSlots; // The number of free slots per column
	private int totalFreeSlots = 6 * 7;
	private int winner = -1;
	private boolean draw = false;

	public ConnectFour() {
		board = new int[7][6];
		freeSlots = new int[7];
		for (int i = 0; i < 7; i++)
			freeSlots[i] = 6;
		winner = -1;
	}

	private ConnectFour(ConnectFour newBoard) {
		for (int x = 0; x < 7; x++) {
			System.arraycopy(newBoard.board[x], 0, board[x], 0, 6);
		}
		currentPlayer = newBoard.currentPlayer;
		System.arraycopy(newBoard.freeSlots, 0, freeSlots, 0, 7);
		totalFreeSlots = newBoard.totalFreeSlots;
		winner = newBoard.winner;
		draw = newBoard.draw;
	}

	@Override
	public ConnectFour duplicate() {
		return new ConnectFour(this);
	}

	@Override
	public ArrayList<Move> getMoves() {
		ArrayList<Move> moves = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			if (freeSlots[i] > 0) {
				ConnectFourMove cfm = new ConnectFourMove(i);
				moves.add(cfm);
			}
		}

		return moves;
	}

	/*
	 * Return true if last move won the game for that player
	 */
	private boolean thisMoveWonTheGame(int x, int y, int pl) {
		int horizontal = 1;
		int vertical = 1;
		int risingDiagonal = 1;
		int sinkingDiagonal = 1;

		horizontal += scanLine(x, y, -1, 0, pl);
		horizontal += scanLine(x, y, 1, 0, pl);
		vertical += scanLine(x, y, 0, 1, pl);
		risingDiagonal += scanLine(x, y, 1, -1, pl);
		risingDiagonal += scanLine(x, y, -1, 1, pl);
		sinkingDiagonal += scanLine(x, y, 1, 1, pl);
		sinkingDiagonal += scanLine(x, y, -1, -1, pl);

		return (horizontal >= 4 || vertical >= 4 ||
				risingDiagonal >= 4 || sinkingDiagonal >= 4);
	}

	/*
	 * Return the number of pieces extending from position x, y in the
	 * direction of xf, yf. Think of the latter as a direction vector.
	 */
	private int scanLine(int x, int y, int xf, int yf, int playerID) {
		int sum = 0;
		for (int i = 1; i < 4; i++) {
			if (x + i * xf > 6 || x + i * xf < 0)
				break;
			if (y + i * yf > 5 || y + i * yf < 0)
				break;

			if (board[x + i * xf][y + i * yf] == playerID + 1)
				sum++;
			else
				break;
		}

		return sum;
	}

	@Override
	public void makeMove(Move m) {
		ConnectFourMove cfm = (ConnectFourMove) m;

		int xIndex = cfm.row;
		int yIndex = freeSlots[cfm.row] - 1;

		board[xIndex][yIndex] = currentPlayer + 1;
		freeSlots[xIndex]--;
		totalFreeSlots--;

		// Check if the move won the game, if so update the winner
		if (thisMoveWonTheGame(xIndex, yIndex, currentPlayer)) {
			winner = currentPlayer;
		} else {
			if (totalFreeSlots == 0)
				draw = true;
		}

		// Switch player after every move
		if (currentPlayer == 0)
			currentPlayer = 1;
		else
			currentPlayer = 0;
	}

	@Override
	public int getQuantityOfPlayers() {
		return 2;
	}

	@Override
	public int getCurrentPlayer() {
		return currentPlayer;
	}

	public void print() {
		System.out.println("--------------");
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 7; x++) {
				if (board[x][y] == 1)
					System.out.print("()");
				else if (board[x][y] == 2)
					System.out.print("<>");
				else if (board[x][y] == 0)
					System.out.print("  ");
				else
					System.out.print("{}");
			}
			System.out.println("");
		}
	}

	@Override
	public boolean gameOver() {
		return draw || winner >= 0;
	}

	@Override
	public double[] getScore() {
		double[] score = new double[2];
		if (winner >= 0)
			score[winner] = 1.0d;
		else if (draw) {
			score[0] = 0.5d;
			score[1] = 0.5d;
		}

		return score;
	}

	/*
	 * This method is not used by this game, but at least
	 * a function body is required to fulfill the Board
	 * interface contract. 
	 */
	public double[] getMoveWeights() {
		return null;
	}

	@Override
	public void bPrint() {
	}
}
