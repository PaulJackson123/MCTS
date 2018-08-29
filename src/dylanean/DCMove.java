package dylanean;

import main.Move;

class DCMove implements Move {
	private final int pieceType;
	private final int fromRank;
	private final int fromFile;
	private final int toRank;
	private final int toFile;

	/**
	 * Create a move from one location to another.
	 *
	 * @param pieceType The positive integer representing the piece type (1=Soldier, 2=Archer,
	 *                  3=Knight, 4=King). For display only.
	 * @param fromRank
	 * @param fromFile
	 * @param toRank
	 * @param toFile
	 */
	DCMove(int pieceType, int fromRank, int fromFile, int toRank, int toFile) {
		this.pieceType = pieceType;
		this.fromRank = fromRank;
		this.fromFile = fromFile;
		this.toRank = toRank;
		this.toFile = toFile;
	}

	int getFromRank() {
		return fromRank;
	}

	int getFromFile() {
		return fromFile;
	}

	int getToRank() {
		return toRank;
	}

	int getToFile() {
		return toFile;
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

		DCMove dcMove = (DCMove) o;

		if (pieceType != dcMove.pieceType) {
			return false;
		}
		if (fromRank != dcMove.fromRank) {
			return false;
		}
		if (fromFile != dcMove.fromFile) {
			return false;
		}
		if (toRank != dcMove.toRank) {
			return false;
		}
		return toFile == dcMove.toFile;
	}

	@Override
	public int hashCode() {
		int result = pieceType;
		result = 31 * result + fromRank;
		result = 31 * result + fromFile;
		result = 31 * result + toRank;
		result = 31 * result + toFile;
		return result;
	}

	@Override
	public String toString() {
		return "Move{" +
				"pieceType=" + pieceType + "/" + DylaneanChess.toChars(pieceType) +
				", fromRank=" + fromRank + "/" + (char) ('a' + fromRank) +
				", fromFile=" + fromFile +
				", toRank=" + toRank + "/" + (char) ('a' + toRank) +
				", toFile=" + toFile +
				'}';
	}
}
