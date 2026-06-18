package com.chess.engine.player.ai;
import com.chess.engine.board.Move;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class MoveOrdering {
    private static final int[][] KILLER_MOVES = new int[64][2];
    private static final int[][] HISTORY_TABLE = new int[64][64];
    private static final MoveOrdering INSTANCE = new MoveOrdering();

    private MoveOrdering() {}
    public static MoveOrdering get() { return INSTANCE; }

    public void reset() {
        for(int d = 0; d < 64; d++) {
            KILLER_MOVES[d][0] = -1;
            KILLER_MOVES[d][1] = -1;
        }
        for(int[] row : HISTORY_TABLE) java.util.Arrays.fill(row, 0);
    }

    public void recordKiller(final Move move, final int depth) {
        if(depth < 64) {
            KILLER_MOVES[depth][1] = KILLER_MOVES[depth][0];
            KILLER_MOVES[depth][0] = move.hashCode();
        }
    }

    public void recordHistory(final Move move, final int depth) {
        if(move.getCurrentCoordinate() >= 0 && move.getDestinationCoordinate() >= 0) {
            HISTORY_TABLE[move.getCurrentCoordinate()][move.getDestinationCoordinate()] += depth * depth;
        }
    }

    public List<Move> orderMoves(final Collection<Move> moves, final int depth) {
        final List<Move> ordered = new ArrayList<>(moves);
        ordered.sort(Comparator.comparingInt((Move m) -> -scoreMove(m, depth)));
        return ordered;
    }

    private int scoreMove(final Move move, final int depth) {
        if(move instanceof Move.PawnPromotion) return 20000;
        if(move.isAttack()) {
            // MVV-LVA: value of victim minus attacker
            final int victimValue  = move.getAttackedPiece() != null ? move.getAttackedPiece().getPieceValue() : 0;
            final int attackerValue = move.getMovedPiece().getPieceValue();
            return 10000 + victimValue - attackerValue / 100;
        }
        // Killer move bonus
        if(depth < 64) {
            final int hash = move.hashCode();
            if(KILLER_MOVES[depth][0] == hash || KILLER_MOVES[depth][1] == hash) return 9000;
        }
        // History heuristic
        if(move.getCurrentCoordinate() >= 0 && move.getDestinationCoordinate() >= 0) {
            return HISTORY_TABLE[move.getCurrentCoordinate()][move.getDestinationCoordinate()];
        }
        return 0;
    }
}
