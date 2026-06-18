package com.chess.engine.player.ai;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveStatus;
import com.chess.engine.player.MoveTransition;

/** Level 3 — classic minimax (no pruning), fixed depth 3. */
public final class MiniMaxStrategy implements MoveStrategy {
    private static final BoardEvaluator EVALUATOR = new StandardBoardEvaluator();
    private static final int DEPTH = 3;

    @Override
    public Move execute(final Board board) {
        Move bestMove = Move.NULL_MOVE;
        int bestValue = Integer.MIN_VALUE;
        for(final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                final int value = board.currentPlayer().getAlliance().isWhite()
                        ? min(transition.getTransitionBoard(), DEPTH - 1)
                        : max(transition.getTransitionBoard(), DEPTH - 1);
                if(value >= bestValue) {
                    bestValue = value;
                    bestMove = move;
                }
            }
        }
        return bestMove;
    }

    private int max(final Board board, final int depth) {
        if(depth == 0 || isEndGame(board)) return EVALUATOR.evaluate(board, depth);
        int max = Integer.MIN_VALUE;
        for(final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                max = Math.max(max, min(transition.getTransitionBoard(), depth - 1));
            }
        }
        return max;
    }

    private int min(final Board board, final int depth) {
        if(depth == 0 || isEndGame(board)) return EVALUATOR.evaluate(board, depth);
        int min = Integer.MAX_VALUE;
        for(final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                min = Math.min(min, max(transition.getTransitionBoard(), depth - 1));
            }
        }
        return min;
    }

    private boolean isEndGame(final Board board) {
        return board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStaleMate();
    }

    @Override
    public String getStrategyName() { return "MiniMax (Level 3)"; }
}
