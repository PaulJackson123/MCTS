package dylanean;

import main.Board;
import main.CallLocation;
import main.Move;

import java.util.*;

import static main.Utils.copy2d;
import static main.Utils.copy2dNullSafe;

/**
 * Chess game with 4 piece types: green soldiers, red archers, blue knights and black/white kings
 *
 * Each piece has a unique range of motion.
 * - The soldier can one space in any direction
 * - The archer can move once space diagonally, or two spaces forward
 * - The knight can move two spaces horizontally or vertically
 * - The king can move 2 diagonally, 1 horizontally, or jump 3 vertically
 *
 * Pieces cannot jump over other pieces. The only exception is the king's vertical moves.
 *
 * There are restrictions on which piece types each piece type can kill
 * - Soldiers can kill archers and the king
 * - Archers can kill knights and the king
 * - Knights can kill soldiers and the king
 * - The king can kill any other piece type, including the king
 *
 * Variations:
 * - Kings cannot attack
 * - Pieces can move less than their full range of motion
 * - Pieces of the same type (except kings) can attack their own type which results in both pieces locked in place
 *   until a 3rd piece kills one of the other two.
 * - Stalemate occurs after 50 moves with no attack
 *
 * Invented by Dylan Blythe
 */
public class DylaneanChess implements Board {

	// 0=blank, 1=soldier, 2=archer, 3=knight, 4=king, player 0 pieces are negative, 1 positive
	// Player 0 starts on ranks 0-5, player 1 on ranks 6-11. King on file 2 for 0, file 3 for 1
	private double[] scores = new double[2];
	private int[][] board = new int[12][6]; // [rank][file]
	// Each player initialized with a null array indicating that king must be placed on this turn
	private int[][] unplacedPieces = new int[2][]; // [player][pieceType - 1] count of pieces yet to be placed - null means none placed
	private Set<OpenSpot>[] openSpots = new Set[2]; // [player] 2-element immutable arrays (rank/file) of open spots
	private Set<PlacedPiece>[] placedPieces = new Set[2]; // [player]
	private boolean setupPhase = true;
	private int currentPlayer = 0;
	private boolean draw;
	private boolean gameOver;
	private Map<Move, Double> heuristics;

	// Which source pieces can attack which target pieces. [source][target] returns true if legal
	private static final boolean[][] LEGAL_MOVES_NO_LOCKING = new boolean[][] {
			// Source type 0 is meaningless
			{false, false, false, false, false},
			// Soldier
			{true, false, true, false, true},
			// Archer
			{true, false, false, true, true},
			// Knight
			{true, true, false, false, true},
			// King
			{true, true, true, true, true}
	};
	private static final boolean[][] LEGAL_MOVES_WITH_LOCKING = new boolean[][] {
			// Source type 0 is meaningless
			{false, false, false, false, false},
			// Soldier
			{true, true, true, false, true},
			// Archer
			{true, false, true, true, true},
			// Knight
			{true, true, false, true, true},
			// King
			{true, true, true, true, true}
	};

	DylaneanChess() {
		for (int i = 0; i < 2; i++) {
			placedPieces[i] = new HashSet<>();
		}
		for (int i = 0; i < 2; i++) {
			unplacedPieces[i] = new int[3];
			Arrays.fill(unplacedPieces[i], 6);

			Set<OpenSpot> spots = new HashSet<>();
			int lowRank = i == 0 ? 0 : 7;
			int highRank = i == 0 ? 5 : 12;
			for (int r = lowRank; r < highRank; r++) {
				for (int f = 0; f < 6; f++) {
					if (board[r][f] == 0) {
						spots.add(new OpenSpot(r, f));
					}
				}
			}
			openSpots[i] = spots;
		}
		setupKing(0, 0, 2);
		setupKing(1, 11, 3);
	}

	private void setupKing(int player, int rank, int file) {
		int sign = player == 0 ? -1 : 1;
		board[rank][file] = 4 * sign;
		openSpots[player].remove(new OpenSpot(rank, file));
		placedPieces[player].add(new PlacedPiece(4, rank, file));
	}

	private DylaneanChess(DylaneanChess z) {
		copy2d(z.board, board, 12, 6);
		setupPhase = z.setupPhase;
		if (z.setupPhase) {
			copy2dNullSafe(z.unplacedPieces, unplacedPieces, 2, 3);
			for (int i = 0; i < 2; i++) {
				// An openSpot is immutable so no need to clone
				openSpots[i] = new HashSet<>(z.openSpots[i]);
			}
		}
		for (int i = 0; i < 2; i++) {
			placedPieces[i] = new HashSet<>(z.placedPieces[i]);
		}
		currentPlayer = z.currentPlayer;
		draw = z.draw;
		gameOver = z.gameOver;
	}

	@Override
	public DylaneanChess duplicate() {
		return new DylaneanChess(this);
	}

	@Override
	public List<Move> getMoves(final CallLocation location) {
		List<Move> moves = new ArrayList<>();
		int pieces[] = unplacedPieces[currentPlayer];
		if (pieces == null) {
			// King placement is compulsory first move
			if (currentPlayer == 0) {
				moves.add(new DCSetupMove(4, 0, 2));
			}
			else {
				moves.add(new DCSetupMove(4, 11, 3));
			}
		}
		else {
			Set<OpenSpot> spots = openSpots[currentPlayer];
			for (int p = 0; p < 3; p++) {
				int piece = p + 1;
				for (int i = 0, count = pieces[p]; i < count; i++) {
					for (OpenSpot spot : spots) {
						moves.add(new DCSetupMove(piece, spot.rank, spot.file));
					}
				}
			}
		}
		return moves;
	}

