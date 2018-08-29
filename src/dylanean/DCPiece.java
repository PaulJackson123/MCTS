package dylanean;

import main.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract class DCPiece {
	private final int pieceType; // 1 through 4
	private final int rank;
	private final int file;

	private DCPiece(int pieceType, int rank, int file) {
		this.pieceType = pieceType;
		this.rank = rank;
		this.file = file;
	}

	static DCPiece createDCPiece(int p, int rank, int file) {
		int pieceType = Math.abs(p);
		switch (pieceType) {
			case 1:
				return new Soldier(pieceType, rank, file);
			case 2:
				return new Archer(pieceType, rank, file);
			case 3:
				return new Knight(pieceType, rank, file);
			case 4:
				return new King(pieceType, rank, file);
		}
		throw new IllegalArgumentException("Invalid piece type:" + pieceType);
	}

	/**
	 * An ordered array of the legal moves this piece can make. Each move is a
	 * {rank, file} two-element array indicating the relative offset of the new
	 * position. All other rules concerning edge of board, jumping over other
	 * pieces, leaving oneself in check, etc, must still be applied.
	 *
	 * @param player the player making the moves
	 */
	abstract int[][] getLegalOffsets(int player);

	/**
	 * An ordered array of the relative board position that must be unoccupied
	 * in order for the complementary legalOffset to be a valid move. Each element
	 * of this array applies to the element at the same index of getLegalMoves.
	 *
	 * A null indicates that there is no position that must be blank. It happens to
	 * be the case that no move requires more than one blank square (the 3-space
	 * King move is a jump.)
	 *
	 * @param player the player making the moves
	 */
	abstract int[][] getBarriers(int player);

	/**
	 * Returns the (signed) piece that this player can uniquely attack. Returns 0
	 * if this player (the king) can attack any other piece.
	 *
	 * @param player The player doing the attacking
	 *
	 * @return The piece that this player's piece can attack
	 */
	abstract int getCanAttack(int player);

	/**
	 * Returns the moves that this player's piece can legally make.
	 *
	 * @param chess The current state of the game
	 * @param player The player making the move
	 *
	 * @return The list of legal moves for this piece
	 */
	Collection<DCMove> getMoves(DylaneanChess chess, int player) {
		// TODO: Pass in list to add pieces to
		List<DCMove> moves = new ArrayList<>();
		int[][] board = chess.getBoard();
		int canAttack = getCanAttack(player);
		int[][] legalOffsets = getLegalOffsets(player);
		int[][] barriers = getBarriers(player);
		for (int i = 0; i < legalOffsets.length; i++) {
			int[] offset = legalOffsets[i];
			int toRank = rank + offset[0];
			int toFile = file + offset[1];
			if (toRank >= 0 && toRank < 12 && toFile >=0 && toFile < 6) {
				int[] barrier = barriers[i];
				if (barrier == null || board[rank + barrier[0]][file + barrier[1]] == 0) {
					addLegalMove(moves, chess, toRank, toFile, canAttack, player);
				}
			}
		}
		return moves;
	}

	boolean hasAnyMoves(DylaneanChess chess, int player) {
		int[][] board = chess.getBoard();
		int canAttack = getCanAttack(player);
		int[][] legalOffsets = getLegalOffsets(player);
		int[][] barriers = getBarriers(player);
		for (int i = 0; i < legalOffsets.length; i++) {
			int[] offset = legalOffsets[i];
			int toRank = rank + offset[0];
			int toFile = file + offset[1];
			if (toRank >= 0 && toRank < 12 && toFile >=0 && toFile < 6) {
				int[] barrier = barriers[i];
				if (barrier == null || board[rank + barrier[0]][file + barrier[1]] == 0) {
					if (isLegalMove(chess, toRank, toFile, canAttack, player)) {
						return true;
					}
				}
			}
		}
		return false;
	}


	/**
	 * It is not valid to call board.getMoves() and look to see if opponent's king is
	 * the target, because taking the king is valid even when taking the king would
	 * put the attacker's king into check. Those moves would not be returned by getMoves().
	 *
	 * @param board the board
	 * @param player the player making the move, NOT the opponent
	 *
	 * @return whether the player has its opponent in check
	 */
	boolean hasOpponentInCheck(int[][] board, int player) {
		int king = player == 0 ? 4 : -4;
		int[][] legalOffsets = getLegalOffsets(player);
		int[][] barriers = getBarriers(player);
		for (int i = 0; i < legalOffsets.length; i++) {
			int[] offset = legalOffsets[i];
			int toRank = rank + offset[0];
			int toFile = file + offset[1];
			if (toRank >= 0 && toRank < 12 && toFile >=0 && toFile < 6) {
				int[] barrier = barriers[i];
				if (barrier == null || board[rank + barrier[0]][file + barrier[1]] == 0) {
					if (board[toRank][toFile] == king) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void addLegalMove(List<DCMove> moves, DylaneanChess chess, int toRank, int toFile, int canAttack, int player) {
		DCMove move = getLegalMove(chess, toRank, toFile, canAttack, player);
		if (move != null) {
			moves.add(move);
		}
	}

	private boolean isLegalMove(DylaneanChess chess, int toRank, int toFile, int canAttack, int player) {
		return getLegalMove(chess, toRank, toFile, canAttack, player) != null;
	}

	private DCMove getLegalMove(DylaneanChess chess, int toRank, int toFile, int canAttack, int player) {
		int[][] board = chess.getBoard();
		int target = board[toRank][toFile];
		if (target == 0 ||
				canAttack == target || // Attacking complementary piece
				(player == 0 ? target > 0 : target < 0 ) && canAttack == 0 || // King is attacking
				(player == 0 ? target == 4 : target == -4)) { // Attacking the king
			// TODO: Rather than create new copy each time, have caller pass a single copy and make temp changes and restore them before returning
			int[][] copy = new int[12][6];
			Utils.copy2d(board, copy, 12, 6);
			copy[rank][file] = 0;
			copy[toRank][toFile] = player == 0 ? 0 - pieceType : pieceType;
			// TODO: player could come from board
			// Check to make sure player did not leave or put self in check
			int opponent = DylaneanChess.OTHER_PLAYER[player];
			Iterable<DCPiece> opponentPieces = chess.getPlacedPieces(opponent);
			for (DCPiece opponentPiece : opponentPieces) {
				if (opponentPiece.rank != toRank || opponentPiece.file != toFile) {
					if (opponentPiece.hasOpponentInCheck(copy, opponent)) {
						return null;
					}
				}
			}
			return new DCMove(pieceType, this.rank, this.file, toRank, toFile);
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DCPiece that = (DCPiece) o;
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

	/**
	 * The soldier can one space in any direction
	 */
	private static class Soldier extends DCPiece {
		private static final int[][] LEGAL_OFFSETS = new int[][] {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
		private static final int[][] BARRIERS = new int[][] {null, null, null, null, null, null, null, null};

		private Soldier(int pieceType, int rank, int file) {
			super(pieceType, rank, file);
		}

		public int[][] getLegalOffsets(int player) {
			return LEGAL_OFFSETS;
		}

		public int[][] getBarriers(int player) {
			return BARRIERS;
		}

		@Override
		int getCanAttack(int player) {
			return player == 0 ? 2 : -2;
		}
	}

	/**
	 * The archer can move once space diagonally, or two spaces forward
	 */
	private static class Archer extends DCPiece {
		private static final int[][] LEGAL_OFFSETS_0 = new int[][] {{2, 0}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
		private static final int[][] LEGAL_OFFSETS_1 = new int[][] {{-2, 0}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
		private static final int[][] BARRIERS_0 = new int[][] {{1, 0}, null, null, null, null};
		private static final int[][] BARRIERS_1 = new int[][] {{-1, 0}, null, null, null, null};

		private Archer(int pieceType, int rank, int file) {
			super(pieceType, rank, file);
		}

		@Override
		int[][] getLegalOffsets(int player) {
			return player == 0 ? LEGAL_OFFSETS_0 : LEGAL_OFFSETS_1;
		}

		@Override
		int[][] getBarriers(int player) {
			return player == 0 ? BARRIERS_0 : BARRIERS_1;
		}

		@Override
		int getCanAttack(int player) {
			return player == 0 ? 3 : -3;
		}
	}

	/**
	 * The knight can move two spaces horizontally or vertically
	 */
	private static class Knight extends DCPiece {
		private static final int[][] LEGAL_OFFSETS = new int[][] {{2, 0}, {0, 2}, {-2, 0}, {0, -2}};
		private static final int[][] BARRIERS = new int[][] {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

		private Knight(int pieceType, int rank, int file) {
			super(pieceType, rank, file);
		}

		@Override
		int[][] getLegalOffsets(int player) {
			return LEGAL_OFFSETS;
		}

		@Override
		int[][] getBarriers(int player) {
			return BARRIERS;
		}

		@Override
		int getCanAttack(int player) {
			return player == 0 ? 1 : -1;
		}
	}

	/**
	 * The king can move 2 diagonally, 1 horizontally, or jump 3 vertically
	 */
	private static class King extends DCPiece {
		private static final int[][] LEGAL_OFFSETS = new int[][] {{3, 0}, {2, 2}, {0, 1}, {-2, 2}, {-3, 0}, {-2, -2}, {0, -1}, {2, -2}};
		private static final int[][] BARRIERS = new int[][] {null, {1, 1}, null, {-1, 1}, null, {-1, -1}, null, {1, -1}};

		private King(int pieceType, int rank, int file) {
			super(pieceType, rank, file);
		}

		@Override
		int[][] getLegalOffsets(int player) {
			return LEGAL_OFFSETS;
		}

		@Override
		int[][] getBarriers(int player) {
			return BARRIERS;
		}

		@Override
		int getCanAttack(int player) {
			return 0;
		}
	}
}
