# Level 6 — Advanced Alpha-Beta (Master Engine)

**File:** `src/com/chess/engine/player/ai/AdvancedAlphaBetaStrategy.java`

**Strength:** ~1900+ Elo equivalent  
**Search:** Iterative deepening, time-limited (3 seconds)  
**Time per move:** Up to 3 000 ms  

---

## Overview

Level 6 builds on everything in Level 5 and adds four more techniques used by serious chess engines:

| Technique | Purpose |
|---|---|
| **Null-Move Pruning** | Skip branches where even passing a move doesn't help |
| **Late Move Reduction (LMR)** | Search "bad" moves at reduced depth |
| **Quiescence Search** | Extend search past leaf nodes for captures to avoid the horizon effect |
| **Futility / Delta Pruning** | Skip moves that statically cannot raise alpha |

---

## 1. Null-Move Pruning

### Concept

In most chess positions, having the right to move is an advantage. If the current player **passes their turn** (makes a null move) and the opponent's best reply still fails to beat beta, then a real move will certainly fail too — so we prune the entire subtree.

### Algorithm

```
if nullMoveAllowed and depth >= R+1 and not inCheck:
    // Approximate null move: static eval at reduced depth
    nullScore ← sign × evaluate(board, depth - R - 1)
    if nullScore ≥ β:
        return β    // prune — opponent can't save this branch even with a free move
```

**R (reduction):** We use R=2. So a depth-6 search evaluates the null move at depth 3.

### Why Not in Zugzwang?

Null-move pruning is **unsound** in zugzwang positions (where every move worsens your position). The guard `not inCheck` handles the most obvious case. A full engine would also detect pawn-only endgames where zugzwang is likely — this implementation omits that refinement for simplicity.

### Recursion Flag
```java
negamax(board, depth-1, -beta, -alpha, /*nullMoveAllowed=*/true)
```
After a null move, we don't allow another null move immediately to prevent "double null" which would be unsound.

---

## 2. Late Move Reduction (LMR)

### Concept

