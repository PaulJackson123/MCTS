package azul;

import main.Move;

class AzulSetupMove implements Move {
	private final int nextPlayer;

	private final int[] factorySelections;

	public AzulSetupMove(final int nextPlayer, final int[] factorySelections) {
		this.nextPlayer = nextPlayer;
		this.factorySelections = factorySelections;
	}

	public int getNextPlayer() {
		return nextPlayer;
	}

	public int[] getFactorySelections() {
		return factorySelections;
	}

	@Override
	public int compareTo(final Move o) {
		return 0;
	}
}
