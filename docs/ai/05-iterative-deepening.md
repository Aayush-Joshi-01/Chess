# Level 5 — Iterative Deepening + Transposition Table

**Files:**
- `src/com/chess/engine/player/ai/IterativeDeepeningStrategy.java`
- `src/com/chess/engine/player/ai/ZobristHasher.java`
- `src/com/chess/engine/player/ai/TranspositionTable.java`

**Strength:** ~1500 Elo equivalent  
**Search:** Iterative deepening, time-limited (2 seconds)  
**Time per move:** Up to 2 000 ms  

---

## Overview

Level 5 adds three major techniques on top of Alpha-Beta:

1. **Iterative Deepening** — search depth 1, then 2, then 3… until time runs out, always keeping the best move from the previous completed depth
2. **Transposition Table (TT)** — a large hash table that stores results from previously searched positions, avoiding redundant work when the same position is reached via different move orders
3. **Aspiration Windows** — narrow the alpha-beta search window around the previous depth's score to get more cutoffs; widen if the score falls outside

---

## Iterative Deepening: The Big Picture

```
┌─────────────────────────────────────────────────────────────────┐
│                  ITERATIVE DEEPENING TIMELINE                   │
└─────────────────────────────────────────────────────────────────┘

  t = 0 ms ──────────────────────────────────────── t = 2000 ms

  ┌───┐  ┌──────┐  ┌────────────┐  ┌──────────────────────┐  ┌╌╌
  │ 1 │  │  2   │  │     3      │  │          4           │  ╎ 5
  └───┘  └──────┘  └────────────┘  └──────────────────────┘  └╌╌
  done   done      done            done           ▲ time limit
  best=e4 best=Nf3  best=Nf3        best=d4        │
                                                   │
                                              Return d4 (last
                                              fully completed depth)

  Depth 5 was IN PROGRESS when time ran out → discarded, not used
  This prevents returning a blunder from an incomplete search.
```

---

## Why Iterative Deepening Doesn't Waste Work

```
  Average branching factor b = 30

  Cumulative nodes searched:
  ┌──────────────────────────────────────────────────────────────┐
  │  Depth 1: b^1  =          30 nodes                          │
  │  Depth 2: b^2  =         900 nodes                          │
  │  Depth 3: b^3  =      27,000 nodes                          │
  │  Depth 4: b^4  =     810,000 nodes   ← bulk of the work     │
  │  ─────────────────────────────────────────────────          │
  │  Total re-work  =     837,930 nodes  (3.5% overhead)        │
  │  Direct depth 4 =     810,000 nodes                         │
  │                                                              │
  │  The overhead of "repeating" depths 1-3 is only 3.5%.       │
  │  The BENEFIT: depth 3's best move is searched first at       │
  │  depth 4, causing early cutoffs that save far more than 3.5% │
  └──────────────────────────────────────────────────────────────┘
```

---

## 1. Iterative Deepening (ID)

### Why Not Just Search Deep Directly?

If you search depth 7 directly you have no result until it finishes — which might take too long. ID solves this by always having a usable answer from the last completed depth.

### Algorithm

```
function IterativeDeepeningMove(board):
    bestMove  ← NULL_MOVE
    bestScore ← 0
    deadline  ← now() + 2000ms

    for depth = 1 to 20:
        if now() > deadline: break

        α ← bestScore - WINDOW        // aspiration window
        β ← bestScore + WINDOW

        result ← rootSearch(board, depth, α, β, deadline)

        if result == null: break       // time expired mid-search

        if result.score ≤ α or result.score ≥ β:
            // Fell outside window — re-search with full window
            result ← rootSearch(board, depth, -∞, +∞, deadline)
            if result == null: break

        bestScore ← result.score
        if result.move != NULL:
            bestMove ← result.move

    return bestMove
```

### Java Implementation

