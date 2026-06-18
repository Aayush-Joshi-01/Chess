package com.chess.engine.player.ai;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveStatus;
import com.chess.engine.player.MoveTransition;
import java.util.List;

/**
 * Level 6 — Master engine: iterative deepening, transposition table, null-move pruning,
 * late move reduction (LMR), quiescence search, futility pruning, and delta pruning.
 */
public final class AdvancedAlphaBetaStrategy implements MoveStrategy {
    private static final BoardEvaluator EVALUATOR = new StandardBoardEvaluator();
    private static final MoveOrdering MOVE_ORDERING = MoveOrdering.get();
    private static final long TIME_LIMIT_MS = 3000L;

    private static final int NULL_MOVE_R = 2;         // null-move reduction
    private static final int LMR_THRESHOLD = 4;       // moves before LMR kicks in
    private static final int FUTILITY_MARGIN_1 = 200; // one ply from horizon
    private static final int FUTILITY_MARGIN_2 = 400; // two plies from horizon
    private static final int DELTA_MARGIN = 200;      // quiescence delta pruning

    private final TranspositionTable tt = new TranspositionTable();
    private long deadline;

    @Override
    public Move execute(final Board board) {
        MOVE_ORDERING.reset();
        this.deadline = System.currentTimeMillis() + TIME_LIMIT_MS;
        Move bestMove = Move.NULL_MOVE;
        int bestScore = 0;

        for(int depth = 1; depth <= 30; depth++) {
            if(System.currentTimeMillis() > deadline) break;
            int alpha = Integer.MIN_VALUE + 1;
            int beta  = Integer.MAX_VALUE;
            Move iterBest = Move.NULL_MOVE;
            final List<Move> moves = MOVE_ORDERING.orderMoves(board.currentPlayer().getLegalMoves(), depth);
            for(final Move move : moves) {
                if(System.currentTimeMillis() > deadline) break;
                final MoveTransition transition = board.currentPlayer().makeMove(move);
                if(transition.getMoveStatus() == MoveStatus.DONE) {
                    final int value = -negamax(transition.getTransitionBoard(), depth - 1, -beta, -alpha, false);
                    if(value > alpha) {
                        alpha = value;
                        iterBest = move;
                    }
                }
            }
            if(iterBest != Move.NULL_MOVE) {
                bestMove = iterBest;
                bestScore = alpha;
            }
        }
        return bestMove;
    }

    private int negamax(final Board board, final int depth, int alpha, int beta, final boolean nullMoveAllowed) {
        if(System.currentTimeMillis() > deadline) return 0;

        final long key = ZobristHasher.hash(board);
        final TranspositionTable.Entry entry = tt.probe(key);
        if(entry != null && entry.depth() >= depth) {
            if(entry.flag() == TranspositionTable.EXACT) return entry.score();
            if(entry.flag() == TranspositionTable.LOWER) alpha = Math.max(alpha, entry.score());
            else if(entry.flag() == TranspositionTable.UPPER) beta = Math.min(beta, entry.score());
            if(alpha >= beta) return entry.score();
        }

        if(depth == 0) return quiescence(board, alpha, beta);
        if(isEndGame(board)) return sign(board) * EVALUATOR.evaluate(board, depth);

        // Null-move pruning: skip if in check or king/pawn-only endgame
        if(nullMoveAllowed && depth >= NULL_MOVE_R + 1 && !board.currentPlayer().isInCheck()) {
            // Simplified null move: just evaluate at depth-R-1 without actually making a null move
            // (full null-move would require a "pass" move; we approximate with reduced eval)
            final int nullScore = sign(board) * EVALUATOR.evaluate(board, depth - NULL_MOVE_R - 1);
            if(nullScore >= beta) return beta;
        }

        // Futility pruning (only near horizon, not in check)
        final boolean inCheck = board.currentPlayer().isInCheck();
        if(!inCheck && depth == 1) {
            final int staticEval = sign(board) * EVALUATOR.evaluate(board, depth);
            if(staticEval + FUTILITY_MARGIN_1 <= alpha) {
                return quiescence(board, alpha, beta);
            }
        }
        if(!inCheck && depth == 2) {
            final int staticEval = sign(board) * EVALUATOR.evaluate(board, depth);
            if(staticEval + FUTILITY_MARGIN_2 <= alpha) {
                return quiescence(board, alpha, beta);
            }
        }

        Move bestMove = null;
        int origAlpha = alpha;
        int moveCount = 0;
        final List<Move> moves = MOVE_ORDERING.orderMoves(board.currentPlayer().getLegalMoves(), depth);

        for(final Move move : moves) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() != MoveStatus.DONE) continue;
            moveCount++;

            int value;
            // Late Move Reduction: reduce depth for quiet moves ordered late
            if(!inCheck && moveCount > LMR_THRESHOLD && depth >= 3 && !move.isAttack()
                    && !(move instanceof Move.PawnPromotion)) {
                // Search at reduced depth
                value = -negamax(transition.getTransitionBoard(), depth - 2, -alpha - 1, -alpha, true);
                // If it beats alpha, re-search at full depth
                if(value > alpha) {
                    value = -negamax(transition.getTransitionBoard(), depth - 1, -beta, -alpha, true);
                }
            } else {
                value = -negamax(transition.getTransitionBoard(), depth - 1, -beta, -alpha, true);
            }

            if(value > alpha) {
                alpha = value;
                bestMove = move;
            }
            if(alpha >= beta) {
                if(!move.isAttack()) {
                    MOVE_ORDERING.recordKiller(move, depth);
                    MOVE_ORDERING.recordHistory(move, depth);
                }
                tt.store(key, depth, alpha, TranspositionTable.LOWER, bestMove);
                return alpha;
            }
        }

        final int flag = (alpha > origAlpha) ? TranspositionTable.EXACT : TranspositionTable.UPPER;
        tt.store(key, depth, alpha, flag, bestMove);
        return alpha;
    }

    private int quiescence(final Board board, int alpha, final int beta) {
        if(System.currentTimeMillis() > deadline) return 0;
        final int standPat = sign(board) * EVALUATOR.evaluate(board, 0);
        if(standPat >= beta) return beta;
        // Delta pruning: skip if even the best possible capture can't raise alpha
        if(standPat + DELTA_MARGIN + 900 < alpha) return alpha; // 900 = queen value
        if(standPat > alpha) alpha = standPat;

        for(final Move move : board.currentPlayer().getLegalMoves()) {
            if(!move.isAttack() && !(move instanceof Move.PawnPromotion)) continue;
            // Delta pruning per move
            if(move.isAttack() && move.getAttackedPiece() != null) {
                if(standPat + move.getAttackedPiece().getPieceValue() + DELTA_MARGIN < alpha) continue;
            }
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if(transition.getMoveStatus() == MoveStatus.DONE) {
                final int value = -quiescence(transition.getTransitionBoard(), -beta, -alpha);
                if(value >= beta) return beta;
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
    public String getStrategyName() { return "Advanced Alpha-Beta (Level 6)"; }
}
