package com.chess.engine.pieces;
import com.chess.engine.Alliance;
import java.util.List;
public class Piece {
    protected final int piecePosition;
    protected final Alliance pieceAlliance;
    Piece(final int piecePosition, final Alliance pieceAlliance){
        this.piecePosition = piecePosition;
        this.pieceAlliance = pieceAlliance;
    }
    public abstract List<Move> calculatedLegalMoves(final Board board);

}