Move ordering places the most promising moves first. Moves ordered late in the list (move #5, #6, etc.) are statistically unlikely to be the best move. LMR searches them at reduced depth, saving time. If the reduced search beats alpha (suggesting the move might actually be good), we re-search at full depth.

### Algorithm

```
moveCount ← 0
for each move in orderedMoves:
    moveCount++
    if not inCheck
       and moveCount > LMR_THRESHOLD (4)
       and depth >= 3
       and not capture
       and not promotion:

        // Reduced-depth search (scout)
        score ← -negamax(child, depth-2, -α-1, -α)

        if score > α:
            // Move looks good — re-search at full depth
            score ← -negamax(child, depth-1, -β, -α)
    else:
        score ← -negamax(child, depth-1, -β, -α)
```

### Why `-α-1` in the Scout Search?

The scout window `[-α-1, -α]` is a **null window** (width 1). It tests whether the move can beat α without doing a full search. If `score > α` (the move beats the window), we know it's potentially the best move and re-search at full depth to get the exact score.

### Savings

In a position with 30 legal moves, after 4 are searched normally, the remaining 26 are reduced from depth D to depth D-2. This cuts their subtree size by ~b² ≈ 900×, making LMR one of the most impactful optimisations in practice.

---

## 3. Quiescence Search

### The Horizon Effect

At depth 3, the engine might see:
```
White captures a queen → score +900 (looks great!)
```
But it doesn't see:
```
Black recaptures with a pawn → actually net 0
Black delivers checkmate next move → score -∞
```

The position at the leaf isn't "quiet" — there are still captures available that will dramatically change the evaluation. The horizon effect causes the engine to overvalue positions where it has just made a capture.

### Algorithm

```
function quiescence(board, α, β):
    standPat ← sign × evaluate(board)

    if standPat ≥ β: return β         // fail-hard cutoff
    if standPat > α: α ← standPat    // improve lower bound with static eval

    for each capture (or promotion) in board.currentPlayer().getLegalMoves():
        // Delta pruning (see below)
        if standPat + capturedPieceValue + DELTA_MARGIN < α: continue

        transition ← makeMove(capture)
        if transition.isDone():
            score ← -quiescence(transition.board, -β, -α)
            if score ≥ β: return β
            if score > α: α ← score

    return α
```

### Stand-Pat

The `standPat` evaluation represents "if we stop searching here, the current player can choose **not** to capture." This is the key insight: in chess you're not forced to capture (unlike some other games). If the static evaluation already beats beta, we prune.

### Delta Pruning (inside quiescence)

```
if standPat + capturedPieceValue + DELTA_MARGIN (200) < α:
    skip this capture
```

If even taking the opponent's piece (gaining `capturedPieceValue`) plus a generous margin (200cp) cannot raise alpha, there's no point searching it. This prunes clearly hopeless captures.

### Quiescence Depth

Quiescence has no fixed depth limit in this implementation — it searches until no captures remain (a "quiet" position). In practice, most positions become quiet within 4–6 extra plies, and the exponential cost is manageable because branching factor in quiescence (captures only) is much smaller than the full branching factor (~5–8 vs ~30).

---

## 4. Futility Pruning

Near the leaf nodes, if the static evaluation plus a margin cannot raise alpha, the move is futile:

```
// One ply from horizon
if not inCheck and depth == 1:
    staticEval ← sign × evaluate(board)
    if staticEval + FUTILITY_MARGIN_1 (200) ≤ α:
        return quiescence(board, α, β)  // skip regular search, go straight to qsearch

// Two plies from horizon
if not inCheck and depth == 2:
    staticEval ← sign × evaluate(board)
    if staticEval + FUTILITY_MARGIN_2 (400) ≤ α:
        return quiescence(board, α, β)
```

The margins (200cp at depth 1, 400cp at depth 2) account for the maximum material gain possible in one/two moves. If even that can't reach alpha, all quiet moves at this node are futile.

### Why Fall Through to Quiescence?

Rather than returning alpha directly, we fall through to quiescence search. This correctly handles cases where a capture at the frontier can still improve the score (otherwise we'd miss important material-winning moves).

---

## 5. Combined Evaluation Stack

At a leaf node (depth=0), the search chain is:

```
negamax(depth=0) → quiescence(board, α, β)
  quiescence searches captures only until no more captures
  each leaf of quiescence → evaluate(board, 0)
```

This means the effective depth for tactical positions is much greater than the nominal depth N, while the cost for quiet positions is just the static evaluation at depth N.

---

## 6. Full Move Ordering Priority

```
Priority 1: TT best move (if available)          → searched first, no score assigned
Priority 2: Promotions                            → score 20000
Priority 3: Captures (MVV-LVA)                    → score 10000 + victimVal - attackerVal/100
Priority 4: Killers (2 per depth)                 → score 9000
Priority 5: History heuristic                     → score 0–8999
Priority 6: Remaining quiet moves                 → score 0
```

After a beta cutoff on a quiet move:
- Record as a killer for this depth
- Increment its history table entry by `depth²`

---

## Performance vs Lower Levels

| Level | Depth | Nodes (approx.) | Techniques |
|---|---|---|---|
| 3 MiniMax | 3 | 27,000 | None |
| 4 AlphaBeta | 4 | 10,000–40,000 | AB pruning, MVV-LVA |
| 5 ID+TT | ~6–8 | 50,000–200,000 | + TT, killers, history, aspiration |
| 6 Advanced | ~8–12 | 100,000–500,000 | + null-move, LMR, quiescence, futility |

Level 6 searches more nodes but reaches effectively much greater depth due to quiescence, making it significantly stronger in tactical positions despite the broader node count.

---

## Limitations

- **No endgame tablebases** — cannot play perfect endgames
- **Approximate null-move** — uses static eval rather than a true pass-move (the board is immutable, making a real null move expensive to implement)
- **No singular extension** — doesn't identify and extend moves that are clearly best
- **No pondering** — doesn't think during the opponent's turn
