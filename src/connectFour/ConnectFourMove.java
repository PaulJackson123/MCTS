package connectFour;

import main.Move;

/**
 * Moves are simple to manage for Connect Four. We
 * store in each move only the row of the board
 * where this piece will be inserted. The board
 * itself is responsible for implementing
 * the function that actually performs this move.
 */
class ConnectFourMove implements Move {
	int row;

	public ConnectFourMove(int row) {
		this.row = row;
	}

	public String toString() {
		return String.valueOf(row);
	}

	@Override
	public int compareTo(Move o) {
		throw new UnsupportedOperationException();
	}
}