```java
// IterativeDeepeningStrategy.java
public final class IterativeDeepeningStrategy implements MoveStrategy {

    private static final BoardEvaluator   EVALUATOR     = new StandardBoardEvaluator();
    private static final MoveOrdering     MOVE_ORDERING = MoveOrdering.INSTANCE;
    private static final TranspositionTable TT          = new TranspositionTable();
    private static final int WINDOW = 50;        // aspiration window: ±50cp
    private static final long TIME_LIMIT = 2000; // milliseconds

    @Override
    public Move execute(final Board board) {
        MOVE_ORDERING.reset();
        TT.clear();

        Move bestMove  = Move.NULL_MOVE;
        int  bestScore = 0;
        final long deadline = System.currentTimeMillis() + TIME_LIMIT;

        for (int depth = 1; depth <= 20; depth++) {
            if (System.currentTimeMillis() >= deadline) break;

            // Aspiration window: narrow around previous score
            int alpha = bestScore - WINDOW;
            int beta  = bestScore + WINDOW;

            final SearchResult result =
                    rootSearch(board, depth, alpha, beta, deadline);

            if (result == null) break;  // time ran out mid-search

            // If score fell outside the window, re-search with full bounds
            if (result.score <= alpha || result.score >= beta) {
                final SearchResult retry = rootSearch(
                        board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, deadline);
                if (retry == null) break;
                bestScore = retry.score;
                if (retry.move != Move.NULL_MOVE) bestMove = retry.move;
            } else {
                bestScore = result.score;
                if (result.move != Move.NULL_MOVE) bestMove = result.move;
            }
        }
        return bestMove;
    }

    private SearchResult rootSearch(final Board board, final int depth,
                                    int alpha, final int beta,
                                    final long deadline) {
        Move bestMove  = Move.NULL_MOVE;
        int  bestScore = Integer.MIN_VALUE;

        for (final Move move : MOVE_ORDERING.orderMoves(
                board.currentPlayer().getLegalMoves(), board, depth)) {

            if (System.currentTimeMillis() >= deadline) return null;

            final MoveTransition t = board.currentPlayer().makeMove(move);
            if (!t.getMoveStatus().isDone()) continue;

            final long hash  = ZobristHasher.hash(t.getTransitionBoard());
            final int  score = -negamax(t.getTransitionBoard(),
                                        depth - 1, -beta, -alpha,
                                        deadline, hash);

            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
                alpha = Math.max(alpha, score);
            }
        }
        return new SearchResult(bestMove, bestScore);
    }

    private int negamax(final Board board, final int depth,
                        int alpha, final int beta,
                        final long deadline, final long hash) {

        if (System.currentTimeMillis() >= deadline) return 0;

        // TT probe
        final TranspositionTable.Entry entry = TT.probe(hash);
        if (entry != null && entry.depth >= depth) {
            if (entry.flag == TranspositionTable.EXACT) return entry.score;
            if (entry.flag == TranspositionTable.LOWER) alpha = Math.max(alpha, entry.score);
            if (entry.flag == TranspositionTable.UPPER) beta  = Math.min(beta,  entry.score);
            if (alpha >= beta) return entry.score;
        }

        if (depth == 0 || isEndGame(board)) {
            return sign(board) * EVALUATOR.evaluate(board, depth);
        }

        int  best = Integer.MIN_VALUE;
        int  flag = TranspositionTable.UPPER;

        for (final Move move : MOVE_ORDERING.orderMoves(
                board.currentPlayer().getLegalMoves(), board, depth)) {

            final MoveTransition t = board.currentPlayer().makeMove(move);
            if (!t.getMoveStatus().isDone()) continue;

            final long childHash = ZobristHasher.hash(t.getTransitionBoard());
            final int  score = -negamax(t.getTransitionBoard(),
                                        depth - 1, -beta, -alpha,
                                        deadline, childHash);

            if (score >= beta) {
                TT.store(hash, depth, beta, TranspositionTable.LOWER,
                         move.getCurrentCoordinate() << 6 | move.getDestinationCoordinate());
                MOVE_ORDERING.recordKiller(move, depth);
                return beta;
            }
            if (score > best) {
                best = score;
                flag = (score > alpha) ? TranspositionTable.EXACT : TranspositionTable.UPPER;
                if (score > alpha) {
                    alpha = score;
                    MOVE_ORDERING.recordHistory(move, depth);
                }
            }
        }

        TT.store(hash, depth, best, flag, 0);
        return best;
    }

    private record SearchResult(Move move, int score) {}
    private static boolean isEndGame(Board b) {
        return b.currentPlayer().isInCheckMate() || b.currentPlayer().isInStaleMate();
    }
    private static int sign(Board b) {
        return b.currentPlayer().getAlliance().isWhite() ? 1 : -1;
    }
    @Override public String getStrategyName() { return "IterativeDeepening"; }
}
```

