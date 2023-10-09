package com.chess.engine.pieces;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
public class King extends Piece{
    private final static int[] CANDIDATE_MOVE_COORDINATE = {-9, -8 -7, -1, 1, 7, 8, 9};
    King(int piecePosition, Alliance pieceAlliance) {
        super(piecePosition, pieceAlliance);
    }
    @Override
    public Collection<Move> calculatedLegalMoves(Board board) {
        final List<Move> legalMoves = new ArrayList<>();
        int candidateDestinationCoordinate;
        for(final int currentCandidateOffset : CANDIDATE_MOVE_COORDINATE){
            candidateDestinationCoordinate = this.piecePosition + currentCandidateOffset;
        }
        return ImmutableList.copyOf(legalMoves);
    }
}