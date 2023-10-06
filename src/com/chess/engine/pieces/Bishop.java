package com.chess.engine.pieces;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
public class Bishop extends Piece{
    private final static int[] CANDIDATE_MOVE_VECTOR_COORDINATE = {-9, -7, 7, 9};
    Bishop(int piecePosition, Alliance pieceAlliance) {
        super(piecePosition, pieceAlliance);
    }
    @Override
    public Collection<Move> calculatedLegalMoves(Board board) {
        final List<Move> legalMoves = new ArrayList<>();
        for(final int candidateCoordinateOffset : CANDIDATE_MOVE_VECTOR_COORDINATE){
            int candidateDestinationCoordinate = this.piecePosition;
            while(BoardUtils.isValidTileCoordinate(candidateDestinationCoordinate)){

            }
        }
        return null;
    }
}
