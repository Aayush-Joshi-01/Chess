package com.chess.engine.player.ai;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.MoveStatus;

/** Level 2 — depth-1 greedy: picks the move with the best immediate evaluation. */
public final class GreedyStrategy implements MoveStrategy {
    private static final BoardEvaluator EVALUATOR = new StandardBoardEvaluator();

    @Override
    public Move execute(final Board board) {
        Move bestMove = Move.NULL_MOVE;
        int highestValue = Integer.MIN_VALUE;
        for(final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                final int currentValue = board.currentPlayer().getAlliance().isWhite()
                        ? EVALUATOR.evaluate(transition.getTransitionBoard(), 0)
                        : -EVALUATOR.evaluate(transition.getTransitionBoard(), 0);
                if(currentValue >= highestValue) {
                    highestValue = currentValue;
                    bestMove = move;
                }
            }
        }
        return bestMove;
    }
    @Override
    public String getStrategyName() { return "Greedy (Level 2)"; }
}
