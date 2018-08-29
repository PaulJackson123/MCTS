package azul;

import main.Move;

class AzulPlayerMove implements Move {
	/**
	 * Source factory. 0 = center area, 1 - n = factory number
	 */
	private final byte factory;
	private final byte color;

	/**
	 * Target line. 0 = floor, 1 - 5 = line number
	 */
	private final byte line;

	/**
	 * Number of tiles selected - to aid toString()
	 */
	private final byte count;

	// TODO: How do we deal with tile placement during endOfRound in variant play?

	AzulPlayerMove(int factory, int color, int line, int count) {
		this.factory = (byte) factory;
		this.color = (byte) color;
		this.line = (byte) line;
		this.count = (byte) count;
	}

	AzulPlayerMove(byte factory, byte color, byte line, byte count) {
		this.factory = factory;
		this.color = color;
		this.line = line;
		this.count = count;
	}

	byte getFactory() {
		return factory;
	}

	byte getColor() {
		return color;
	}

	byte getLine() {
		return line;
	}

	@Override
	public int compareTo(final Move o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AzulPlayerMove that = (AzulPlayerMove) o;

		return factory == that.factory && color == that.color && line == that.line;
	}

	@Override
	public int hashCode() {
		int result = factory;
		result = 31 * result + color;
		result = 31 * result + line;
		return result;
	}

	@Override
	public String toString() {
		return "AzulPlayerMove{" +
				"factory=" + factory +
				", color=" + color +
				", line=" + line +
				", count=" + count +
				'}';
	}
}
