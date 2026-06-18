# Level 6 — Advanced Alpha-Beta (Master Engine)

**File:** `src/com/chess/engine/player/ai/AdvancedAlphaBetaStrategy.java`

**Strength:** ~1900+ Elo equivalent  
**Search:** Iterative deepening, time-limited (3 seconds)  
**Time per move:** Up to 3 000 ms  

---

## Overview

Level 6 builds on everything in Level 5 and adds four more techniques used by serious chess engines:

```
  ┌────────────────────────────────────────────────────────────────┐
  │              LEVEL 6 TECHNIQUE STACK                           │
  ├────────────────────────────────────────────────────────────────┤
  │  ✓ Iterative Deepening    (from Level 5)                       │
  │  ✓ Transposition Table    (from Level 5)                       │
  │  ✓ Aspiration Windows     (from Level 5)                       │
  │  ✓ Killers + History      (from Level 5)                       │
  │  ══════════════════════════════════════════════════════════    │
  │  ★ Null-Move Pruning     ← skips branches where passing fails  │
  │  ★ Late Move Reduction   ← reduces depth for weak-looking moves│
  │  ★ Quiescence Search     ← extends past leaf to avoid horizon  │
  │  ★ Futility + Delta Pruning ← skips moves that can't raise α   │
  └────────────────────────────────────────────────────────────────┘
```

---

## 1. Null-Move Pruning

### Concept

```
  Normal chess:  having the right to move = advantage
  Null move:     you give up your turn (illegal in real chess)

  If you PASS your turn and the opponent STILL can't beat beta,
  then your real moves will certainly fail too → prune the branch.

  ┌───────────────────────────────────────────────────────────────┐
  │  Position: White has a very strong position, score >> β        │
  │                                                               │
  │  Normal:   White plays a move → likely still >> β             │
  │  Null:     White passes turn → Black responds → still ≥ β     │
  │            ► White's real moves will also cause β cutoff       │
  │            ► Prune WITHOUT searching them                      │
  └───────────────────────────────────────────────────────────────┘
```

### Algorithm

```
if nullMoveAllowed and depth >= R+1 and not inCheck:
    // Approximate null move: static eval at reduced depth
    nullScore ← sign × evaluate(board, depth - R - 1)
    if nullScore ≥ β:
        return β    // prune — opponent can't save this branch

  R = 2  (reduction factor)
  depth 6 → null move evaluated at depth 3 (6 - 2 - 1 = 3)
```

### When Null-Move CANNOT Be Applied

```
  ┌──────────────────────────────────────────────────────────────┐
  │  Guard: not inCheck                                          │
  │         not after a previous null move (no "double null")    │
  │         depth >= R+1  (need room for the reduced search)     │
  │                                                              │
  │  Danger: Zugzwang positions (where every move loses)         │
  │          In zugzwang, passing = advantage, but a real move   │
  │          = disadvantage. Null-move would wrongly prune.      │
  │          (Rare in practice; mainly pawn endgames)            │
  └──────────────────────────────────────────────────────────────┘
```

---

## 2. Late Move Reduction (LMR)

### Concept

```
  Move ordering puts the BEST moves first.
  Moves searched late in the list are statistically unlikely to be best.

  ┌────────────────────────────────────────────────────────────┐
  │   Move list (ordered):                                     │
  │                                                            │
  │   #1  Qxf7+   ← capture, try at full depth (likely good)  │
  │   #2  Nf3-g5  ← killer move, try at full depth            │
  │   #3  e4-e5   ← high history score, try at full depth     │
  │   #4  a2-a3   ← quiet, move #4+, try at REDUCED depth     │
  │   #5  h2-h3   ← quiet, move #5+, try at REDUCED depth     │
  │   ...                                                      │
  │   If reduced search beats α → re-search at full depth      │
  └────────────────────────────────────────────────────────────┘
```

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

        // Scout search at depth-2 (reduced)
        score ← -negamax(child, depth-2, -α-1, -α)

        if score > α:
            // Looks promising — re-search at full depth
            score ← -negamax(child, depth-1, -β, -α)
    else:
        score ← -negamax(child, depth-1, -β, -α)
