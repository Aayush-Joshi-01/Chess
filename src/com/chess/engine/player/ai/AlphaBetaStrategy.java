package com.chess.engine.player.ai;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveStatus;
import com.chess.engine.player.MoveTransition;
import java.util.List;

/** Level 4 — negamax with alpha-beta pruning, depth 4, with basic move ordering. */
public final class AlphaBetaStrategy implements MoveStrategy {
    private static final BoardEvaluator EVALUATOR = new StandardBoardEvaluator();
    private static final MoveOrdering MOVE_ORDERING = MoveOrdering.get();
    private static final int DEPTH = 4;

    @Override
    public Move execute(final Board board) {
        MOVE_ORDERING.reset();
        Move bestMove = Move.NULL_MOVE;
        int bestValue = Integer.MIN_VALUE;
        final int alpha = Integer.MIN_VALUE + 1;
        final int beta  = Integer.MAX_VALUE;
        final List<Move> moves = MOVE_ORDERING.orderMoves(board.currentPlayer().getLegalMoves(), DEPTH);
        for(final Move move : moves) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                final int value = -negamax(transition.getTransitionBoard(), DEPTH - 1, -beta, -alpha);
                if(value > bestValue) {
                    bestValue = value;
                    bestMove = move;
                }
            }
        }
        return bestMove;
    }

    private int negamax(final Board board, final int depth, int alpha, final int beta) {
        if(depth == 0 || isEndGame(board)) {
            return sign(board) * EVALUATOR.evaluate(board, depth);
        }
        final List<Move> moves = MOVE_ORDERING.orderMoves(board.currentPlayer().getLegalMoves(), depth);
        for(final Move move : moves) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                final int value = -negamax(transition.getTransitionBoard(), depth - 1, -beta, -alpha);
                if(value >= beta) return beta; // beta cutoff
                if(value > alpha) alpha = value;
            }
        }
        return alpha;
    }

    private boolean isEndGame(final Board board) {
        return board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStaleMate();
    }

    private int sign(final Board board) {
        return board.currentPlayer().getAlliance().isWhite() ? 1 : -1;
    }

    @Override
    public String getStrategyName() { return "Alpha-Beta (Level 4)"; }
}
