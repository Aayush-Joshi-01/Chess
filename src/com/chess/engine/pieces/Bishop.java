package com.chess.engine.pieces;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.MajorMove;
import com.chess.engine.board.Tile;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
public class Bishop extends Piece{
    private final static int[] CANDIDATE_MOVE_VECTOR_COORDINATE = {-9, -7, 7, 9}; // vector coordinates for bishop movement
    public Bishop(final Alliance pieceAlliance, final int piecePosition) {
        super(PieceType.BISHOP, piecePosition, pieceAlliance);
    }
    @Override
    public Collection<Move> calculatedLegalMoves(final Board board) {
        final List<Move> legalMoves = new ArrayList<>();
        for(final int candidateCoordinateOffset : CANDIDATE_MOVE_VECTOR_COORDINATE){
            int candidateDestinationCoordinate = this.piecePosition;
            while(BoardUtils.isValidTileCoordinate(candidateDestinationCoordinate)){
                if(
                        isFirstColumnExclusion(candidateDestinationCoordinate, candidateCoordinateOffset) ||
                                isEighthColumnExclusion(candidateDestinationCoordinate, candidateCoordinateOffset)
                ) {
                    break;
                }
                candidateDestinationCoordinate += candidateCoordinateOffset;
                if(BoardUtils.isValidTileCoordinate(candidateDestinationCoordinate)){
                    final Tile candidateDestinationTile = board.getTile(candidateDestinationCoordinate);
                    if (!candidateDestinationTile.isTileOccupied()){
                        legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
                    }
                    else{
                        final Piece pieceAtDestination = candidateDestinationTile.getPiece();
                        //getting the pieces
                        final Alliance pieceAlliance = pieceAtDestination.getPieceAlliance();
                        if(this.pieceAlliance != pieceAlliance){
                            // if the piece is of enemy then we add that move as well to the set of legal moves.
                            legalMoves.add(
                                    new Move.AttackMove(
                                            board,
                                            this,
                                            candidateDestinationCoordinate,
                                            pieceAtDestination
                                    )
                            );
                        }
                        break;
                    }
                }
            }
        }
        return ImmutableList.copyOf(legalMoves);
    }
    @Override
    public Bishop movePiece(Move move) {
        return new Bishop(move.getMovedPiece().pieceAlliance, move.getDestinationCoordinate());
    }
    @Override
    public String toString(){
        return PieceType.BISHOP.toString();
    }
    private static boolean isFirstColumnExclusion(final int currentPosition, final int candidateOffset){
        return BoardUtils.FIRST_COLUMN[currentPosition] && (candidateOffset == -9 ||  candidateOffset == 7);
    }
    private static boolean isEighthColumnExclusion(final int currentPosition, final int candidateOffset){
        return BoardUtils.EIGHTH_COLUMN[currentPosition] && (candidateOffset == -7 || candidateOffset == 9);
    }
}
