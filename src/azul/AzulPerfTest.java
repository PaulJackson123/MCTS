package azul;

import main.MCTS;

import java.util.List;

// byte storage
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=7, color=2, line=2, count=2}
//	Thinking time in milliseconds: 16485
//	Time = 16486
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=5, color=5, line=2, count=2}
//	Thinking time in milliseconds: 15506
//	Time = 15506
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=5, color=5, line=2, count=2}
//	Thinking time in milliseconds: 15407
//	Time = 15407
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=5, color=5, line=2, count=2}
//	Thinking time in milliseconds: 15327
//	Time = 15327
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=7, color=2, line=2, count=2}
//	Thinking time in milliseconds: 15476
//	Time = 15476
public class AzulPerfTest {

	private static final int BAG_SIZE = 7 * 4;

	public static void main(String[] args) {
		Azul azul = new Azul(3, false);
		byte[] colors = new byte[] {5, 5, 3, 1, 5, 5, 3, 1, 5, 1, 4, 2, 3, 3, 1, 2, 2, 2, 5, 5, 5, 5, 4, 1, 1, 1, 2, 2};
		byte[] selections = getSelections(azul, colors);
		azul.makeMove(new AzulSetupMove(0, selections));
		MCTS mcts = AzulMain.newMcts(0.36);
		runIteration(azul, mcts);
		runIteration(azul, mcts);
		runIteration(azul, mcts);
		runIteration(azul, mcts);
		runIteration(azul, mcts);
	}

	private static void runIteration(Azul azul, MCTS mcts) {
		long startTime = System.currentTimeMillis();
		mcts.runMCTS_UCT(azul.duplicate(), 20_000, 0);
		System.out.println("Time = " + (System.currentTimeMillis() - startTime));
	}

	private static byte[] getSelections(Azul azul, byte[] colors) {
		byte[] selections = new byte[BAG_SIZE];
		List<Byte> tileBag = azul.getTileBag();
		List<Byte> tileBox = azul.getTileBox();
		for (int i = 0; i < BAG_SIZE; i++) {
			selections[i] = AzulMain.getTileNumber(tileBag, tileBox, colors[i]);
		}
		return selections;
	}
}