---

## Sequence Diagram: Full Iterative Deepening Flow

```
  execute()         rootSearch()         negamax()       TT          ZobristHasher
      │                   │                  │            │                │
      │  depth=1          │                  │            │                │
      │──────────────────►│                  │            │                │
      │                   │── makeMove() ───►│            │                │
      │                   │── hash(board) ────────────────────────────────►│
      │                   │◄── long hash ──────────────────────────────────│
      │                   │── negamax(d=0) ──►│            │               │
      │                   │                  │── probe() ─►│               │
      │                   │                  │◄── null ────│ (cold cache)  │
      │                   │                  │── evaluate()→ score         │
      │                   │                  │── store() ──►│               │
      │                   │◄── score ─────────│            │               │
      │◄── SearchResult ──│                  │            │               │
      │                   │                  │            │               │
      │  depth=2          │                  │            │               │
      │──────────────────►│                  │            │               │
      │                   │── negamax(d=1) ──►│            │               │
      │                   │                  │── probe() ─►│               │
      │                   │                  │◄── Entry ───│ (TT hit!)     │
      │                   │                  │  flag=EXACT → return score  │
      │                   │◄── score (fast) ──│            │               │
      │◄── SearchResult ──│                  │            │               │
      │   (faster, TT hit)│                  │            │               │
      │                   │                  │            │               │
      │  ... depth=3,4,5 until deadline ...  │            │               │
      │── return bestMove from last complete depth         │               │
```

---

## 2. Transposition Table

### The Transposition Problem

```
  Position X can be reached by different move orders:

  1.e4 d5  2.Nf3  →  Position X
  1.Nf3 d5 2.e4   →  Position X   (same position!)

  Without TT:  search Position X twice from scratch
  With TT:     look up Position X → reuse the cached score
```

### Zobrist Hashing

Each board position is mapped to a 64-bit integer using Zobrist hashing:

```
  Initialisation (once, at startup):
  ┌─────────────────────────────────────────────────────────┐
  │  For each of 64 squares × 12 piece types = 768 keys:    │
  │  PIECE_KEYS[square][pieceType] = random 64-bit integer  │
  │                                                         │
  │  SIDE_KEY = another random 64-bit integer               │
  └─────────────────────────────────────────────────────────┘

  Computing a hash:
  ┌─────────────────────────────────────────────────────────┐
  │  hash = 0                                               │
  │  for each piece on each square:                         │
  │      hash ^= PIECE_KEYS[square][pieceType]              │
  │  if black to move:                                      │
  │      hash ^= SIDE_KEY                                   │
  └─────────────────────────────────────────────────────────┘

  XOR properties that make this work:
  ┌─────────────────────────────────────────────────────────┐
  │  A ^ A = 0      (XOR with itself cancels)               │
  │  A ^ 0 = A      (XOR with 0 is identity)               │
  │  A ^ B ^ A = B  (XOR is its own inverse)                │
  │                                                         │
  │  To UPDATE the hash when a piece moves:                 │
  │  hash ^= PIECE_KEYS[fromSquare][piece]  // remove       │
  │  hash ^= PIECE_KEYS[toSquare][piece]    // add          │
  └─────────────────────────────────────────────────────────┘
```

### Zobrist Java Implementation

