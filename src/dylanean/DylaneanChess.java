package dylanean;

import main.Board;
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
	private Set<DCPiece>[] placedPieces = new Set[2]; // [player]
	private boolean setupPhase = true;
	private int currentPlayer = 0;
	private int movesSinceCapture = 0;
	private boolean draw;
	private boolean gameOver;

	static final int[] OTHER_PLAYER = new int[] {1, 0};

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
		placedPieces[player].add(DCPiece.createDCPiece(4, rank, file));
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
		movesSinceCapture = z.movesSinceCapture;
//		draw = z.draw;
//		gameOver = z.gameOver;
	}

	// For testing mid-game positions
	DylaneanChess(int[][] board, int currentPlayer) {
		copy2d(board, this.board, 12, 6);
		this.setupPhase = false;
		placedPieces[0] = new HashSet<>();
		placedPieces[1] = new HashSet<>();
		for (int rank = 0; rank < 12; rank++) {
			for (int file = 0; file < 6; file++) {
				int pieceType = board[rank][file];
				if (pieceType != 0) {
					DCPiece piece = DCPiece.createDCPiece(Math.abs(pieceType), rank, file);
					if (pieceType < 0) {
						placedPieces[0].add(piece);
					}
					else {
						placedPieces[1].add(piece);
					}
				}
			}
		}

		this.currentPlayer = currentPlayer;
	}

	@Override
	public DylaneanChess duplicate() {
		return new DylaneanChess(this);
	}

	@Override
	public List<Move> getMoves() {
		List<Move> moves = new ArrayList<>();
		if (setupPhase) {
			int pieces[] = unplacedPieces[currentPlayer];
			Set<OpenSpot> spots = openSpots[currentPlayer];
			for (int p = 0; p < 3; p++) {
				// Don't need duplicate moves
				if (pieces[p] > 0) {
					for (OpenSpot spot : spots) {
						moves.add(new DCSetupMove(p + 1, spot.rank, spot.file));
					}
				}
			}
		}
		else {
			for (DCPiece piece : placedPieces[currentPlayer]) {
				moves.addAll(piece.getMoves(this, currentPlayer));
			}
		}
		return moves;
	}

	Iterable<DCPiece> getPlacedPieces(int player) {
		return placedPieces[player];
	}

	@Override
	public void makeMove(final Move m) {
		if (m instanceof DCSetupMove) {
			DCSetupMove setupMove = (DCSetupMove) m;
			int piece = setupMove.getPiece();
			int rank = setupMove.getRank();
			int file = setupMove.getFile();
			// TODO: Remove legality checks
			String message = isSetupMoveLegal(piece, rank, file);
			if (message != null) {
				throw new IllegalArgumentException(message);
			}
			int sign = currentPlayer == 0 ? -1 : 1;
			board[rank][file] = piece * sign;
			unplacedPieces[currentPlayer][piece - 1]--;
			openSpots[currentPlayer].remove(new OpenSpot(rank, file));
			placedPieces[currentPlayer].add(DCPiece.createDCPiece(piece, rank, file));
			currentPlayer = OTHER_PLAYER[currentPlayer];
			// This is testing whether the NEXT player has any setup moves to make
			setupPhase = placedPieces[currentPlayer].size() < 19;
		} else {
			DCMove move = (DCMove) m;
			int fromRank = move.getFromRank();
			int fromFile = move.getFromFile();
			int toRank = move.getToRank();
			int toFile = move.getToFile();
			int piece = board[fromRank][fromFile];
			int target = board[toRank][toFile];
			String message = canAttackPiece(piece, target);
			if (message != null) {
				bPrint();
				System.out.println("Player 0 pieces: " + placedPieces[0]);
				System.out.println("Player 1 pieces: " + placedPieces[1]);
				System.out.println(move);
				throw new IllegalArgumentException(message);
			}

			board[fromRank][fromFile] = 0;
			board[toRank][toFile] = piece;
			placedPieces[currentPlayer].remove(DCPiece.createDCPiece(piece, fromRank, fromFile));
			placedPieces[currentPlayer].add(DCPiece.createDCPiece(piece, toRank, toFile));
			if (target == 0) {
				movesSinceCapture++;
			} else {
				boolean removed = placedPieces[OTHER_PLAYER[currentPlayer]].remove(DCPiece.createDCPiece(target, toRank, toFile));
				assert removed;
				movesSinceCapture = 0;
			}
			currentPlayer = OTHER_PLAYER[currentPlayer];
			gameOver = true;
			for (DCPiece placedPiece : placedPieces[currentPlayer]) {
				if (placedPiece.hasAnyMoves(this, currentPlayer)) {
					gameOver = false;
					break;
				}
			}
			if (gameOver) {
				// Stalemate, unless player is in check
				draw = !isPlayerInCheck(currentPlayer);
			}
			else if (movesSinceCapture == 50) {
				gameOver = true;
				draw = true;
			}
			if (gameOver) {
				if (draw) {
					scores[0] = 0.5d;
					scores[1] = 0.5d;
				}
				else {
					scores[currentPlayer] = 0.0d;
					scores[OTHER_PLAYER[currentPlayer]] = 1.0d;
				}
			}
		}
	}

	private boolean isPlayerInCheck(int player) {
		int otherPlayer = OTHER_PLAYER[player];
		for (DCPiece piece : placedPieces[otherPlayer]) {
			if (piece.hasOpponentInCheck(this.board, otherPlayer)) {
				return true;
			}
		}
		return false;
	}

	private int getPieceOwner(int piece) {
		if (piece == 0) {
			return -1;
		}
		else if (piece < 0) {
			return 0;
		}
		else {
			return 1;
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

	String isMoveLegal(int fromRank, int fromFile, int toRank, int toFile) {
		if (setupPhase) {
			return "Still in setup phase";
		}
		int piece = board[fromRank][fromFile];
		int target = board[toRank][toFile];
		if (piece == 0) {
			return "Position " + toChars(fromRank, fromFile) + " is empty";
		}
		int owner = getPieceOwner(piece);
		if (owner != currentPlayer) {
			return "Piece is owned by player " + owner;
		}
		DCPiece placedPiece = DCPiece.createDCPiece(piece, fromRank, fromFile);
		String moveLegal = canAttackPiece(piece, target);
		if (moveLegal != null) {
			return moveLegal;
		}
		if (isLegalMove(fromRank, fromFile, toRank, toFile, piece, placedPiece)) {
			return "Not a legal move.";
		}
		return null;
	}

	private boolean isLegalMove(int fromRank, int fromFile, int toRank, int toFile, int piece, DCPiece placedPiece) {
		// This is only called to validate human moves so inefficiency is OK.
		Collection<DCMove> moves = placedPiece.getMoves(this, currentPlayer);
		DCMove move = new DCMove(Math.abs(piece), fromRank, fromFile, toRank, toFile);
		return !moves.contains(move);
	}

	private String canAttackPiece(int fromPiece, int toPiece) {
		int owner = getPieceOwner(toPiece);
		if (currentPlayer == owner) {
			return "Target piece is owned by current player";
		}
		if (!DylaneanChess.LEGAL_MOVES_NO_LOCKING[Math.abs(fromPiece)][Math.abs(toPiece)]) {
			return toChars(fromPiece).toUpperCase() + " cannot attack " + toChars(toPiece).toUpperCase();
		}
		return null;
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
		System.out.println("#===0===1===2===3===4===5==#");
		for (int r = 11; r >= 0; r--) {
			System.out.print("" + (char) ('a' + r) +  " ");
			for (int f = 0; f < 6; f++) {
				int piece = board[r][f];
				if (piece == 0) {
					// Use :::: to represent dark squares on the board
					System.out.print((r + f) % 2 == 0 ? "::::" : "    ");
				}
				else {
					char c = (r + f) % 2 == 0 ? ':' : ' ';
					System.out.print("" + c + toChars(piece) + c);
				}
			}
			System.out.println(" " + (char) ('a' + r));
		}
		System.out.println("#===0===1===2===3===4===5==#");
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

	@Override
	public int getNextPlayer(Move move) {
		return OTHER_PLAYER[currentPlayer];
	}

	static String toChars(int piece) {
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
		return "" + (char) ('A' + toRank) + toFile;
	}

	public int[][] getBoard() {
		return board;
	}

	boolean isSetupPhase() {
		return setupPhase;
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

}
