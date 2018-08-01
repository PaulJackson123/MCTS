package azul;

import main.Board;
import main.CallLocation;
import main.Move;

import java.util.*;

public class Azul implements Board {

	private static final int EXPANSIONS_PER_NODE = 3;
	// Colors are: 0-null 1-blue 2-yellow 3-red 4-black 5-teal
	private int numPlayers;
	private boolean variantPlay;
	private int[] points;
	private double[] scores;
	private int[][][] walls; // [player][row][column]
	private int[][] lineColors; // [player][line]
	private int[][] lineCounts; // [player][line] numberPlaced
	private List<List<Integer>> floors; // [player] tile colors, zero for playFirstTile
	private List<int[]> factories = new ArrayList<>(); // each List element is a factory holding 4-element array of colors
	private List<Integer> centerTiles = new ArrayList<>();
	private List<Integer> tileBag = new LinkedList<>();
	private List<Integer> tileBox = new ArrayList<>();
	private int playFirstTile = -1; // 0-based player number - -1 means play-first tile is still in center of board
	private int currentPlayer = -1; // 0-based player number - -1 is chance player that sets board between rounds
	private int turn = 1; // 1-based turn used to detect stalemate where game cannot end
	private boolean roundComplete = true;
	private boolean draw;
	private boolean gameOver;
	private boolean initialized;

	private static int[] factoriesPerPlayer = {0, 0, 5, 7, 9};
	private static final double[] WEIGHTS = new double[EXPANSIONS_PER_NODE];

	private static final Random RANDOM = new Random();

	static {
		Arrays.fill(WEIGHTS, 1.0d);
	}

	private Map<AzulPlayerMove, Double> heuristics;

	Azul(final int numPlayers, final boolean variantPlay) {
		this.numPlayers = numPlayers;
		this.variantPlay = variantPlay;
		points = new int[numPlayers];
		scores = new double[numPlayers];
		walls = new int[numPlayers][5][5];
		lineColors = new int[numPlayers][5];
		lineCounts = new int[numPlayers][5];
		floors = new ArrayList<>(2);
		for (int x = 0; x < numPlayers; x++) {
			floors.add(new ArrayList<>());
		}
	}

	@Override
	public Azul duplicate() {
		Azul z = new Azul(numPlayers, variantPlay);
		copy1d(points, z.points, numPlayers);
		copy3d(walls, z.walls, numPlayers, 5, 5);
		copy2d(lineColors, z.lineColors, numPlayers, 5);
		copy2d(lineCounts, z.lineCounts, numPlayers, 5);
		for (List<Integer> f : z.floors) {
			floors.add(new ArrayList<>(f));
		}
		z.tileBox.addAll(tileBox);
		z.tileBag.addAll(tileBag);
		for (int[] factory : factories) {
			z.factories.add(factory.clone());
		}
		z.centerTiles.addAll(centerTiles);
		z.playFirstTile = playFirstTile;
		z.currentPlayer = currentPlayer;
		z.turn = turn;
		z.roundComplete = roundComplete;
		z.draw = draw;
		z.gameOver = gameOver;
		z.initialized = initialized;
		return z;
	}

	private void copy3d(final int[][][] src, final int[][][] dest, final int s1, final int s2, final int s3) {
		for (int i = 0; i < s1; i++) {
			copy2d(src[i], dest[i], s2, s3);
		}
	}

	private void copy2d(final int[][] src, final int[][] dest, final int s1, final int s2) {
		for (int i = 0; i < s1; i++) {
			copy1d(src[i], dest[i], s2);
		}
	}

	private void copy1d(final Object src, final Object dest, final int s) {
		System.arraycopy(src, 0, dest, 0, s);
	}

	private void fill2d(final int[][] ints, final int s1, int val) {
		for (int i = 0; i < s1; i++) {
			fill1d(ints[i], val);
		}
	}

	private void fill1d(int[] ints, int val) {
		Arrays.fill(ints, val);
	}

