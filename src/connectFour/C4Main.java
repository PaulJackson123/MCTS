package connectFour;

import main.MCTS;
import main.Move;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Scanner;

class C4Main {
	private static int scorePlayer0 = 0;
	private static int scorePlayer1 = 0;
	private static int draws = 0;
	private static double[] scr;

	private static void run(int games, int it, double exp, boolean bounds, double pess, double opti) {
		MCTS player = new MCTS();
		player.setExplorationConstant(exp);
		player.setTimeDisplay(true);

		scorePlayer0 = 0;
		scorePlayer1 = 0;
		draws = 0;
		scr = new double[2];

		// Set this to true to take control yourself!
		boolean playerControl = false;
		int activePlayerID = 0;
		Scanner scanner = new Scanner(System.in);

		for (int i = 0; i < games; i++) {
			ConnectFour gameInstance = new ConnectFour();
			while (true) {
				//cf.print();

				if (gameInstance.currentPlayer != activePlayerID || !playerControl) {
					Move m = player.runMCTS_UCT(gameInstance, it, 0L);
					gameInstance.makeMove(m);
				} else {
					System.out.println("Enter a row: ");
					int n = scanner.nextInt();
					Move m = new ConnectFourMove(n);
					gameInstance.makeMove(m);
				}

				//cf.print();

				if (gameInstance.gameOver()) {
					scr = gameInstance.getScore();
					if (scr[0] > 0.8) {
						scorePlayer0++;
					} else if (scr[1] > 0.8) {
						scorePlayer1++;
					} else {
						draws++;
					}

					gameInstance.print();
					System.out.println("Score Vector: " + Arrays.toString(scr));
					System.out.println(i + ": Scores: " + scorePlayer0 + " / " + scorePlayer1 + " Draws: " + draws);
					break;
				}
			}

		}
		scanner.close();

		System.out.println("Iterations: " + it + " Exp. Con.: " + exp);
		System.out.println("pessBias: " + pess + " optiBias: " + opti);
		System.out.println("Score for this run: " + scorePlayer0 + " / " + scorePlayer1 + " Draws: " + draws);

		try {
			FileOutputStream fos = new FileOutputStream("newRuns_5.txt", true);
			Writer writer = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));

			writer.write("Iterations: " + it + " Exp. Con.: " + exp + ", Bounds = " + bounds + "\n");
			writer.write("pessBias: " + pess + " optiBias: " + opti + "\n");
			writer.write("Score for this run: " + scorePlayer0 + " / " + scorePlayer1 + " Draws: " + draws + "\n");
			writer.close();
			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		run(100, 200000, 1.4d, false, 0, 0);
		//run(100, 50000, 1.45d, false, 0, 0);
		//run(100, 50000, 1.3d, false, 0, 0);
		//run(100, 75000, 3.0, false, 0, 0);
		//run(100, 100000, 3.0, false, 0, 0);
		//run(100, 150000, 3.0, false, 0, 0);
		//run(100, 200000, 3.0, false, 0, 0);
		//run(100, 250000, 3.0, false, 0, 0);
		//run(100, 300000, 3.0, false, 0, 0);	
	}
}
