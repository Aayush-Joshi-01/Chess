package com.chess.engine.player.ai;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.Player;
import java.util.Collection;

public final class StandardBoardEvaluator implements BoardEvaluator {

    private static final int CHECK_BONUS        = 50;
    private static final int CHECKMATE_BONUS    = 10000;
    private static final int DEPTH_BONUS        = 100;
    private static final int CASTLE_BONUS       = 60;
    private static final int MOBILITY_MULTIPLIER = 5;
    private static final int DOUBLE_PAWN_PENALTY = -30;
    private static final int ISOLATED_PAWN_PENALTY = -20;
    private static final int ROOK_OPEN_FILE_BONUS  = 25;
    private static final int ROOK_HALF_FILE_BONUS  = 10;

    // Piece-Square Tables (from White's perspective; mirror for Black)
    private static final int[] PAWN_TABLE = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] KNIGHT_TABLE = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] BISHOP_TABLE = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] ROOK_TABLE = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         0,  0,  0,  5,  5,  0,  0,  0
    };
    private static final int[] QUEEN_TABLE = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };
    private static final int[] KING_MIDDLE_TABLE = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };
    private static final int[] KING_END_TABLE = {
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    };

    @Override
    public int evaluate(final Board board, final int depth) {
        return scorePlayer(board, board.whitePlayer(), depth) -
               scorePlayer(board, board.blackPlayer(), depth);
    }

    private int scorePlayer(final Board board, final Player player, final int depth) {
        return materialValue(player) +
               mobilityScore(player) +
               checkBonus(player, depth) +
               castleBonus(player) +
               pawnStructureScore(board, player) +
               rookOpenFileScore(board, player);
    }

    private int materialValue(final Player player) {
        int score = 0;
        for(final Piece piece : player.getActivePieces()){
            score += piece.getPieceValue() + pieceSquareBonus(piece, player.getAlliance());
        }
        return score;
    }

    private int pieceSquareBonus(final Piece piece, final Alliance alliance) {
        // Map piece position to PST index (white reads table top-to-bottom from their perspective)
        final int pos = alliance.isWhite() ? piece.getPiecePosition() : mirror(piece.getPiecePosition());
        return switch(piece.getPieceType()) {
            case PAWN   -> PAWN_TABLE[pos];
            case KNIGHT -> KNIGHT_TABLE[pos];
            case BISHOP -> BISHOP_TABLE[pos];
            case ROOK   -> ROOK_TABLE[pos];
            case QUEEN  -> QUEEN_TABLE[pos];
            case KING   -> KING_MIDDLE_TABLE[pos];
        };
    }

    private int mirror(final int pos) {
        // Mirror vertically: row r becomes row 7-r
        return (7 - pos / 8) * 8 + (pos % 8);
    }

    private int mobilityScore(final Player player) {
        return player.getLegalMoves().size() * MOBILITY_MULTIPLIER;
    }

    private int checkBonus(final Player player, final int depth) {
        if(player.getOpponent().isInCheckMate()) {
            return CHECKMATE_BONUS + (depth * DEPTH_BONUS);
        }
        if(player.getOpponent().isInCheck()) {
            return CHECK_BONUS;
        }
        return 0;
    }

    private int castleBonus(final Player player) {
        return player.isCastled() ? CASTLE_BONUS : 0;
    }

    private int pawnStructureScore(final Board board, final Player player) {
        int score = 0;
        final boolean[] pawnFiles = new boolean[8];
        for(final Piece piece : player.getActivePieces()){
            if(piece.getPieceType() == Piece.PieceType.PAWN){
                final int file = piece.getPiecePosition() % 8;
                if(pawnFiles[file]){
                    score += DOUBLE_PAWN_PENALTY;
                }
                pawnFiles[file] = true;
            }
        }
        // Isolated pawn penalty
        for(int f = 0; f < 8; f++){
            if(pawnFiles[f]){
                final boolean leftEmpty  = f == 0 || !pawnFiles[f-1];
                final boolean rightEmpty = f == 7 || !pawnFiles[f+1];
                if(leftEmpty && rightEmpty){
                    score += ISOLATED_PAWN_PENALTY;
                }
            }
        }
        return score;
    }

    private int rookOpenFileScore(final Board board, final Player player) {
        int score = 0;
        final boolean[] whitePawnFiles = new boolean[8];
        final boolean[] blackPawnFiles = new boolean[8];
        for(final Piece piece : board.getWhitePieces()){
            if(piece.getPieceType() == Piece.PieceType.PAWN) whitePawnFiles[piece.getPiecePosition() % 8] = true;
        }
        for(final Piece piece : board.getBlackPieces()){
            if(piece.getPieceType() == Piece.PieceType.PAWN) blackPawnFiles[piece.getPiecePosition() % 8] = true;
        }
        for(final Piece piece : player.getActivePieces()){
            if(piece.getPieceType() == Piece.PieceType.ROOK){
                final int file = piece.getPiecePosition() % 8;
                final boolean ownPawn = player.getAlliance().isWhite() ? whitePawnFiles[file] : blackPawnFiles[file];
                final boolean oppPawn = player.getAlliance().isWhite() ? blackPawnFiles[file] : whitePawnFiles[file];
                if(!ownPawn && !oppPawn) score += ROOK_OPEN_FILE_BONUS;
                else if(!ownPawn)        score += ROOK_HALF_FILE_BONUS;
            }
        }
        return score;
    }
}
