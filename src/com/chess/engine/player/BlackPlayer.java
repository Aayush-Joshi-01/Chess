package com.chess.engine.player;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.Piece;
import com.chess.engine.pieces.Rook;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BlackPlayer extends Player{
    public BlackPlayer(final Board board, final Collection<Move> whiteStandardLegalMoves,
                       final Collection<Move> blackStandardLegalMoves) {
        super(board, blackStandardLegalMoves, whiteStandardLegalMoves);
    }
    @Override
    public Collection<Piece> getActivePieces() { return this.board.getBlackPieces(); }
    @Override
    public Alliance getAlliance() { return Alliance.BLACK; }
    @Override
    public Player getOpponent() { return this.board.whitePlayer(); }

    @Override
    protected Collection<Move> calculateKingCastles(final Collection<Move> playerLegals,
                                                     final Collection<Move> opponentLegals) {
        final List<Move> kingCastles = new ArrayList<>();
        if(this.playerKing.isFirstMove() && !this.isInCheck()) {
            // King-side castle (squares 5, 6 must be empty)
            if(!this.board.getTile(5).isTileOccupied() && !this.board.getTile(6).isTileOccupied()) {
                final Tile rookTile = this.board.getTile(7);
                if(rookTile.isTileOccupied() && rookTile.getPiece().isFirstMove() &&
                   rookTile.getPiece().getPieceType().isRook()) {
                    if(Player.calculateAttacksOnTile(5, opponentLegals).isEmpty() &&
                       Player.calculateAttacksOnTile(6, opponentLegals).isEmpty()) {
                        kingCastles.add(new Move.KingSideCastleMove(
                                this.board, this.playerKing, 6,
                                (Rook) rookTile.getPiece(), 7, 5));
                    }
                }
            }
            // Queen-side castle (squares 1, 2, 3 must be empty)
            if(!this.board.getTile(3).isTileOccupied() && !this.board.getTile(2).isTileOccupied() &&
               !this.board.getTile(1).isTileOccupied()) {
                final Tile rookTile = this.board.getTile(0);
                if(rookTile.isTileOccupied() && rookTile.getPiece().isFirstMove() &&
                   rookTile.getPiece().getPieceType().isRook()) {
                    if(Player.calculateAttacksOnTile(2, opponentLegals).isEmpty() &&
                       Player.calculateAttacksOnTile(3, opponentLegals).isEmpty()) {
                        kingCastles.add(new Move.QueenSideCastleMove(
                                this.board, this.playerKing, 2,
                                (Rook) rookTile.getPiece(), 0, 3));
                    }
                }
            }
        }
        return kingCastles;
    }
}