	@Override // TODO: Cache this? At least for player moves
	public List<Move> getMoves(final CallLocation location) {
		List<Move> moves = new ArrayList<>();
		if (currentPlayer == -1) {
			if (!roundComplete) {
				throw new IllegalStateException("Incorrect to initialize board before round complete.");
			}
			for (int m = 0; m < EXPANSIONS_PER_NODE; m++) {
				int numSelections = factoriesPerPlayer[numPlayers] * 4;
				int[] factorySelections = new int[numSelections];
				int bagSize = initialized ? tileBag.size() : 100;
				for (int i = 0; i < numSelections; i++) {
					if (bagSize == 0) {
						bagSize = tileBox.size();
					}
					factorySelections[i] = RANDOM.nextInt(bagSize--);
				}
				int nextPlayer = playFirstTile == -1 ? RANDOM.nextInt(numPlayers) : playFirstTile;
				moves.add(new AzulSetupMove(nextPlayer, factorySelections));
			}
		} else {
			// Temp structure for holding tiles for a factory. Used to eliminate duplicate moves.
			List<Integer> colors = new ArrayList<>(4);
			for (int factory = 0; factory < factories.size(); factory++) {
				colors.clear();
				for (int i : factories.get(factory)) {
					colors.add(i);
				}
				addMovesForColors(moves, colors, factory + 1);
			}
			// Take from center
			colors.clear();
			colors.addAll(centerTiles);
			addMovesForColors(moves, colors, 0);
		}
		return moves;
	}

	private void addMovesForColors(final List<Move> moves, final List<Integer> colors, final int factory) {
		while (colors.size() > 0) {
			Integer color = colors.get(0);
			int count = removeAll(colors, color);
			for (int row = 0; row < 5; row++) {
				int lineColor = lineColors[currentPlayer][row];
				int lineCount = lineCounts[currentPlayer][row];
				if (lineCount < row + 1 && // can't add tile to full pattern line
						(lineColor == 0 || lineColor == color) && // can't mix colors on pattern line
						getColumnWithColor(walls[currentPlayer][row], color) == 0 // can't place a tile on a row that already has that color
						) {
					String moveLegal = isMoveLegal(factory, color, row + 1, count);
					if (moveLegal == null) {
						moves.add(new AzulPlayerMove(factory, color, row + 1, count));
					} else {
						throw new IllegalStateException("shouldn't" + moveLegal);
					}

				}
			}
			// Floor it
			if (isMoveLegal(factory, color, 0, count) == null) {
				moves.add(new AzulPlayerMove(factory, color, 0, count));
			}
		}
	}

	/**
	 * Removes all elements matching element from List
	 *
	 * @param list    The list from which to remove the elements
	 * @param element The element to remove from the list
	 * @return The number of elements removed
	 */
	private int removeAll(final List<Integer> list, final Integer element) {
		int count = 0;
		int index;
		while ((index = list.indexOf(element)) != -1) {
			list.remove(index);
			count++;
		}
		return count;
	}