```

### The Null Window `-α-1` Explained

```
  Scout window: [-α-1, -α]  (width = 1, a "null window")

  ┌───────────────────────────────────────────────────────────────┐
  │  Purpose: test if score > α WITHOUT doing a full search        │
  │                                                               │
  │  If score ≤ α:  move failed to beat current best → skip it   │
  │  If score > α:  move might be good → re-search with full [β]  │
  │                                                               │
  │  Cost: 1 cheap reduced-depth search instead of a full one.   │
  │  Savings: 26 of 30 moves reduced from depth D to depth D-2.  │
  │           Each subtree is b² ≈ 900× smaller.                  │
  └───────────────────────────────────────────────────────────────┘
```

---

## 3. Quiescence Search

### The Horizon Effect

```
  Normal search stops at depth N. But the position at the leaf
  might not be "quiet" — captures are still available.

  Engine sees at depth N:
  ┌─────────────────────────────────────────────────────────────┐
  │  Depth 3: White captures Black Queen → score = +900 ✓       │
  │           ▲ SEARCH STOPS HERE                               │
  └─────────────────────────────────────────────────────────────┘

  What actually happens next (not seen):
  ┌─────────────────────────────────────────────────────────────┐
  │  Depth 4: Black recaptures with pawn → net score = 0        │
  │  Depth 5: Black delivers checkmate → score = -∞ !!!         │
  └─────────────────────────────────────────────────────────────┘

  This is the "horizon effect" — a disaster just past the search
  horizon looks like a win.
```

### Quiescence Algorithm

```
function quiescence(board, α, β):
    standPat ← sign × evaluate(board)
    │
    ├── if standPat ≥ β:  return β          // stand-pat cutoff
    ├── if standPat > α:  α ← standPat      // improve lower bound
    │
    for each CAPTURE (or promotion) in legalMoves:
        │
        ├── Delta pruning:
        │   if standPat + capturedValue + 200 < α: skip  (hopeless)
        │
        └── transition ← makeMove(capture)
            score ← -quiescence(transition.board, -β, -α)
            if score ≥ β: return β
            if score > α: α ← score

    return α   // score of the quietest reachable position
```

### Stand-Pat Explained

```
  ┌───────────────────────────────────────────────────────────────┐
  │  In chess you are NOT forced to capture.                      │
  │                                                               │
  │  standPat = static eval of current position                   │
  │           = "what if we stop here and take no captures?"      │
  │                                                               │
  │  If standPat ≥ β: the position is already good enough,        │
  │                   no need to search captures (beta cutoff).   │
  │                                                               │
  │  If standPat > α: update lower bound — we can at least        │
  │                   achieve the static eval by not capturing.   │
  └───────────────────────────────────────────────────────────────┘
```

### Delta Pruning (inside quiescence)

```
  Before searching each capture:

  ┌──────────────────────────────────────────────────────────────┐
  │  if standPat + capturedPieceValue + DELTA (200) < α:         │
  │      skip this capture                                       │
  │                                                              │
  │  Example: standPat = -300, capturedValue = 100 (pawn)        │
  │           -300 + 100 + 200 = 0 < α (50)                      │
  │           Even taking this pawn can't raise α → skip it      │
  │                                                              │
  │  DELTA = 200 is a generous margin for positional improvement  │
  └──────────────────────────────────────────────────────────────┘