```java
// ZobristHasher.java
public final class ZobristHasher {

    // 64 squares × 12 piece types (6 white + 6 black)
    private static final long[][] PIECE_KEYS = new long[64][12];
    private static final long     SIDE_KEY;

    static {
        // Fixed seed for reproducibility across runs
        final Random rng = new Random(0xDEADBEEF_CAFEBABEL);
        for (int sq = 0; sq < 64; sq++)
            for (int pt = 0; pt < 12; pt++)
                PIECE_KEYS[sq][pt] = rng.nextLong();
        SIDE_KEY = rng.nextLong();
    }

    public static long hash(final Board board) {
        long h = 0L;
        for (int sq = 0; sq < 64; sq++) {
            final Tile tile = board.getTile(sq);
            if (tile.isTileOccupied()) {
                final Piece p  = tile.getPiece();
                // pieceIndex: 0-5 white pieces, 6-11 black pieces
                final int   pi = p.getPieceType().ordinal()
                               + (p.getPieceAlliance().isBlack() ? 6 : 0);
                h ^= PIECE_KEYS[sq][pi];
            }
        }
        if (board.currentPlayer().getAlliance().isBlack()) {
            h ^= SIDE_KEY;
        }
        return h;
    }
}
```

### XOR Hashing Visualised

```
  Board has: White Pawn on e2 (sq=52), White King on e1 (sq=60)

  Step 1: h = 0
  Step 2: h ^= PIECE_KEYS[52][0]   // sq=52, pawn=type0 white
          h = 0xA3F2...
  Step 3: h ^= PIECE_KEYS[60][5]   // sq=60, king=type5 white
          h = 0xA3F2... ^ 0x7E91... = 0x1C63...

  Pawn moves e2→e4:
  Step 4: h ^= PIECE_KEYS[52][0]   // remove from e2 → cancels step 2
          h = 0x7E91...
  Step 5: h ^= PIECE_KEYS[36][0]   // add to e4
          h = (new hash for new position)

  XOR self-inverse means piece removal = XOR the same key again.
```

### Table Structure

Each entry stores:
```
  ┌───────────────────────────────────────────────────────┐
  │  struct Entry {                                       │
  │      long  key;         // full 64-bit hash           │
  │      int   depth;       // search depth stored        │
  │      int   score;       // result score               │
  │      int   flag;        // EXACT | LOWER | UPPER      │
  │      int   encodedMove; // best move (from<<6 | to)   │
  │  }                                                    │
  │                                                       │
  │  Size: 1,048,576 entries (1M, power of 2)             │
  │  Index: hash & (SIZE - 1)   (fast modulo via AND)     │
  └───────────────────────────────────────────────────────┘
```

### TranspositionTable Java Implementation

```java
// TranspositionTable.java
public final class TranspositionTable {

    public static final int EXACT = 0, LOWER = 1, UPPER = 2;
    private static final int SIZE = 1 << 20;  // 1,048,576 entries

    // Parallel arrays instead of Entry objects — better cache locality
    private final long[] keys   = new long[SIZE];
    private final int[]  depths = new int[SIZE];
    private final int[]  scores = new int[SIZE];
    private final int[]  flags  = new int[SIZE];
    private final int[]  moves  = new int[SIZE];

    public void store(long key, int depth, int score, int flag, int move) {
        final int idx = (int)(key & (SIZE - 1));
        // Always-replace scheme: new entry overwrites old unconditionally
        keys[idx]   = key;
        depths[idx] = depth;
        scores[idx] = score;
        flags[idx]  = flag;
        moves[idx]  = move;
    }

    public Entry probe(final long key) {
        final int idx = (int)(key & (SIZE - 1));
        // Verify full key to detect hash collisions
        if (keys[idx] != key) return null;
        return new Entry(depths[idx], scores[idx], flags[idx], moves[idx]);
    }

    public record Entry(int depth, int score, int flag, int encodedMove) {}
}
```

### TT Flag Types

