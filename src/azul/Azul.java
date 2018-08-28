package azul;

import main.Board;
import main.Move;
import main.Utils;

import java.util.*;

class Azul implements Board {

	private static final int EXPANSIONS_PER_NODE = 7;
	private static final double[] WEIGHTS = new double[EXPANSIONS_PER_NODE];
	static final Random RANDOM = new Random();
	private static final int MAX_TURNS = 1000;
	private static final int MAX_TURN_SCORE = 5 + 5 + 2 + 7 + 10; // 29
	static int[] factoriesPerPlayer = {0, 0, 5, 7, 9};

	static {
		Arrays.fill(WEIGHTS, 1.0d);
	}

	// Colors are: 0-null 1-blue 2-yellow 3-red 4-black 5-teal
	private byte numPlayers;
	private boolean variantPlay;
	private int[] points;
	private double[] scores;
	private byte[][][] walls; // [player][row][column]
	private byte[][] lineColors; // [player][line]
	private byte[][] lineCounts; // [player][line] numberPlaced
	private List<List<Byte>> floors; // [player] tile colors, zero for playFirstTile
	private List<byte[]> factories = new ArrayList<>(); // each List element is a factory holding 4-element array of colors
	private List<Byte> centerTiles = new ArrayList<>();
	private List<Byte> tileBag = new ArrayList<>();
	private List<Byte> tileBox = new ArrayList<>();
	private byte playFirstTile = -1; // 0-based player number - -1 means play-first tile is still in center of board
	private byte currentPlayer = -1; // 0-based player number - -1 is chance player that sets board between rounds
	private int turn = 1; // 1-based turn used to detect stalemate where game cannot end
	private boolean roundComplete = true;
	private boolean draw;
	private boolean gameOver;
	private Map<AzulPlayerMove, Double> heuristics;

	public Azul(byte numPlayers, boolean variantPlay) {
		init(numPlayers, variantPlay);
		// This is setup for a new game
		for (byte i = 1; i <= 5; i++) {
			for (int j = 0; j < 20; j++) {
				tileBag.add(i);
			}
		}
	}

	public Azul(int numPlayers, boolean variantPlay) {
		this((byte) numPlayers, variantPlay);
	}

	private Azul(Azul z) {
		init(z.numPlayers, z.variantPlay);
		Utils.copy1d(z.points, points, numPlayers);
		Utils.copy3d(z.walls, walls, numPlayers, 5, 5);
		Utils.copy2d(z.lineColors, lineColors, numPlayers, 5);
		Utils.copy2d(z.lineCounts, lineCounts, numPlayers, 5);
		for (List<Byte> f : z.floors) {
			floors.add(new ArrayList<>(f));
		}
		tileBox.addAll(z.tileBox);
		tileBag.addAll(z.tileBag);
		for (byte[] factory : z.factories) {
			factories.add(factory.clone());
		}
		centerTiles.addAll(z.centerTiles);
		playFirstTile = z.playFirstTile;
		currentPlayer = z.currentPlayer;
		turn = z.turn;
		roundComplete = z.roundComplete;
		draw = z.draw;
		gameOver = z.gameOver;
	}

	private void init(byte numPlayers, boolean variantPlay) {
		this.numPlayers = numPlayers;
		this.variantPlay = variantPlay;
		points = new int[numPlayers];
		scores = new double[numPlayers];
		walls = new byte[numPlayers][5][5];
		lineColors = new byte[numPlayers][5];
		lineCounts = new byte[numPlayers][5];
		floors = new ArrayList<>(2);
		for (int x = 0; x < numPlayers; x++) {
			floors.add(new ArrayList<>());
		}
	}

	@Override
	public Azul duplicate() {
		return new Azul(this);
	}

	@Override // TODO: Cache this? At least for player moves
	public List<Move> getMoves() {
		List<Move> moves = new ArrayList<>();
		if (currentPlayer == -1) {
			if (!roundComplete) {
				throw new IllegalStateException("Incorrect to initialize board before round complete.");
			}
			for (int m = 0; m < EXPANSIONS_PER_NODE; m++) {
				int numSelections = factoriesPerPlayer[numPlayers] * 4;
				byte[] factorySelections = new byte[numSelections];
				int bagSize = tileBag.size();
				for (int i = 0; i < numSelections; i++) {
					if (bagSize == 0) {
						bagSize = tileBox.size();
					}
					factorySelections[i] = (byte) RANDOM.nextInt(bagSize--);
				}
				byte nextPlayer = playFirstTile == -1 ? (byte) RANDOM.nextInt(numPlayers) : playFirstTile;
				moves.add(new AzulSetupMove(nextPlayer, factorySelections));
			}
		} else {
			// Temp structure for holding tiles for a factory. Used to eliminate duplicate moves.
			List<Byte> colors = new ArrayList<>(4);
			for (int factory = 0; factory < factories.size(); factory++) {
				colors.clear();
				for (byte i : factories.get(factory)) {
					colors.add(i);
				}
				addMovesForColors(moves, colors, (byte) (factory + 1));
			}
			// Take from center
			colors.clear();
			colors.addAll(centerTiles);
			addMovesForColors(moves, colors, (byte) 0);
		}
		return moves;
	}

