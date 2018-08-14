package azul;

import main.Move;

class AzulSetupMove implements Move {
	private final byte nextPlayer;

	private final byte[] factorySelections;

	AzulSetupMove(int nextPlayer, byte[] factorySelections) {
		this.nextPlayer = (byte) nextPlayer;
		this.factorySelections = factorySelections;
	}

	AzulSetupMove(byte nextPlayer, byte[] factorySelections) {
		this.nextPlayer = nextPlayer;
		this.factorySelections = factorySelections;
	}

	byte getNextPlayer() {
		return nextPlayer;
	}

	byte[] getFactorySelections() {
		return factorySelections;
	}

	@Override
	public int compareTo(Move o) {
		return 0;
	}
}
