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

### History Heuristic
```
  After each quiet β-cutoff:
    historyTable[from][to] += depth * depth

  Over thousands of nodes, moves that frequently cause cutoffs
  accumulate high scores and bubble to the top of ordering.
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
