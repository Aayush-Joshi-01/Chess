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

### Null-Move Decision Tree

```
  negamax(board, depth=6, α, β):
      │
      ├── inCheck? YES → skip null-move (must search all replies)
      │
      ├── depth < R+1? YES → skip (not enough depth for reduction)
      │
      └── NO → try null move:
               nullScore = sign × evaluate(board, depth - R - 1)
                         = sign × evaluate(board, 3)     // R=2, depth=6
               │
               ├── nullScore >= β → PRUNE (return β, skip real moves)
               │                    saves entire subtree
               │
               └── nullScore < β  → continue normally, search all moves
                                    (but null-move flag set: no double null)
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

### Java Implementation

```java
// Inside AdvancedAlphaBetaStrategy.negamax():
if (allowNullMove && depth >= NULL_MOVE_R + 1
        && !board.currentPlayer().isInCheck()) {

    // Approximate null move with static evaluation at reduced depth
    final int nullScore = sign(board)
            * EVALUATOR.evaluate(board, depth - NULL_MOVE_R - 1);

    if (nullScore >= beta) {
        return beta;  // null-move cutoff — branch is too good for opponent to allow
    }
}

// After null-move, recurse with allowNullMove=false (no "double null")
final int score = -negamax(child, depth-1, -beta, -alpha,
                           deadline, hash, /*allowNullMove=*/true);
// Null-move subtree itself uses allowNullMove=false to prevent double null
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

### LMR Decision Tree

```
  for each move (ordered):
      moveCount++
      │
      ├── moveCount <= 4?       → full depth search  (always search top moves)
      ├── inCheck?              → full depth search  (must see all replies)
      ├── isCapture?            → full depth search  (captures can be good)
      ├── isPromotion?          → full depth search  (promotions matter)
      ├── depth < 3?            → full depth search  (too shallow to reduce)
      │
      └── all guards passed:
              │
              ├── Scout search: -negamax(child, depth-2, -α-1, -α)
              │                  ↑ reduced depth, null window
              │
              ├── score <= α? → move is bad, skip full search
              │
              └── score > α?  → re-search at full depth: -negamax(child, depth-1, -β, -α)
                                 ↑ confirms the move is actually good
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

### Java Implementation

```java
// Inside AdvancedAlphaBetaStrategy.negamax():
int moveCount = 0;

for (final Move move : MOVE_ORDERING.orderMoves(legalMoves, board, depth)) {
    final MoveTransition t = board.currentPlayer().makeMove(move);
    if (!t.getMoveStatus().isDone()) continue;

    moveCount++;
    final Board child     = t.getTransitionBoard();
    final long  childHash = ZobristHasher.hash(child);
    int score;

    final boolean doLMR = !board.currentPlayer().isInCheck()
            && moveCount > LMR_THRESHOLD          // move #4+
            && depth >= 3                          // not too shallow
            && !move.isAttack()                    // not a capture
            && !(move instanceof Move.PawnPromotion);

    if (doLMR) {
        // Scout at depth-2 with null window [-α-1, -α]
        score = -negamax(child, depth - 2, -alpha - 1, -alpha,
                         deadline, childHash, true);

        if (score > alpha) {
            // Scout raised alpha → re-search at full depth
            score = -negamax(child, depth - 1, -beta, -alpha,
                             deadline, childHash, true);
        }
    } else {
        // Full-depth search for captures, killers, early moves
        score = -negamax(child, depth - 1, -beta, -alpha,
                         deadline, childHash, true);
    }

    if (score >= beta) {
        MOVE_ORDERING.recordKiller(move, depth);
        return beta;
    }
    if (score > best) {
        best  = score;
        alpha = Math.max(alpha, score);
        MOVE_ORDERING.recordHistory(move, depth);
    }
}
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

### Horizon Effect Tree Diagram

