package com.chess.engine.player.ai;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Level 1 — picks a uniformly random legal move. */
public final class RandomMoveStrategy implements MoveStrategy {
    @Override
    public Move execute(final Board board) {
        final List<Move> legalMoves = new ArrayList<>(board.currentPlayer().getLegalMoves());
        if(legalMoves.isEmpty()) return Move.NULL_MOVE;
        Collections.shuffle(legalMoves);
        return legalMoves.get(0);
    }
    @Override
    public String getStrategyName() { return "Random (Level 1)"; }
}
