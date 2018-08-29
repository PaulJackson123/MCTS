package ticTacToe;

import main.Move;

class TicTacToeMove implements Move {
	int x;
	int y;

	TicTacToeMove(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public int compareTo(Move o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "[" + x + "," + y + "]";
	}
}