```
  ┌──────────────────────────────────────────────────────────────┐
  │  Flag   │ Meaning            │ When stored                   │
  │  ───────┼────────────────────┼─────────────────────────────  │
  │  EXACT  │ Score is exact     │ α < score < β (full window)   │
  │  LOWER  │ Lower bound (≥ β)  │ Beta cutoff — score too high  │
  │  UPPER  │ Upper bound (≤ α)  │ All moves failed low          │
  └──────────────────────────────────────────────────────────────┘

  How they're used on probe:
  ┌──────────────────────────────────────────────────────────────┐
  │  if EXACT  → return entry.score directly                     │
  │  if LOWER  → α = max(α, entry.score)                         │
  │  if UPPER  → β = min(β, entry.score)                         │
  │  if α ≥ β  → cutoff (score is bounded out of range)          │
  └──────────────────────────────────────────────────────────────┘
```

### TT Decision Tree on Probe

```
  TT.probe(hash):
      │
      ├── key mismatch → return null (cold miss or collision)
      │
      └── key matches:
              │
              ├── entry.depth < currentDepth → ignore (shallower = unreliable)
              │
              └── entry.depth >= currentDepth:
                      │
                      ├── flag == EXACT   → return score directly ✓
                      │
                      ├── flag == LOWER   → alpha = max(alpha, score)
                      │                    if alpha >= beta → cutoff
                      │
                      └── flag == UPPER   → beta = min(beta, score)
                                           if alpha >= beta → cutoff
```

---

## 3. Aspiration Windows

```
  Without aspiration windows:
  Every depth searches with α=-∞, β=+∞ → many nodes explored

  With aspiration windows (WINDOW = 50 centipawns):

  Depth 1: score = +45  (searched full window [-∞, +∞])
           │
           ▼
  Depth 2: α = 45-50 = -5,  β = 45+50 = 95
           Window: [-5, 95]
           score = +30  → inside window ✓   use it
           │
           ▼
  Depth 3: α = 30-50 = -20, β = 30+50 = 80
           Window: [-20, 80]
           score = +10  → inside window ✓   use it
           │
           ▼
  Depth 4: α = 10-50 = -40, β = 10+50 = 60
           Window: [-40, 60]
           score = +70  → OUTSIDE β=60 (fail-high)
           Re-search with [-∞, +∞]: score = +70  use it

  Narrow window → more beta cutoffs → faster search.
  Cost of a miss: one extra full-window re-search (rare).
```

### Aspiration Window Sequence Diagram

```
  execute()                 rootSearch()
      │                          │
      │  depth=1, α=-∞, β=+∞    │
      │─────────────────────────►│── returns score=45
      │◄─ SearchResult(45) ──────│
      │  bestScore = 45          │
      │                          │
      │  depth=2, α=-5, β=95     │  ← aspiration window around 45
      │─────────────────────────►│── returns score=30
      │◄─ SearchResult(30) ──────│  ← 30 inside [-5,95] ✓
      │  bestScore = 30          │
      │                          │
      │  depth=3, α=-20, β=80    │
      │─────────────────────────►│── returns score=70
      │◄─ SearchResult(70) ──────│  ← 70 > β=80? NO ← 70 < 80, inside ✓
      │  bestScore = 70          │
      │                          │
      │  depth=4, α=20, β=120    │
      │─────────────────────────►│── returns score=110
      │◄─ SearchResult(110) ─────│  ← 110 < 120? YES ← inside ✓
      │                          │
      │  [time limit reached]    │
      │── return bestMove ───────│
```

---

## 4. Move Ordering at Level 5

```
  Priority │ Source                  │ Score assigned
  ─────────┼─────────────────────────┼────────────────────────
     1      │ TT best move (if any)   │ searched first (no score)
     2      │ Promotions              │ 20000
     3      │ Captures (MVV-LVA)      │ 10000 + victim - atk/100
     4      │ Killer moves (2/depth)  │ 9000
     5      │ History heuristic       │ 0 – 8999
     6      │ Other quiet moves       │ 0
```

### Killer Moves
```
  killers[depth][0] = most recent quiet move that caused β-cutoff at depth D
  killers[depth][1] = second-most recent

  At depth 4:  killer might be Nf3-g5 (a strong attacking knight move)
  Next time we search at depth 4:  try Nf3-g5 right after captures
```