```

---

## 4. Futility Pruning

```
  Near leaf nodes, if the static eval + a generous margin
  still can't reach α, all quiet moves at this node are futile.

  ┌──────────────────────────────────────────────────────────────┐
  │  Depth 1 from leaf (one more move to evaluate):              │
  │      if staticEval + 200 ≤ α:  skip to quiescence           │
  │      (200cp = roughly a pawn — max gain from one quiet move) │
  │                                                              │
  │  Depth 2 from leaf (two more moves to evaluate):             │
  │      if staticEval + 400 ≤ α:  skip to quiescence           │
  │      (400cp = two pawns — max gain from two quiet moves)     │
  │                                                              │
  │  Guard: not inCheck (in check we MUST look at all moves)     │
  └──────────────────────────────────────────────────────────────┘

  Visual:
  ┌──────────────────────────────────────────────────────────────┐
  │  score needed to be useful = α (e.g. +50)                    │
  │  current static eval = -200                                  │
  │  max gain from 1 quiet move = +200                           │
  │  best possible: -200 + 200 = 0 < 50 = α                     │
  │  → Futile: skip all quiet moves, run quiescence instead      │
  └──────────────────────────────────────────────────────────────┘
```

---

## 5. The Full Search Stack

```
  Call at depth N:
  ┌─────────────────────────────────────────────────────────────┐
  │  negamax(depth=N)                                           │
  │    ├── TT probe → hit? return cached score                  │
  │    ├── Null-move? → reduced search → cutoff? return β       │
  │    ├── Futility? (depth 1 or 2) → jump to quiescence        │
  │    └── For each move (ordered):                             │
  │          ├── First 4 moves: full depth N-1                  │
  │          └── Remaining:     LMR reduced to N-2              │
  │                               if beats α → re-search N-1    │
  │                                                             │
  │  negamax(depth=0) → quiescence(board, α, β)                 │
  │    ├── Stand-pat: if ≥ β return, update α                   │
  │    └── For each capture:                                    │
  │          ├── Delta pruning: skip hopeless captures           │
  │          └── -quiescence(child, -β, -α)                     │
  └─────────────────────────────────────────────────────────────┘
```

---

## 6. Full Move Ordering Priority

```
  ┌─────────────────────────────────────────────────────────────┐
  │  Priority │ Source              │ Score                     │
  │  ─────────┼─────────────────────┼─────────────────────────  │
  │     1     │ TT best move        │ tried first (no score)    │
  │     2     │ Promotions          │ 20000                     │
  │     3     │ Captures (MVV-LVA)  │ 10000 + vic - atk/100     │
  │     4     │ Killers (2/depth)   │ 9000                      │
  │     5     │ History heuristic   │ 0 – 8999                  │
  │     6     │ Other quiet moves   │ 0                         │
  └─────────────────────────────────────────────────────────────┘
```

---

## Performance vs Lower Levels

```
  ┌────────────────────────────────────────────────────────────────┐
  │  Level │ Technique     │ Eff. Depth │ Nodes (approx) │  Elo   │
  │  ──────┼───────────────┼────────────┼────────────────┼──────  │
  │   L3   │ MiniMax       │     3      │     27,000     │  700   │
  │   L4   │ Alpha-Beta    │     4      │  10k – 40k     │ 1100   │
  │   L5   │ ID + TT       │    6–8     │  50k – 200k    │ 1500   │
  │   L6   │ Advanced AB   │   8–12     │ 100k – 500k    │ 1900+  │
  └────────────────────────────────────────────────────────────────┘

  Note: L6 searches more raw nodes than L5 (quiescence adds nodes)
        but reaches much greater EFFECTIVE depth in tactical positions,
        giving dramatically stronger tactical play.

  Effective depth gain from each L6 technique:
  ┌───────────────────────────────────────────────────────────────┐
  │  Null-move pruning  │ +1–2 plies equivalent  │ ~30% speed up │
  │  LMR               │ +2–3 plies equivalent  │ ~50% speed up │
  │  Quiescence        │ tactical horizon gone   │ quality gain  │
  │  Futility          │ +0.5 plies equivalent  │ ~10% speed up │
  └───────────────────────────────────────────────────────────────┘
```

---

## Limitations

- **No endgame tablebases** — cannot play perfect endgames
- **Approximate null-move** — uses static eval rather than a true pass-move (the board is immutable, making a real null move expensive to implement)
- **No singular extension** — doesn't identify and extend moves that are clearly best
- **No pondering** — doesn't think during the opponent's turn
