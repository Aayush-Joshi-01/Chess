package com.chess.engine.pieces;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import java.util.Collection;
public abstract class Piece {
    protected final int piecePosition;
    protected final Alliance pieceAlliance;
    protected final boolean isFirstMove;
    Piece(final int piecePosition, final Alliance pieceAlliance){
        this.piecePosition = piecePosition;
        this.pieceAlliance = pieceAlliance;
        // TODO to add implementation to this functionality for the pawn class.
        this.isFirstMove = false;
    }
    public Alliance getPieceAlliance(){
        return this.pieceAlliance;
    }
    public boolean isFirstMove(){
        return this.isFirstMove;
    }
    public abstract Collection<Move> calculatedLegalMoves(final Board board);
    public int getPiecePosition() {
        return this.piecePosition;
    }
}