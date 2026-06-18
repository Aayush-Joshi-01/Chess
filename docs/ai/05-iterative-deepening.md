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

### Why Does ID Not Waste Work?

Counter-intuitively, repeating all shallower searches is cheap because:
- A depth-N search does ≈ b^N nodes
- Sum of depths 1 through N-1: b^1 + b^2 + ... + b^(N-1) ≈ b^(N-1) × b/(b-1)
- With b=30: the overhead of all previous depths is only ~3.5% of the final depth

The main benefit of ID is the **move ordering benefit**: the best move found at depth N-1 is searched first at depth N, causing early cutoffs.

---

## 2. Transposition Table

Many chess positions can be reached via different move orders. Without a TT, the engine searches each one from scratch.

### Zobrist Hashing

Each board position is mapped to a 64-bit integer using Zobrist hashing:

```
hash = 0
for each piece on each square:
    hash ^= PIECE_KEYS[square][pieceType]    // XOR with pre-generated random key
if black to move:
    hash ^= SIDE_TO_MOVE_KEY
```

**Why XOR?** XOR is its own inverse, so when a piece moves:
```
hash ^= PIECE_KEYS[fromSquare][piece]   // remove piece from source
hash ^= PIECE_KEYS[toSquare][piece]     // add piece to destination
```
This allows incremental hash updates in O(1) — though our implementation recomputes from scratch since boards are immutable.

**Collision probability:** With a 20-bit index (1M entries) and 64-bit keys, the probability of two different positions sharing a slot is 1/2^64 ≈ negligible.

### Table Structure

Each entry stores:
```
struct Entry {
    long  key;         // full 64-bit Zobrist hash (to detect collisions)
    int   depth;       // how deeply this position was searched
    int   score;       // the score found
    int   flag;        // EXACT | LOWER | UPPER
    int   encodedMove; // best move (from<<6 | to), for move ordering
}
```

**Flag types:**
| Flag | Meaning | When stored |
|---|---|---|
| `EXACT` | Score is exact | `α < score < β` (inside window) |
| `LOWER` | Score is a lower bound (≥ β) | Beta cutoff occurred |
| `UPPER` | Score is an upper bound (≤ α) | All moves failed low |

### TT Lookup Logic

```
entry ← tt.probe(hash)
if entry != null and entry.depth >= currentDepth:
    if entry.flag == EXACT:  return entry.score
    if entry.flag == LOWER:  α = max(α, entry.score)
    if entry.flag == UPPER:  β = min(β, entry.score)
    if α >= β:               return entry.score   // cutoff
```

Only use TT entries searched at least as deeply as the current search — shallower entries give unreliable scores at greater depth.

### Replacement Scheme

This implementation uses **always replace**: new entries overwrite old ones unconditionally. This is simple and works well in practice because deeper searches naturally overwrite shallower ones.

---

## 3. Aspiration Windows

After depth N completes with score S, depth N+1 searches with a narrow window `[S-50, S+50]` (±50 centipawns). If the search falls outside this window, it re-searches with `[-∞, +∞]`.

```
Depth 1: score = 45    (full window)
Depth 2: search [−5, 95]
  → score = 20         (fell below window, fail-low)
  → re-search [−∞, +∞]: score = 25
Depth 3: search [−25, 75]
  → score = 30         (inside window, use it)
```

**Why it helps:** A narrow window causes more beta cutoffs (upper bound), making the search much faster. The cost is an occasional re-search on fail-low/high, but this is rare in practice.

---

## 4. Move Ordering at Level 5

In addition to MVV-LVA, Level 5 adds:

### Killer Moves
When a quiet move causes a beta cutoff at depth D, it's stored as a "killer" for depth D. On the next search at the same depth, killers are tried before other quiet moves.

```
killers[depth][0] = most recent killer
killers[depth][1] = second-most recent killer
```

Killers are position-independent (stored by depth, not by hash), so they work even when the TT doesn't have an entry.

### History Heuristic
When a quiet move causes a beta cutoff, increment its score in a `historyTable[from][to]` by `depth²`. Over the search, moves that frequently cause cutoffs bubble to the top of the ordering.

```
historyTable[from][to] += depth * depth
```

### Combined Ordering Priority
```
1. TT best move (if entry has one)         → tried first
2. Captures (MVV-LVA scored)               → 10000+
3. Promotions                              → 20000
4. Killer moves (2 per depth)              → 9000
5. History heuristic moves                 → 0–8999
6. Remaining quiet moves                   → 0
```

---

## Time Management

The search checks `System.currentTimeMillis() > deadline` at the start of each `negamax` call. If time expires mid-search, it returns 0 (a neutral score). The root discards any partial-depth result and uses the best move from the last fully completed depth. This prevents returning a blunder caused by an incomplete search.