```java
// MoveOrdering.java — killer storage
public void recordKiller(final Move move, final int depth) {
    if (!move.isAttack()) {   // killers are quiet moves only
        KILLER_MOVES[depth][1] = KILLER_MOVES[depth][0];
        KILLER_MOVES[depth][0] = move;
    }
}
```

### History Heuristic
```
  After each quiet β-cutoff:
    historyTable[from][to] += depth * depth

  Over thousands of nodes, moves that frequently cause cutoffs
  accumulate high scores and bubble to the top of ordering.
```

```java
// MoveOrdering.java — history update
public void recordHistory(final Move move, final int depth) {
    if (!move.isAttack()) {
        HISTORY_TABLE[move.getCurrentCoordinate()]
                     [move.getDestinationCoordinate()] += depth * depth;
    }
}

// reset() — called once per execute() to prevent stale data
public void reset() {
    for (int d = 0; d < 64; d++) {
        KILLER_MOVES[d][0] = null;
        KILLER_MOVES[d][1] = null;
    }
    for (int[] row : HISTORY_TABLE) Arrays.fill(row, 0);
}
```

---

## Time Management

```
  ┌──────────────────────────────────────────────────────────────┐
  │  Time check at EVERY negamax call:                           │
  │      if System.currentTimeMillis() > deadline: return 0      │
  │                                                              │
  │  Root behavior:                                              │
  │      if depth N returns null (timed out mid-search):         │
  │          discard depth N result                              │
  │          return bestMove from depth N-1                      │
  │                                                              │
  │  This prevents:                                              │
  │      ✗ Returning a blunder from depth N (partial result)     │
  │      ✓ Always returning the best fully-searched move         │
  └──────────────────────────────────────────────────────────────┘
```

### Time Management Sequence

```
  execute()              rootSearch(depth=5)        negamax(depth=4)
      │                         │                          │
      │ t=1800ms, depth=5       │                          │
      │────────────────────────►│                          │
      │                         │── negamax() ────────────►│
      │                         │                          │  t=2001ms
      │                         │                          │  deadline exceeded!
      │                         │                          │── return 0
      │                         │◄─ 0 (timed out) ─────────│
      │                         │   result is unreliable   │
      │◄─ null ─────────────────│  rootSearch returns null  │
      │                         │                          │
      │  null received:         │                          │
      │  discard depth=5 result │                          │
      │  return bestMove from depth=4 (last complete)      │
```

---

## Level 5 vs Level 4 Summary

```
  ┌────────────────────────────────────────────────────────────────┐
  │               │  AlphaBeta (L4)    │  IterDeep (L5)           │
  │  ─────────────┼────────────────────┼─────────────────────────  │
  │  Max depth    │  fixed 4           │  dynamic 6–8             │
  │  Time limit   │  none              │  2 seconds               │
  │  TT           │  none              │  1M entries Zobrist      │
  │  Aspiration   │  none              │  ±50cp window            │
  │  Killers      │  none              │  2 per depth             │
  │  History      │  none              │  64×64 table             │
  │  Strength     │  ~1100 Elo         │  ~1500 Elo               │
  └────────────────────────────────────────────────────────────────┘
```

---

## Progression to Level 6

```
  What Level 5 is missing that Advanced AlphaBeta adds:
  ┌──────────────────────────────────────────────────────────────┐
  │                                                              │
  │  Level 5: searches all moves at full depth (with ordering)  │
  │           stops at depth N even if captures remain          │
  │                                                              │
  │  Level 6 adds:                                              │
  │    • Null-move pruning: skip branches where even passing     │
  │      your turn beats beta                                    │
  │    • LMR: reduce depth for late-ordered quiet moves         │
  │    • Quiescence: extend past depth N for captures           │
  │    • Futility pruning: skip nodes that can't raise alpha    │
  │                                                              │
  │  Result: depth 8–12 effective vs 6–8                        │
  │                                                              │
  └──────────────────────────────────────────────────────────────┘
```
