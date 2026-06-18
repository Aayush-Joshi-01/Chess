package com.chess.engine.player.ai;
import com.chess.engine.board.Move;

/** Fixed-size transposition table using Zobrist hashes. */
public final class TranspositionTable {
    public static final int EXACT = 0;
    public static final int LOWER = 1; // alpha (lower bound)
    public static final int UPPER = 2; // beta  (upper bound)

    private static final int SIZE = 1 << 20; // ~1M entries
    private static final int MASK = SIZE - 1;

    private final long[] keys   = new long[SIZE];
    private final int[]  depths = new int[SIZE];
    private final int[]  scores = new int[SIZE];
    private final int[]  flags  = new int[SIZE];
    private final int[]  moves  = new int[SIZE]; // encoded as from<<6|to

    public void store(final long key, final int depth, final int score, final int flag, final Move best) {
        final int idx = (int)(key & MASK);
        // Always replace (simple replacement scheme)
        keys[idx]   = key;
        depths[idx] = depth;
        scores[idx] = score;
        flags[idx]  = flag;
        moves[idx]  = best == null ? -1 : (best.getCurrentCoordinate() << 6 | best.getDestinationCoordinate());
    }

    public Entry probe(final long key) {
        final int idx = (int)(key & MASK);
        if(keys[idx] != key) return null;
        return new Entry(depths[idx], scores[idx], flags[idx], moves[idx]);
    }

    public record Entry(int depth, int score, int flag, int encodedMove) {}
}
