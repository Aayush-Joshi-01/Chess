package com.chess.engine.player.ai;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import java.util.*;

/**
 * Simple opening book mapping Zobrist hashes to candidate moves.
 * Covers common openings: e4/d4 systems, Sicilian, French, Caro-Kann, King's Indian, London.
 */
public final class OpeningBook {
    private static final int MAX_BOOK_DEPTH = 20; // half-moves (10 moves)
    private final Map<Long, int[][]> book = new HashMap<>();
    private int halfMoveCount = 0;

    private static final OpeningBook INSTANCE = new OpeningBook();
    public static OpeningBook get() { return INSTANCE; }

    private OpeningBook() {
        // Entries: {from, to} pairs — loaded lazily on first game
        // The book is seeded with positions by playing through standard openings
        // and recording their Zobrist hashes at each step
        seedBook();
    }

    private void seedBook() {
        // We encode opening lines as sequences of coordinate pairs
        // Format: {from, to} where coordinates are 0-63
        // Each line is registered from every position within it

        // e4 (52→36)
        addLine(new int[][]{
            {52, 36},  // 1. e4
            {12, 28},  // 1...e5
            {62, 45},  // 2. Nf3
            {1, 18},   // 2...Nc6
            {61, 34},  // 3. Bb5 — Ruy Lopez
        });
        addLine(new int[][]{
            {52, 36},  // 1. e4
            {12, 28},  // 1...e5
            {62, 45},  // 2. Nf3
            {1, 18},   // 2...Nc6
            {58, 25},  // 3. Bc4 — Italian
        });
        addLine(new int[][]{
            {52, 36},  // 1. e4
            {10, 26},  // 1...c5 — Sicilian
            {62, 45},  // 2. Nf3
            {1, 18},   // 2...Nc6
            {51, 35},  // 3. d4
            {26, 35},  // 3...cxd4
            {45, 35},  // 4. Nxd4
        });
        addLine(new int[][]{
            {52, 36},  // 1. e4
            {12, 20},  // 1...e6 — French Defense
            {51, 35},  // 2. d4
            {11, 27},  // 2...d5
            {57, 40},  // 3. Nc3
        });
        addLine(new int[][]{
            {52, 36},  // 1. e4
            {11, 27},  // 1...d5 — Scandinavian
            {36, 27},  // 2. exd5
            {3, 27},   // 2...Qxd5
        });
        addLine(new int[][]{
            {52, 36},  // 1. e4
            {11, 19},  // 1...c6 — Caro-Kann
            {51, 35},  // 2. d4
            {11, 27},  // 2...d5
            {36, 27},  // 3. exd5
            {19, 27},  // 3...cxd5
        });
        // 1. d4 systems
        addLine(new int[][]{
            {51, 35},  // 1. d4
            {11, 27},  // 1...d5
            {50, 34},  // 2. c4 — Queen's Gambit
            {27, 34},  // 2...dxc4 — QGA
        });
        addLine(new int[][]{
            {51, 35},  // 1. d4
            {11, 27},  // 1...d5
            {50, 34},  // 2. c4
            {12, 20},  // 2...e6 — QGD
            {57, 40},  // 3. Nc3
            {6, 21},   // 3...Nf6
        });
        addLine(new int[][]{
            {51, 35},  // 1. d4
            {6, 21},   // 1...Nf6
            {50, 34},  // 2. c4
            {12, 28},  // 2...g6 — King's Indian
            {57, 40},  // 3. Nc3
            {9, 25},   // 3...Bg7
            {52, 36},  // 4. e4
            {3, 12},   // 4...d6
            {62, 45},  // 5. Nf3
        });
        addLine(new int[][]{
            {51, 35},  // 1. d4
            {6, 21},   // 1...Nf6
            {58, 37},  // 2. Bf4 — London System
            {12, 20},  // 2...e6
            {62, 45},  // 3. Nf3
            {11, 27},  // 3...d5
            {52, 36},  // 4. e3
        });
        // King's Pawn — King's Gambit
        addLine(new int[][]{
            {52, 36},  // 1. e4
            {12, 28},  // 1...e5
            {53, 37},  // 2. f4 — King's Gambit
        });
    }

    private void addLine(final int[][] moves) {
        Board board = Board.createStandardBoard();
        for(final int[] pair : moves) {
            final long hash = ZobristHasher.hash(board);
            book.computeIfAbsent(hash, k -> new int[0][]).getClass(); // ensure entry
            // Store the move pair
            final int[][] existing = book.getOrDefault(hash, new int[0][]);
            final int[][] updated = Arrays.copyOf(existing, existing.length + 1);
            updated[existing.length] = pair;
            book.put(hash, updated);
            // Advance board
            final Move move = Move.MoveFactory.createMove(board, pair[0], pair[1]);
            if(move == Move.NULL_MOVE) break;
            final com.chess.engine.player.MoveTransition t = board.currentPlayer().makeMove(move);
            if(!t.getMoveStatus().isDone()) break;
            board = t.getTransitionBoard();
        }
    }

    public Move lookup(final Board board) {
        if(halfMoveCount >= MAX_BOOK_DEPTH) return null;
        final long hash = ZobristHasher.hash(board);
        final int[][] candidates = book.get(hash);
        if(candidates == null || candidates.length == 0) return null;
        // Random weighted pick
        final int[] choice = candidates[new Random().nextInt(candidates.length)];
        final Move move = Move.MoveFactory.createMove(board, choice[0], choice[1]);
        if(move == Move.NULL_MOVE) return null;
        halfMoveCount++;
        return move;
    }

    public void reset() { halfMoveCount = 0; }
}
