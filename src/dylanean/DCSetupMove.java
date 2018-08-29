package dylanean;

import main.Move;

class DCSetupMove implements Move {
	private final int piece;
	private final int rank;
	private final int file;

	DCSetupMove(int piece, int rank, int file) {
		this.piece = piece;
		this.rank = rank;
		this.file = file;
	}

	int getPiece() {
		return piece;
	}

	int getRank() {
		return rank;
	}

	int getFile() {
		return file;
	}

	@Override
	public int compareTo(final Move o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DCSetupMove that = (DCSetupMove) o;

		return piece == that.piece && rank == that.rank && file == that.file;
	}

	@Override
	public int hashCode() {
		int result = piece;
		result = 31 * result + rank;
		result = 31 * result + file;
		return result;
	}

	@Override
	public String toString() {
		return "SetupMove{" +
				"piece=" + piece + "/" + DylaneanChess.toChars(piece) +
				", rank=" + rank + "/" + (char) ('a' + rank) +
				", file=" + file +
				'}';
	}
}
