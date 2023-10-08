package com.chess.engine.pieces;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import java.util.Collection;
public class Queen extends Piece{
    Queen(int piecePosition, Alliance pieceAlliance) {
        super(piecePosition, pieceAlliance);
    }
    @Override
    public Collection<Move> calculatedLegalMoves(Board board) {
        return null;
    }
}