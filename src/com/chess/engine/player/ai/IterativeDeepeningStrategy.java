package com.chess.engine.player.ai;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveStatus;
import com.chess.engine.player.MoveTransition;
import java.util.ArrayList;
import java.util.List;

/**
 * Level 5 — iterative deepening alpha-beta with transposition table,
 * Zobrist hashing, and aspiration windows.
 */
public final class IterativeDeepeningStrategy implements MoveStrategy {
    private static final BoardEvaluator EVALUATOR = new StandardBoardEvaluator();
    private static final MoveOrdering MOVE_ORDERING = MoveOrdering.get();
    private static final long TIME_LIMIT_MS = 2000L;
    private static final int ASPIRATION_WINDOW = 50;

    private final TranspositionTable tt = new TranspositionTable();

    @Override
    public Move execute(final Board board) {
        MOVE_ORDERING.reset();
        Move bestMove = Move.NULL_MOVE;
        int bestScore = 0;
        final long deadline = System.currentTimeMillis() + TIME_LIMIT_MS;

        for(int depth = 1; depth <= 20; depth++) {
            if(System.currentTimeMillis() > deadline) break;
            // Aspiration window
            int alpha = (depth == 1) ? Integer.MIN_VALUE + 1 : bestScore - ASPIRATION_WINDOW;
            int beta  = (depth == 1) ? Integer.MAX_VALUE     : bestScore + ASPIRATION_WINDOW;

            SearchResult result = search(board, depth, alpha, beta, deadline);
            if(result == null) break; // time expired

            // Widen window on fail-low/high
            if(result.score <= alpha || result.score >= beta) {
                result = search(board, depth, Integer.MIN_VALUE + 1, Integer.MAX_VALUE, deadline);
                if(result == null) break;
            }
            bestScore = result.score;
            if(result.move != null && result.move != Move.NULL_MOVE) {
                bestMove = result.move;
            }
        }
        return bestMove;
    }

    private SearchResult search(final Board board, final int depth, int alpha, final int beta, final long deadline) {
        Move bestMove = Move.NULL_MOVE;
        int bestScore = Integer.MIN_VALUE + 1;
        final List<Move> moves = MOVE_ORDERING.orderMoves(board.currentPlayer().getLegalMoves(), depth);
        for(final Move move : moves) {
            if(System.currentTimeMillis() > deadline) return null;
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                final int value = -negamax(transition.getTransitionBoard(), depth - 1, -beta, -alpha, deadline);
                if(value > bestScore) {
                    bestScore = value;
                    bestMove = move;
                }
                if(value > alpha) alpha = value;
                if(alpha >= beta) break;
            }
        }
        return new SearchResult(bestMove, bestScore);
    }

    private int negamax(final Board board, final int depth, int alpha, final int beta, final long deadline) {
        if(System.currentTimeMillis() > deadline) return 0;

        final long key = ZobristHasher.hash(board);
        final TranspositionTable.Entry entry = tt.probe(key);
        if(entry != null && entry.depth() >= depth) {
            if(entry.flag() == TranspositionTable.EXACT) return entry.score();
            if(entry.flag() == TranspositionTable.LOWER) alpha = Math.max(alpha, entry.score());
            if(entry.flag() == TranspositionTable.UPPER) {
                final int newBeta = Math.min(beta, entry.score());
                if(alpha >= newBeta) return entry.score();
            }
        }

        if(depth == 0 || isEndGame(board)) {
            return sign(board) * EVALUATOR.evaluate(board, depth);
        }

        Move bestMove = null;
        int origAlpha = alpha;
        final List<Move> moves = MOVE_ORDERING.orderMoves(board.currentPlayer().getLegalMoves(), depth);
        for(final Move move : moves) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                final int value = -negamax(transition.getTransitionBoard(), depth - 1, -beta, -alpha, deadline);
                if(value > alpha) {
                    alpha = value;
                    bestMove = move;
                }
                if(alpha >= beta) {
                    MOVE_ORDERING.recordKiller(move, depth);
                    tt.store(key, depth, alpha, TranspositionTable.LOWER, bestMove);
                    return alpha;
                }
            }
        }
        final int flag = (alpha > origAlpha) ? TranspositionTable.EXACT : TranspositionTable.UPPER;
        tt.store(key, depth, alpha, flag, bestMove);
        return alpha;
    }

    private boolean isEndGame(final Board board) {
        return board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStaleMate();
    }
    private int sign(final Board board) {
        return board.currentPlayer().getAlliance().isWhite() ? 1 : -1;
    }

    private record SearchResult(Move move, int score) {}

    @Override
    public String getStrategyName() { return "Iterative Deepening + TT (Level 5)"; }
}
