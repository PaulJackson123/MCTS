package ticTacToe;

import main.Board;
import main.Move;

import java.util.ArrayList;

public class TicTacToe implements Board {
	private int[][] board;
	int currentPlayer;
	private int winner;
	private boolean draw;
	private boolean gameOver;
	private int freeSlots;

	public TicTacToe() {
		board = new int[3][3];
		freeSlots = 9;
	}

	private TicTacToe(TicTacToe t) {
		winner = t.winner;
		currentPlayer = t.currentPlayer;
		draw = t.draw;
		freeSlots = t.freeSlots;
		gameOver = t.gameOver;
		board = new int[3][3];
		for (int x = 0; x < 3; x++) {
			System.arraycopy(t.board[x], 0, board[x], 0, 3);
		}
	}

	@Override
	public boolean gameOver() {
		return gameOver;
	}

	@Override
	public TicTacToe duplicate() {
		return new TicTacToe(this);
	}

	@Override
	public ArrayList<Move> getMoves() {
		ArrayList<Move> moves = new ArrayList<>();
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				if (board[x][y] == 0)
					moves.add(new TicTacToeMove(x, y));
			}
		}

		return moves;
	}

	@Override
	public void makeMove(Move m) {
		TicTacToeMove move = (TicTacToeMove) m;
		if (board[move.x][move.y] != 0) {
			throw new IllegalArgumentException("Move " + m + " already played by player " + (board[move.x][move.y] - 1));
		}

		board[move.x][move.y] = currentPlayer + 1;
		freeSlots--;
		final int cp = currentPlayer + 1;

		if (board[0][0] == cp && board[0][1] == cp && board[0][2] == cp ||
				board[1][0] == cp && board[1][1] == cp && board[1][2] == cp ||
				board[2][0] == cp && board[2][1] == cp && board[2][2] == cp ||
				board[0][0] == cp && board[1][0] == cp && board[2][0] == cp ||
				board[0][1] == cp && board[1][1] == cp && board[2][1] == cp ||
				board[0][2] == cp && board[1][2] == cp && board[2][2] == cp ||
				board[0][0] == cp && board[1][1] == cp && board[2][2] == cp ||
				board[0][2] == cp && board[1][1] == cp && board[2][0] == cp) {
			gameOver = true;
			winner = currentPlayer;
		} else if (freeSlots == 0) {
			gameOver = true;
			draw = true;
		}

		if (currentPlayer == 0) {
			currentPlayer = 1;
		} else {
			currentPlayer = 0;
		}
	}

	@Override
	public int getCurrentPlayer() {
		return currentPlayer;
	}

	@Override
	public double[] getScore() {
		double[] score;
		score = new double[2];
		if (!draw) {
			score[winner] = 1.0d;
		} else {
			score[0] = 0.5d;
			score[1] = 0.5d;
		}

		return score;
	}

	@Override
	public int getQuantityOfPlayers() {
		return 2;
	}

	@Override
	public double[] getMoveWeights() {
		return null;
	}

	@Override
	public void bPrint() {
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				int i = board[x][y];
				System.out.print((i == 0 ? ":" : i - 1) + " ");
			}
			System.out.println();
		}
	}

	@Override
	public String toString() {
		return "" + board[0][0] + " " + board[1][0] + " " + board[2][0] + "\n" +
				board[0][1] + " " + board[1][1] + " " + board[2][1] + "\n" +
				board[0][2] + " " + board[1][2] + " " + board[2][2];
	}
}