	private void addMovesForColors(List<Move> moves, List<Byte> colors, byte factory) {
		while (colors.size() > 0) {
			byte color = colors.get(0);
			byte count = removeAll(colors, color);
			for (byte row = 0; row < 5; row++) {
				byte lineColor = lineColors[currentPlayer][row];
				byte lineCount = lineCounts[currentPlayer][row];
				if (lineCount < row + 1 && // can't add tile to full pattern line
						(lineColor == 0 || lineColor == color) && // can't mix colors on pattern line
						getColumnWithColor(walls[currentPlayer][row], color) == 0 // can't place a tile on a row that already has that color
						) {
					String moveLegal = isMoveLegal(factory, color, row + 1, count);
					if (moveLegal == null) {
						moves.add(new AzulPlayerMove(factory, color, (byte) (row + 1), count));
					} else {
						throw new IllegalStateException("shouldn't" + moveLegal);
					}

				}
			}
			// Floor it
			if (isMoveLegal(factory, color, 0, count) == null) {
				moves.add(new AzulPlayerMove(factory, color, (byte) 0, count));
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
	private byte removeAll(List<Byte> list, Byte element) {
		byte count = 0;
		int index;
		while ((index = list.indexOf(element)) != -1) {
			list.remove(index);
			count++;
		}
		return count;
	}

	@Override
	public void makeMove(Move m) {
		if (m instanceof AzulSetupMove) {
			if (factories.size() != 0) {
				throw new IllegalStateException("Cannot make setup move when there are factories left.");
			}
			AzulSetupMove setupMove = (AzulSetupMove) m;
			fillFactories(setupMove.getFactorySelections());
			currentPlayer = setupMove.getNextPlayer();
			roundComplete = false;
			playFirstTile = -1;
		} else {
			AzulPlayerMove playerMove = (AzulPlayerMove) m;
			byte factory = playerMove.getFactory();
			byte color = playerMove.getColor();
			byte line = playerMove.getLine();
			byte tilesCount = 0;
			// Where is tile taken from
			if (factory == 0) {
				if (playFirstTile == -1) {
					playFirstTile = currentPlayer;
					floors.get(currentPlayer).add((byte) 0);
				}
				Iterator<Byte> iterator = centerTiles.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == color) {
						iterator.remove();
						tilesCount++;
					}
				}
			} else {
				for (byte tile : factories.remove(factory - 1)) {
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
				byte total = (byte) (lineCounts[currentPlayer][line - 1] + tilesCount);
				if (total <= line) {
					lineCounts[currentPlayer][line - 1] += tilesCount;
				} else {
					lineCounts[currentPlayer][line - 1] = line;
					floor(color, (byte) (total - line));
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
				currentPlayer = (byte) ((currentPlayer + 1) % numPlayers);
			}
			turn++;
		}
	}

	private void floor(byte color, byte count) {
		for (int x = 0; x < count; x++) {
			floors.get(currentPlayer).add(color);
		}
	}

	private void fillFactories(byte[] factorySelections) {
		int s = 0;
		for (int f = 0; f < factoriesPerPlayer[numPlayers]; f++) {
			byte[] factory = new byte[4];
			for (int i = 0; i < 4; i++) {
				if (tileBag.isEmpty()) {
					if (tileBox.isEmpty()) {
						// In the rare case that you run out of tiles again while there are none left in the lid,
						// start the new round as usual even though not all Factory displays are properly filled.
						if (i > 0) {
							byte[] partial = new byte[i];
							System.arraycopy(factory, 0, partial, 0, i);
							factories.add(partial);
						}
						return;
					}
					tileBag.addAll(tileBox);
					tileBox.clear();
				}
				factory[i] = Utils.swapEndAndRemove(tileBag, factorySelections[s++]);
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
		if (turn >= MAX_TURNS) {
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
			points[player] = points[player] + getRoundScore(player, true);
		}
	}

	private int getRoundScore(int player, boolean moveTiles) {
		byte[][] wall;
		if (moveTiles) {
			wall = walls[player];
		}
		else {
			wall = new byte[5][5];
			Utils.copy2d(walls[player], wall, 5, 5);
		}
		byte[] lineColor = lineColors[player];
		byte[] lineCount = lineCounts[player];
		int roundScore = 0;
		for (int row = 0; row < 5; row++) {
			byte color = lineColor[row];
			byte count = lineCount[row];
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
				if (moveTiles) {
					// Move remaining tiles to tile box
					for (int ignore = 1; ignore < count; ignore++) {
						tileBox.add(color);
					}
					// Remove tiles from the line
					lineColor[row] = 0;
					lineCount[row] = 0;
				}
			}
		}
		List<Byte> floor = floors.get(player);
		switch (floor.size()) {
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
				roundScore -= (floor.size() - 5) * 3 + 8;
				break;
		}
		if (moveTiles) {
			for (Byte x : floor) {
				if (x != 0) {
					tileBox.add(x);
				}
			}
			floor.clear();
		}
		if (roundScore < 0) {
			roundScore = roundScore < 0 - points[player] ? 0 - points[player] : roundScore;
		}
		return roundScore;
	}

	// Add bonuses for completed rows, columns and colors
	private void scoreGame() {
		for (int player = 0; player < numPlayers; player++) {
			int bonuses = getBonuses(walls[player]);
			points[player] += bonuses;
		}
	}

	private int getBonuses(byte[][] wall) {
		int bonuses = 0;
		byte tilesInRow[] = new byte[5];
		byte tilesInCol[] = new byte[5];
		byte colorCount[] = new byte[5];
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
		return bonuses;
	}

	private long getMatchCount(byte[] array, int i) {
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

	byte getPlayFirstTile() {
		return playFirstTile;
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
		List<Byte> tmpCenter = centerTiles;
		if (playFirstTile == -1) {
			tmpCenter = new ArrayList<>();
			tmpCenter.add((byte) 0);
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
				System.out.print(Utils.repeat("   ", spaces));
				System.out.print(Utils.repeat(asColor2(lineColors[p][r]) + " ", lineCounts[p][r]));
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
			for (Byte t : floors.get(p)) {
				System.out.print(asColor2(t) + " ");
			}
			System.out.println();
		}
	}

	private void printTiles(int i, byte[] tiles) {
		System.out.print(i + ") ");
		for (int tile : tiles) {
			System.out.print(asColor2(tile));
			System.out.print(" ");
		}
		System.out.println();
	}

	private byte[] sort(List<Byte> tiles) {
		byte[] temp = new byte[tiles.size()];
		for (int i = 0; i < tiles.size(); i++) {
			temp[i] = tiles.get(i);
		}
		return sort(temp);
	}

	private byte[] sort(byte[] tiles) {
		byte[] temp = tiles.clone();
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
	private int getWallColor(int row, int col) {
		return (5 + col - row) % 5 + 1;
	}

	/**
	 * Identifies the column having the given color on the given row on a standard board.
	 *
	 * @param row   0-based row
	 * @param color 1-based col
	 * @return a 0-based column
	 */
	private int getColumnForColor(int row, int color) {
		return (color - 1 + row) % 5;
	}

	/**
	 * Identifies the column having the given color on the given row on a standard board.
	 *
	 * @param line  a 5-element array of colors representing a row on a wall
	 * @param color 1-based col
	 * @return a 1-based column number if rhe color is found, 0 otherwise
	 */
	private int getColumnWithColor(byte[] line, int color) {
		for (int i = 0, lineLength = line.length; i < lineLength; i++) {
			if (line[i] == color) {
				return i + 1;
			}
		}
		return 0;
	}

	private String asColor2(int tile) {
		return asColor2(tile, false);
	}

	private String asColor2(int tile, boolean lowerCase) {
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
		for (byte[][] wall : walls) {
			for (byte[] line : wall) {
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
		if (turn >= MAX_TURNS) {
			return true;
		}
		else {
			return false;
		}
	}

	int getFactoryCount() {
		return factories.size();
	}

	List<Byte> getTileBag() {
		return new ArrayList<>(tileBag);
	}

	List<Byte> getTileBox() {
		return new ArrayList<>(tileBox);
	}

	/**
	 * Scores board. Finds lead. Returns -1 for terrible move, 0 for neutral move, 1 for best possible move.
	 */
	double getHeuristic(AzulPlayerMove move) {
		Azul b = duplicate(); // TODO: Reduce value if there are multiple factories with this combination of tiles
		int base = b.points[currentPlayer] +
				b.getRoundScore(currentPlayer, false) +
				b.getBonuses(b.walls[currentPlayer]);
		b.makeMove(move);
		int score = b.points[currentPlayer] +
				(b.isEndOfRound() ? 0 : b.getRoundScore(currentPlayer, false)) +
				(b.isEndOfGame() ? 0 : b.getBonuses(walls[currentPlayer]));
		int delta = score - base;
		// Worst move can be very negative. Clipping at 0.
		double unclipped = delta / MAX_TURN_SCORE;
		if (unclipped < 0.0d) {
			return 0.0d;
		}
		else {
			return unclipped;
		}
	}

}
