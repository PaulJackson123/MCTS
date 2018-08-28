package dylanean;

import main.Move;

class DCMove implements Move {
	private final int piece;
	private final int fromRank;
	private final int fromFile;
	private final int toRank;
	private final int toFile;

	DCMove(int piece, int fromRank, int fromFile, int toRank, int toFile) {
		this.piece = piece;
		this.fromRank = fromRank;
		this.fromFile = fromFile;
		this.toRank = toRank;
		this.toFile = toFile;
	}

	public int getPiece() {
		return piece;
	}

	public int getFromRank() {
		return fromRank;
	}

	public int getFromFile() {
		return fromFile;
	}

	public int getToRank() {
		return toRank;
	}

	public int getToFile() {
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

		if (piece != dcMove.piece) {
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
		int result = piece;
		result = 31 * result + fromRank;
		result = 31 * result + fromFile;
		result = 31 * result + toRank;
		result = 31 * result + toFile;
		return result;
	}

	@Override
	public String toString() {
		return "DCMove{" +
				"piece=" + piece +
				", fromRank=" + fromRank +
				", fromFile=" + fromFile +
				", toRank=" + toRank +
				", toFile=" + toFile +
				'}';
	}
}