	@Override
	public void makeMove(final Move m) {
		if (m instanceof DCSetupMove) {
			DCSetupMove setupMove = (DCSetupMove) m;
			int piece = setupMove.getPiece();
			int rank = setupMove.getRank();
			int file = setupMove.getFile();
			String message = isSetupMoveLegal(piece, rank, file);
			if (message != null) {
				throw new IllegalArgumentException(message);
			}
			int sign = currentPlayer == 0 ? -1 : 1;
			board[rank][file] = piece * sign;
			unplacedPieces[currentPlayer][piece - 1]--;
			openSpots[currentPlayer].remove(new OpenSpot(rank, file));
			placedPieces[currentPlayer].add(new PlacedPiece(piece, rank, file));
			currentPlayer = (currentPlayer + 1) % 2;
			setupPhase = placedPieces[currentPlayer].size() < 19;
		} else {
			DCMove move = (DCMove) m;
		}
	}

	String isSetupMoveLegal(int piece, int rank, int file) {
		if (!setupPhase) {
			return "Not in setup phase";
		}
		if (unplacedPieces[currentPlayer][piece - 1] == 0) {
			return "All " + toChars(piece) + " pieces have been placed";
		}
		int spot = board[rank][file];
		if (spot != 0) {
			return "Position " + toChars(rank, file) + " already contains " + toChars(spot);
		}
		return null;
	}

	String isMoveLegal(int piece, int fromRank, int fromFile, int toRank, int toFile) {
		return null;
	}

	private void endGame() {
		// Award 1.0 points to winner or 0.5 for draw.
		int winner = 0;
		double award = draw ? 0.5 : 1.0;
		for (int i = 0; i < 2; i++) {
			scores[i] = i == winner ? award : 0.0;
		}
		draw = false;
		gameOver = true;
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
		return 2;
	}

	@Override
	public double[] getScore() {
		return scores;
	}

	@Override
	public double[] getMoveWeights() {
		throw new UnsupportedOperationException("There are no random moves in Dylanean Chess.");
	}

	@Override
	public void bPrint() {
		System.out.println();
		System.out.println("#==0==1==2==3==4==5==#");
		for (int r = 11; r >= 0; r--) {
			System.out.print("" + (char) ('a' + r) +  " ");
			for (int f = 0; f < 6; f++) {
				int piece = board[r][f];
				if (piece == 0) {
					// Use ::: to represent dart squares on the board
					System.out.print((r + f) % 2 == 0 ? ":::" : "   ");
				}
				else {
					System.out.print(toChars(piece) + " ");
				}
			}
			System.out.println(" " + (char) ('a' + r));
		}
		System.out.println("#==0==1==2==3==4==5==#");
		if (setupPhase) {
			for (int player = 0; player < 2; player++) {
				System.out.print("Player " + player + ":");
				for (int piece = 1; piece < 4; piece++) {
					System.out.print(" " + toChars(piece) + "=" + unplacedPieces[player][piece - 1]);
				}
				System.out.println();
			}
		}
	}

	private static String toChars(int piece) {
		String s;
		switch (Math.abs(piece)) {
			case 1:
				s = "sr";
				break;
			case 2:
				s = "ar";
				break;
			case 3:
				s = "kt";
				break;
			case 4:
				s = "kg";
				break;
			default:
				throw new IllegalArgumentException("Illegal piece value: " + piece);
		}
		return piece < 0 ? s : s.toUpperCase();
	}

	private static String toChars(int toRank, int toFile) {
		return "" + (char) ('A' + toFile) + toRank;
	}

	private boolean isEndOfGame() {
		// TODO:
		return false;
	}

	/**
	 * Scores board. Finds lead. Returns 1.0 for infinite lead, -1.0 for infinite trail, 0.0 for tied for lead.
	 */
	double getHeuristic(Move move, double expCoef) {
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

	private Double calculateHeuristic(Move move, double expCoef) {
		DylaneanChess b = new DylaneanChess(this);
		b.makeMove(move);
		// TODO: Count  and score pieces
		return 0.0;
	}

	private static class OpenSpot {
		private final int rank;
		private final int file;

		private OpenSpot(int rank, int file) {
			this.rank = rank;
			this.file = file;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			OpenSpot that = (OpenSpot) o;
			return rank == that.rank &&
					file == that.file;
		}

		@Override
		public int hashCode() {
			return 31 * rank + file;
		}

		@Override
		public String toString() {
			return "rank=" + rank +
					", file=" + file +
					" (" + toChars(rank, file) + ")";
		}
	}

	private static class PlacedPiece {
		private final int pieceType; // -4 through 4 excluding 0
		private final int rank;
		private final int file;

		private PlacedPiece(int pieceType, int rank, int file) {
			this.pieceType = pieceType;
			this.rank = rank;
			this.file = file;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			PlacedPiece that = (PlacedPiece) o;
			return pieceType == that.pieceType &&
					rank == that.rank &&
					file == that.file;
		}

		@Override
		public int hashCode() {
			int result = pieceType;
			result = 31 * result + rank;
			result = 31 * result + file;
			return result;
		}

		@Override
		public String toString() {
			return "pieceType=" + pieceType +
					", rank=" + rank +
					", file=" + file;
		}
	}
}
