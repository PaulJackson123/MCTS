package azul;

import main.MCTS;

import java.util.List;

// Baseline run
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=5, color=2, line=2, count=2}
//	Thinking time in milliseconds: 17081
//	Time = 17081
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=7, color=2, line=2, count=2}
//	Thinking time in milliseconds: 15843
//	Time = 15843
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=4, color=1, line=1, count=1}
//	Thinking time in milliseconds: 15776
//	Time = 15776
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=1, color=5, line=2, count=2}
//	Thinking time in milliseconds: 15569
//	Time = 15570
//	Making choice for player: 0
//	Selected move: AzulPlayerMove{factory=7, color=2, line=2, count=2}
//	Thinking time in milliseconds: 15542
//	Time = 15542
public class AzulPerfTest {

	private static final int BAG_SIZE = 7 * 4;

	public static void main(String[] args) {
		Azul azul = new Azul(3, false);
		int[] colors = new int[] {5, 5, 3, 1, 5, 5, 3, 1, 5, 1, 4, 2, 3, 3, 1, 2, 2, 2, 5, 5, 5, 5, 4, 1, 1, 1, 2, 2};
		int[] selections = getSelections(azul, colors);
		azul.makeMove(new AzulSetupMove(0, selections));
		MCTS mcts = AzulMain.newMcts();
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

	private static int[] getSelections(Azul azul, int[] colors) {
		int[] selections = new int[BAG_SIZE];
		List<Integer> tileBag = azul.getTileBag();
		List<Integer> tileBox = azul.getTileBox();
		for (int i = 0; i < BAG_SIZE; i++) {
			selections[i] = AzulMain.getTileNumber(tileBag, tileBox, colors[i]);
		}
		return selections;
	}
}
