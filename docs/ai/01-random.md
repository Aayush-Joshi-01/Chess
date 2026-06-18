# Level 1 — Random Move Strategy

**File:** `src/com/chess/engine/player/ai/RandomMoveStrategy.java`  
**Strength:** ~200 Elo equivalent  
**Search depth:** None (0 plies)  
**Time per move:** < 1 ms  

---

## Overview

The Random strategy makes no attempt to evaluate the position. It simply collects all legal moves available to the current player and picks one uniformly at random. It exists as the baseline difficulty — a player who has never seen chess before.

---

## Algorithm

```
function RandomMove(board):
    moves ← board.currentPlayer().getLegalMoves()
    shuffle(moves)                  // Fisher-Yates shuffle
    return moves[0]
```

No tree search, no evaluation, no lookahead. Every legal move has equal probability `1/N` of being selected, where `N` is the number of legal moves.

---

## Implementation Details

### Move Collection
`board.currentPlayer().getLegalMoves()` returns an `ImmutableList<Move>` built during board construction. This list already includes all special moves (castling, en passant, promotions) because they are added at the `Player` level, not just the piece level.

### Shuffle
`Collections.shuffle(legalMoves)` uses Java's default `Random` (seeded from `System.nanoTime()`), giving each game a different move sequence. The list is copied to an `ArrayList` first since `ImmutableList` doesn't support in-place shuffling.

### Edge Case
If `legalMoves` is empty (checkmate or stalemate), `NULL_MOVE` is returned. The GUI detects this state before ever calling the AI, but the guard is there for safety.

---

## Weaknesses

- Will throw away queens, hang pieces, ignore checks
- Will castle randomly without strategic reason
- Has no concept of material, development, or king safety
- Occasionally plays "brilliant" moves by accident

---

## Why It's Useful

- Useful for teaching absolute beginners who need a non-threatening opponent
- Useful for testing move generation: if Random plays 1000 games without crashing, the legal move generator is likely correct
- Provides a measurable lower bound on AI strength