	@Override
	public void makeMove(final Move m) {
		if (m instanceof AzulSetupMove) {
			if (factories.size() != 0) {
				throw new IllegalStateException("Cannot make setup move when there are factories left.");
			}
			AzulSetupMove setupMove = (AzulSetupMove) m;
			if (!initialized) {
				// This is setup for a new game
				for (int i = 1; i <= 5; i++) {
					for (int j = 0; j < 20; j++) {
						tileBag.add(i);
					}
				}
				initialized = true;
			}
			fillFactories(setupMove.getFactorySelections());
			currentPlayer = setupMove.getNextPlayer();
			roundComplete = false;
			playFirstTile = -1;
		} else {
			AzulPlayerMove playerMove = (AzulPlayerMove) m;
			int factory = playerMove.getFactory();
			int color = playerMove.getColor();
			int line = playerMove.getLine();
			int tilesCount = 0;
			// Where is tile taken from
			if (factory == 0) {
				if (playFirstTile == -1) {
					playFirstTile = currentPlayer;
					floors.get(currentPlayer).add(0);
				}
				Iterator<Integer> iterator = centerTiles.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == color) {
						iterator.remove();
						tilesCount++;
					}
				}
			} else {
				for (int tile : factories.remove(factory - 1)) {
					if (tile == color) {
						tilesCount++;
					} else {
						centerTiles.add(tile);
					}
				}
			}
			// Where does tile go
			String error = isMoveLegal(factory, color, line, tilesCount);
			if (error != null) {
				throw new IllegalArgumentException(error);
			}
			// Count is not 0
			if (line == 0) {
				floor(color, tilesCount);
			} else {
				lineColors[currentPlayer][line - 1] = color;
				int total = lineCounts[currentPlayer][line - 1] + tilesCount;
				if (total <= line) {
					lineCounts[currentPlayer][line - 1] += tilesCount;
				} else {
					lineCounts[currentPlayer][line - 1] = line;
					floor(color, total - line);
				}
			}
			if (isEndOfRound()) {
				scoreRound();
				roundComplete = true;
				currentPlayer = -1;
				if (isEndOfGame()) {
					endGame();
				}
			} else {
				currentPlayer = (currentPlayer + 1) % numPlayers;
			}
			turn++;
		}
	}

	private void floor(int color, int count) {
		for (int x = 0; x < count; x++) {
			floors.get(currentPlayer).add(color);
		}
	}

	private void fillFactories(int[] factorySelections) {
		int s = 0;
		for (int f = 0; f < factoriesPerPlayer[numPlayers]; f++) {
			int[] factory = new int[4];
			for (int i = 0; i < 4; i++) {
				if (tileBag.isEmpty()) {
					if (tileBox.isEmpty()) {
						// In the rare case that you run out of tiles again while there are none left in the lid,
						// start the new round as usual even though not all Factory displays are properly filled.
						if (i > 0) {
							int[] partial = new int[i];
							System.arraycopy(factory, 0, partial, 0, i);
							factories.add(partial);
						}
						return;
					}
					tileBag.addAll(tileBox);
					tileBox.clear();
				}
				// This rearranging approach selects from the middle but trims from the end for efficiency
				factory[i] = tileBag.set(factorySelections[s++], tileBag.get(tileBag.size() - 1));
				tileBag.remove(tileBag.size() - 1);
			}
			factories.add(factory);
		}
	}

	String isMoveLegal(int factory, int color, int line) {
		if (factory < 0 || factory > factories.size()) {
			return "There is no factory " + factory;
		}
		if (color < 1 || color > 5) {
			return "Color not in range 1-5";
		}
		if (line < 0 || line > 5) {
			return "Line not in range 0-5";
		}
		return isMoveLegal(factory, color, line, getTilesCount(factory, color));
	}

	int getTilesCount(int factory, int color) {
		int tilesCount;
		if (factory == 0) {
			tilesCount = (int) centerTiles.stream().filter(t -> t == color).count();
		} else {
			tilesCount = (int) getMatchCount(factories.get(factory - 1), color);
		}
		return tilesCount;
	}

	private String isMoveLegal(int factory, int color, int line, int tilesCount) {
		if (tilesCount == 0) {
			return "There are no " + asColor2(color) + " tiles in factory " + factory;
		} else if (line == 0) {
			return null;
		} else if (lineCounts[currentPlayer][line - 1] > 0 && lineColors[currentPlayer][line - 1] != color) {
			return "Line " + line + " is " + asColor2(lineColors[currentPlayer][line - 1]) +
					".  Cannot place " + asColor2(color) + " tile.";
		} else if (lineCounts[currentPlayer][line - 1] == line) {
			return "Line " + line + " is full.";
		} else if (getColumnWithColor(walls[currentPlayer][line - 1], color) != 0) {
			return "Wall already has " + asColor2(color) + " tile on line " + line + ".";
		} else if (variantPlay) {
			return "Wall already has " + asColor2(color) + " tile on line " + line + ".";
		} else {
			return null;
		}
	}

	private void endGame() {
		if (turn >= 100) {
			Arrays.fill(scores, 0.5);
			draw = true;
		} else {
			scoreGame();

			// Award 1.0 points to lone winner or 0.5 to those that are tied for first.
			double highest = -1.0;
			for (int i = 0; i < numPlayers; i++) {
				if (points[i] > highest) {
					highest = points[i];
					draw = false;
				} else if (points[i] == highest) {
					draw = true;
				}
			}
			double award = draw ? 0.5 : 1.0;
			for (int i = 0; i < numPlayers; i++) {
				scores[i] = points[i] == highest ? award : 0.0;
			}
		}

		gameOver = true;
	}

	/**
	 * Calculates points for completed lines and applies floor penalty. Moves completed tiles to tileBox.
	 */
	private void scoreRound() {
		if (variantPlay) {
			throw new UnsupportedOperationException("TODO: support tile placement at end of round");
		}
		// Score all players at once
		for (int player = 0; player < numPlayers; player++) {
			int[][] wall = walls[player];
			int[] lineColor = lineColors[player];
			int[] lineCount = lineCounts[player];
			int roundScore = 0;
			for (int row = 0; row < 5; row++) {
				int color = lineColor[row];
				int count = lineCount[row];
				if (count == row + 1) {
					int col = getColumnForColor(row, color);
					int horizontalNeighbors = 0;
					for (int c = col - 1; c >= 0 && wall[row][c] != 0; c--) {
						horizontalNeighbors++;
					}
					for (int c = col + 1; c < 5 && wall[row][c] != 0; c++) {
						horizontalNeighbors++;
					}
					int verticalNeighbors = 0;
					for (int r = row - 1; r >= 0 && wall[r][col] != 0; r--) {
						verticalNeighbors++;
					}
					for (int r = row + 1; r < 5 && wall[r][col] != 0; r++) {
						verticalNeighbors++;
					}
					if (horizontalNeighbors > 0 && verticalNeighbors > 0) {
						roundScore += horizontalNeighbors + verticalNeighbors + 2;
					} else if (horizontalNeighbors > 0) {
						roundScore += horizontalNeighbors + 1;
					} else if (verticalNeighbors > 0) {
						roundScore += verticalNeighbors + 1;
					} else {
						roundScore += 1;
					}
					// Move one tile to wall
					wall[row][col] = color;
					// Move remaining tiles to tile box
					for (int ignore = 1; ignore < count; ignore++) {
						tileBox.add(color);
					}
					// Remove tiles from the line
					lineColor[row] = 0;
					lineCount[row] = 0;
				}
			}
			List<Integer> tiles = floors.get(player);
			switch (tiles.size()) {
				case 0:
					break;
				case 1:
					roundScore--;
					break;
				case 2:
					roundScore -= 2;
					break;
				case 3:
					roundScore -= 4;
					break;
				case 4:
					roundScore -= 6;
					break;
				case 5:
					roundScore -= 8;
					break;
				default:
					roundScore -= (tiles.size() - 5) * 3 + 8;
					break;
			}
			for (Integer x : tiles) {
				if (x != 0) {
					tileBox.add(x);
				}
			}
			tiles.clear();

			points[player] = points[player] + roundScore > 0 ? points[player] + roundScore : 0;
		}
	}

	// Add bonuses for completed rows, columns and colors
	private void scoreGame() {
		for (int player = 0; player < numPlayers; player++) {
			int[][] wall = walls[player];
			int bonuses = 0;
			int tilesInRow[] = new int[5];
			int tilesInCol[] = new int[5];
			int colorCount[] = new int[5];
			for (int row = 0; row < 5; row++) {
				for (int col = 0; col < 5; col++) {
					if (wall[row][col] > 0) {
						tilesInRow[row]++;
						tilesInCol[col]++;
						colorCount[wall[row][col] - 1]++;
					}
				}
			}
			bonuses += getMatchCount(tilesInRow, 5) * 2;
			bonuses += getMatchCount(tilesInCol, 5) * 7;
			bonuses += getMatchCount(colorCount, 5) * 10;
			points[player] += bonuses;
		}
	}

	private long getMatchCount(int[] array, int i) {
		long count = 0L;
		for (int c : array) {
			if (c == i) {
				count++;
			}
		}
		return count;
	}

	@Override
	public boolean gameOver() {
		return gameOver;
	}

	@Override
	public int getCurrentPlayer() {
		return currentPlayer;
	}

	@Override
	public int getQuantityOfPlayers() {
		return numPlayers;
	}

	int[] getPoints() {
		return points;
	}

	@Override
	public double[] getScore() {
		return scores;
	}

	@Override
	public double[] getMoveWeights() {
		return WEIGHTS;
	}

	@Override
	public void bPrint() {
		System.out.println("Factories");
		List<Integer> tmpCenter = centerTiles;
		if (playFirstTile == -1) {
			tmpCenter = new ArrayList<>();
			tmpCenter.add(0);
			tmpCenter.addAll(centerTiles);
		}
		printTiles(0, sort(tmpCenter));
		for (int i = 0; i < factories.size(); i++) {
			printTiles(i + 1, sort(factories.get(i)));
		}

		System.out.println();
		for (int p = 0; p < numPlayers; p++) {
			System.out.println("Player " + p + " (" + points[p] + ")");
			for (int r = 0; r < 5; r++) {
				// print line
				int spaces = 5 - lineCounts[p][r];
				System.out.print(repeat("   ", spaces));
				System.out.print(repeat(asColor2(lineColors[p][r]) + " ", lineCounts[p][r]));
				System.out.print(" ");
				// print wall
				for (int c = 0; c < 5; c++) {
					// lower case for board, upper case for player tiles
					int wallColor = walls[p][r][c] == 0 ? getWallColor(r, c) : walls[p][r][c];
					String wallString = walls[p][r][c] == 0 && variantPlay
							? ".."
							: asColor2(wallColor, walls[p][r][c] == 0);
					System.out.print(" " + wallString);
				}
				System.out.println();
			}
			System.out.print("Floor: ");
			for (Integer t : floors.get(p)) {
				System.out.print(asColor2(t) + " ");
			}
			System.out.println();
		}
	}

	private void printTiles(int i, int[] tiles) {
		System.out.print(i + ") ");
		for (int tile : tiles) {
			System.out.print(asColor2(tile));
			System.out.print(" ");
		}
		System.out.println();
	}

	private int[] sort(List<Integer> tiles) {
		int[] temp = new int[tiles.size()];
		for (int i = 0; i < tiles.size(); i++) {
			temp[i] = tiles.get(i);
		}
		return sort(temp);
	}

	private int[] sort(int[] tiles) {
		int[] temp = tiles.clone();
		Arrays.sort(temp);
		return temp;
	}

	/**
	 * Identifies the color for a spot on the wall of a standard board.
	 *
	 * @param row 0-based row
	 * @param col 0-based column
	 * @return a 1-based color
	 */
	private int getWallColor(final int row, final int col) {
		return (5 + col - row) % 5 + 1;
	}

	/**
	 * Identifies the column having the given color on the given row on a standard board.
	 *
	 * @param row   0-based row
	 * @param color 1-based col
	 * @return a 0-based column
	 */
	private int getColumnForColor(final int row, final int color) {
		return (color - 1 + row) % 5;
	}

	/**
	 * Identifies the column having the given color on the given row on a standard board.
	 *
	 * @param line  a 5-element array of colors representing a row on a wall
	 * @param color 1-based col
	 * @return a 1-based column number if rhe color is found, 0 otherwise
	 */
	private int getColumnWithColor(final int[] line, final int color) {
		for (int i = 0, lineLength = line.length; i < lineLength; i++) {
			if (line[i] == color) {
				return i + 1;
			}
		}
		return 0;
	}

	private String repeat(final String s, final int count) {
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			sb.append(s);
		}
		return sb.toString();
	}

	private String asColor2(final int tile) {
		return asColor2(tile, false);
	}

	private String asColor2(final int tile, final boolean lowerCase) {
		String s;
		switch (tile) {
			case 0:
				s = "xx";
				break;
			case 1:
				s = "bl";
				break;
			case 2:
				s = "yl";
				break;
			case 3:
				s = "rd";
				break;
			case 4:
				s = "bk";
				break;
			case 5:
				s = "tl";
				break;
			default:
				s = "??";
				break;
		}
		return lowerCase ? s : s.toUpperCase();
	}

	private boolean isEndOfRound() {
		return factories.isEmpty() && centerTiles.isEmpty();
	}

	private boolean isEndOfGame() {
		for (int[][] wall : walls) {
			for (int[] line : wall) {
				boolean b = true;
				for (int t : line) {
					if (t == 0) {
						b = false;
						break;
					}
				}
				if (b) {
					return true;
				}
			}
		}
		return turn >= 1000;
	}

	int getFactoryCount() {
		return factories.size();
	}

	/**
	 * Scores board. Finds lead. Returns 1.0 for infinite lead, -1.0 for infinite trail, 0.0 for tied for lead.
	 */
	double getHeuristic(AzulPlayerMove move, double expCoef) {
		Double h = null;
		if (heuristics == null) {
			heuristics = new HashMap<>();
		} else {
			h = heuristics.get(move);
		}
		if (h == null) {
			heuristics.put(move, h = calculateHeuristic(move, expCoef));
		}
		return h;
	}

	private Double calculateHeuristic(AzulPlayerMove move, double expCoef) {
		Azul b = duplicate();
		b.makeMove(move);
		if (!b.isEndOfRound()) {
			b.scoreRound();
		}
		if (!b.isEndOfGame()) {
			b.scoreGame();
		}
		int best = 0; // TODO is -1 better init?
		int next = 0;
		boolean draw = true;
		for (int point : b.points) {
			if (point > best) {
				next = best;
				best = point;
				draw = false;
			} else if (point == best) {
				draw = true;
			}
		}
		int point = b.points[currentPlayer];
		int diff = 0;

		if (point != best) {
			diff = point - best;
		} else if (draw) {
			diff = 0;
		} else {
			diff = point - next;
		}
		// Logistic function - range [0, 1.0]
		return 1.0d / (1 + Math.exp((0 - diff) * expCoef));
	}
}
