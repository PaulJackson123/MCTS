package azul;

import java.util.Arrays;
import java.util.List;

public class AzulManualSetupTest {

	public static final int BAG_SIZE = 7 * 4;

	public static void main(String[] args) {
		Azul azul = new Azul(3, false);
		int[] colors = new int[] {5, 5, 3, 1, 5, 5, 3, 1, 5, 1, 4, 2, 3, 3, 1, 2, 2, 2, 5, 5, 5, 5, 4, 1, 1, 1, 2, 2};
		int[] selections = getSelections(azul, colors);
		azul.makeMove(new AzulSetupMove(0, selections));
		azul.makeMove(new AzulPlayerMove(5, 5, 4, 2));
		azul.makeMove(new AzulPlayerMove(3, 2, 1, 1));
		azul.makeMove(new AzulPlayerMove(4, 1, 1, 1));
		azul.makeMove(new AzulPlayerMove(4, 2, 2, 2));
		azul.makeMove(new AzulPlayerMove(2, 5, 2, 2));
		azul.makeMove(new AzulPlayerMove(1, 5, 2, 2));
		azul.makeMove(new AzulPlayerMove(0, 1, 5, 5));
		azul.makeMove(new AzulPlayerMove(0, 5, 3, 3));
		azul.makeMove(new AzulPlayerMove(0, 4, 3, 2));
		azul.makeMove(new AzulPlayerMove(0, 2, 3, 2));
		azul.makeMove(new AzulPlayerMove(1, 3, 5, 2));
		azul.makeMove(new AzulPlayerMove(0, 2, 5, 1));
		azul.makeMove(new AzulPlayerMove(0, 1, 1, 1));
		azul.makeMove(new AzulPlayerMove(0, 3, 5, 2));
		colors = new int[] {5, 5, 2, 3, 5, 3, 1, 4, 2, 2, 1, 4, 3, 3, 5, 1, 5, 2, 4, 4, 4, 4, 1, 1, 2, 3, 1, 4};
		selections = getSelections(azul, colors);
		azul.makeMove(new AzulSetupMove(0, selections));
		azul.makeMove(new AzulPlayerMove(1, 5, 4, 2));
		azul.makeMove(new AzulPlayerMove(5, 1, 2, 2));
		azul.makeMove(new AzulPlayerMove(0, 2, 1, 1));
		azul.makeMove(new AzulPlayerMove(5, 2, 3, 1));
		azul.makeMove(new AzulPlayerMove(0, 4, 4, 3));
		azul.makeMove(new AzulPlayerMove(2, 4, 3, 1));
		azul.makeMove(new AzulPlayerMove(0, 1, 2, 2));
		azul.makeMove(new AzulPlayerMove(3, 4, 4, 2));
		azul.makeMove(new AzulPlayerMove(1, 1, 2, 1));
		azul.makeMove(new AzulPlayerMove(1, 3, 1, 2));
		azul.makeMove(new AzulPlayerMove(0, 3, 5, 3));
		azul.makeMove(new AzulPlayerMove(0, 1, 2, 1));
		azul.makeMove(new AzulPlayerMove(0, 5, 5, 3));
		azul.makeMove(new AzulPlayerMove(0, 2, 3, 3));
		azul.makeMove(new AzulPlayerMove(0, 4, 0, 1));
		colors = new int[] {3, 3, 4, 5, 4, 4, 3, 2, 3, 3, 1, 2, 3, 3, 5, 2, 3, 3, 5, 2, 2, 2, 4, 5, 4, 4, 3, 5};
		selections = getSelections(azul, colors);
		azul.makeMove(new AzulSetupMove(2, selections));
		azul.makeMove(new AzulPlayerMove(7, 3, 1, 1));
		azul.makeMove(new AzulPlayerMove(3, 3, 2, 2));
		azul.makeMove(new AzulPlayerMove(5, 2, 2, 2));
		azul.makeMove(new AzulPlayerMove(0, 5, 3, 2));
		azul.makeMove(new AzulPlayerMove(3, 3, 4, 2));
		azul.makeMove(new AzulPlayerMove(0, 4, 3, 3));
		azul.makeMove(new AzulPlayerMove(0, 5, 3, 1));
		azul.makeMove(new AzulPlayerMove(3, 3, 4, 2));
		azul.makeMove(new AzulPlayerMove(2, 3, 1, 1));
		azul.makeMove(new AzulPlayerMove(0, 2, 5, 5));
		azul.makeMove(new AzulPlayerMove(0, 1, 3, 1));
		azul.makeMove(new AzulPlayerMove(1, 3, 4, 2));
		azul.makeMove(new AzulPlayerMove(0, 4, 4, 3));
		azul.bPrint();
		azul.makeMove(new AzulPlayerMove(0, 5, 5, 2));
		azul.bPrint();
		System.out.println("TileBag: " + azul.getTileBag());
		System.out.println("TileBox: " + azul.getTileBox());
		colors = new int[] {4, 1, 2, 5, 1, 1, 2, 3, 4, 4, 4, 1, 1, 1, 4, 2, 5, 1, 3, 3, 2, 1, 4, 5, 1, 1, 5, 5};
		selections = getSelections(azul, colors);
		azul.makeMove(new AzulSetupMove(2, selections));
		azul.bPrint();
		System.out.println("TileBag: " + azul.getTileBag());
		System.out.println("TileBox: " + azul.getTileBox());
		azul.makeMove(new AzulPlayerMove(5, 3, 5, 2));
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
