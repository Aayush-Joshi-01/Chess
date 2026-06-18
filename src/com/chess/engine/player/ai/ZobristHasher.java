package com.chess.engine.player.ai;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.pieces.Piece;
import java.util.Random;

/** Computes a Zobrist hash for a board position for use in the transposition table. */
public final class ZobristHasher {
    // 64 squares × 12 piece types (6 types × 2 alliances)
    private static final long[][] PIECE_KEYS = new long[64][12];
    private static final long SIDE_TO_MOVE_KEY;

    static {
        final Random rng = new Random(0xDEADBEEF_CAFEBABEL);
        for(int sq = 0; sq < 64; sq++){
            for(int p = 0; p < 12; p++){
                PIECE_KEYS[sq][p] = rng.nextLong();
            }
        }
        SIDE_TO_MOVE_KEY = rng.nextLong();
    }

    private ZobristHasher() {}

    public static long hash(final Board board) {
        long h = 0L;
        for(final Piece piece : board.getWhitePieces()){
            h ^= PIECE_KEYS[piece.getPiecePosition()][pieceIndex(piece)];
        }
        for(final Piece piece : board.getBlackPieces()){
            h ^= PIECE_KEYS[piece.getPiecePosition()][pieceIndex(piece)];
        }
        if(board.currentPlayer().getAlliance() == Alliance.BLACK){
            h ^= SIDE_TO_MOVE_KEY;
        }
        return h;
    }

    private static int pieceIndex(final Piece piece) {
        final int typeOrdinal = piece.getPieceType().ordinal(); // 0-5
        final int colorOffset = piece.getPieceAlliance().isWhite() ? 0 : 6;
        return typeOrdinal + colorOffset;
    }
}