```
  Without Quiescence Search:
  ──────────────────────────

  negamax(depth=3)
      └── White: Rxe5 (captures rook, +500)
          └── [SEARCH STOPS] evaluate() → +500 ← WRONG

  Reality (not seen):
      └── White: Rxe5
          └── Black: Nxe5 (recaptures)
              └── evaluate() → +0  (net even trade)


  With Quiescence Search:
  ───────────────────────

  negamax(depth=3)
      └── White: Rxe5
          └── [depth=0] → quiescence(board)
              ├── standPat = +500
              └── captures available: Nxe5 by Black
                  └── -quiescence(after Nxe5)
                      ├── standPat = +0 (rook returned)
                      └── no more captures → return 0
              return max(500, -0) = 0   ← CORRECT
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

### Java Implementation

```java
// AdvancedAlphaBetaStrategy.java — quiescence search
private int quiescence(final Board board, int alpha, final int beta,
                        final long deadline) {
    if (System.currentTimeMillis() >= deadline) return 0;

    // Stand-pat: evaluate without making a capture
    final int standPat = sign(board) * EVALUATOR.evaluate(board, 0);

    if (standPat >= beta)  return beta;   // stand-pat cutoff
    if (standPat > alpha)  alpha = standPat;

    // Search only captures (and promotions)
    for (final Move move : board.currentPlayer().getLegalMoves()) {
        if (!move.isAttack() && !(move instanceof Move.PawnPromotion)) continue;

        // Delta pruning: if even capturing this piece can't raise alpha, skip
        if (move.isAttack()) {
            final int capturedValue = move.getAttackedPiece().getPieceValue();
            if (standPat + capturedValue + DELTA_MARGIN < alpha) continue;
        }

        final MoveTransition t = board.currentPlayer().makeMove(move);
        if (!t.getMoveStatus().isDone()) continue;

        final int score = -quiescence(t.getTransitionBoard(), -beta, -alpha, deadline);

        if (score >= beta)  return beta;
        if (score > alpha)  alpha = score;
    }
    return alpha;
}
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

### Quiescence Sequence Diagram

```
  negamax(depth=0)       quiescence()           quiescence() child
         │                     │                       │
         │── return qSearch() ►│                       │
         │                     │── standPat=evaluate() │
         │                     │  standPat < β, > α    │
         │                     │  α = standPat         │
         │                     │                       │
         │                     │── filter: captures only
         │                     │── move: Rxe5 (capture rook)
         │                     │── makeMove() ────────►│
         │                     │                       │── standPat=evaluate()
         │                     │                       │── no more captures
         │                     │                       │── return α
         │                     │◄─ -score ─────────────│
         │                     │   score > α? update α │
         │                     │── move: Bxd4 (another capture)
         │                     │   delta prune? skip if hopeless
         │                     │── return α (quiet position score)
         │◄── final score ──────│
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

### Java Implementation

```java
// Inside AdvancedAlphaBetaStrategy.negamax():
// Futility pruning near leaf nodes
if (!board.currentPlayer().isInCheck() && depth <= 2) {
    final int staticEval = sign(board) * EVALUATOR.evaluate(board, depth);
    final int margin = (depth == 1) ? FUTILITY_MARGIN_1   // 200
                                    : FUTILITY_MARGIN_2;  // 400
    if (staticEval + margin <= alpha) {
        // Statically hopeless — skip quiet moves, run quiescence
        return quiescence(board, alpha, beta, deadline);
    }
}
```

### Futility Decision Flowchart

```
  negamax(depth=1 or 2):
      │
      ├── inCheck? YES → skip futility, search all moves normally
      │
      ├── staticEval + margin > alpha? YES → search normally
      │
      └── staticEval + margin ≤ alpha:
              │
              └── FUTILITY: jump straight to quiescence()
                  ├── quiescence will still find tactical wins
                  │   (it searches captures only)
                  └── quiet moves skipped entirely
                      (they can't raise alpha by definition)
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

### Full negamax Method

```java
// AdvancedAlphaBetaStrategy.java — complete negamax
private int negamax(final Board board, final int depth,
                    int alpha, final int beta,
                    final long deadline, final long hash,
                    final boolean allowNullMove) {

    if (System.currentTimeMillis() >= deadline) return 0;

    // 1. TT probe
    final TranspositionTable.Entry entry = TT.probe(hash);
    if (entry != null && entry.depth() >= depth) {
        if (entry.flag() == TranspositionTable.EXACT) return entry.score();
        if (entry.flag() == TranspositionTable.LOWER) alpha = Math.max(alpha, entry.score());
        if (entry.flag() == TranspositionTable.UPPER) beta  = Math.min(beta,  entry.score());
        if (alpha >= beta) return entry.score();
    }

    // 2. Terminal / leaf
    if (depth == 0 || isEndGame(board)) {
        return quiescence(board, alpha, beta, deadline);
    }

    // 3. Null-move pruning
    if (allowNullMove && depth >= NULL_MOVE_R + 1
            && !board.currentPlayer().isInCheck()) {
        final int nullScore = sign(board)
                * EVALUATOR.evaluate(board, depth - NULL_MOVE_R - 1);
        if (nullScore >= beta) return beta;
    }

    // 4. Futility pruning (depth 1 or 2 from leaf)
    if (!board.currentPlayer().isInCheck() && depth <= 2) {
        final int staticEval = sign(board) * EVALUATOR.evaluate(board, depth);
        final int margin = (depth == 1) ? FUTILITY_MARGIN_1 : FUTILITY_MARGIN_2;
        if (staticEval + margin <= alpha) {
            return quiescence(board, alpha, beta, deadline);
        }
    }

    // 5. Main search
    int best      = Integer.MIN_VALUE;
    int flag      = TranspositionTable.UPPER;
    int moveCount = 0;

    for (final Move move : MOVE_ORDERING.orderMoves(
            board.currentPlayer().getLegalMoves(), board, depth)) {

        final MoveTransition t = board.currentPlayer().makeMove(move);
        if (!t.getMoveStatus().isDone()) continue;
        moveCount++;

        final Board child     = t.getTransitionBoard();
        final long  childHash = ZobristHasher.hash(child);
        int score;

        // 6. Late Move Reduction
        final boolean doLMR = !board.currentPlayer().isInCheck()
                && moveCount > LMR_THRESHOLD
                && depth >= 3
                && !move.isAttack()
                && !(move instanceof Move.PawnPromotion);

        if (doLMR) {
            score = -negamax(child, depth - 2, -alpha - 1, -alpha,
                             deadline, childHash, true);
            if (score > alpha) {
                score = -negamax(child, depth - 1, -beta, -alpha,
                                 deadline, childHash, true);
            }
        } else {
            score = -negamax(child, depth - 1, -beta, -alpha,
                             deadline, childHash, true);
        }

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

## 7. Full Sequence Diagram: A Level 6 Move Decision

```
  AIThinkTank      execute()      rootSearch()    negamax()   quiescence()
       │               │                │              │             │
       │─ execute() ──►│                │              │             │
       │               │─ depth=1 ─────►│              │             │
       │               │               │─ makeMove() ─►│             │
       │               │               │              │─ TT probe    │
       │               │               │              │  (cold miss) │
       │               │               │              │─ depth=0     │
       │               │               │              │─ qSearch() ─►│
       │               │               │              │             │─ standPat
       │               │               │              │             │─ captures
       │               │               │              │             │─ return α
       │               │               │              │◄─ score ────│
       │               │◄─ result ─────│              │             │
       │               │               │              │             │
       │               │─ depth=2 ─────►│              │             │
       │               │  (aspiration  │─ null-move ──►│             │
       │               │   window ±50) │  check        │             │
       │               │               │─ LMR for late moves         │
       │               │               │─ each move:  │             │
       │               │               │  negamax(d=1)►│             │
       │               │               │              │─ futility?   │
       │               │               │              │  → qSearch() │
       │               │               │              │◄─ score ─────│
       │               │               │◄─ result ────│             │
       │               │               │              │             │
       │               │  ... depths 3,4,5,6 ...      │             │
       │               │  [deadline reached]          │             │
       │               │── return bestMove from last   │             │
       │               │   complete depth             │             │
       │◄─ Move ───────│                              │             │
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
