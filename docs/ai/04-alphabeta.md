# Level 4 — Alpha-Beta Strategy

**File:** `src/com/chess/engine/player/ai/AlphaBetaStrategy.java`  
**Strength:** ~1100 Elo equivalent  
**Search depth:** 4 plies  
**Time per move:** 100 ms – 3 s  

---

## Overview

Alpha-Beta pruning is the single most important optimisation in game-tree search. It produces **identical results** to MiniMax but skips large subtrees that cannot possibly affect the final decision. In the best case it halves the effective depth cost, allowing depth 4 to run in roughly the same time as MiniMax at depth 2.

This implementation uses **negamax** — a compact reformulation where you always evaluate from the current player's perspective, negating the child score on recursion.

---

## Alpha and Beta: What They Mean

```
  ┌────────────────────────────────────────────────────────────────┐
  │                  THE SEARCH WINDOW [α, β]                      │
  │                                                                │
  │     α (alpha) = best score the MAX player is GUARANTEED so far │
  │     β (beta)  = best score the MIN player is GUARANTEED so far │
  │                                                                │
  │   ◄──────────────────────────────────────────────────────────► │
  │   -∞        α ──── search window ──── β        +∞             │
  │         [lower bound]            [upper bound]                 │
  │                                                                │
  │   Scores inside [α,β] are interesting.                         │
  │   Scores outside [α,β] can never be the final answer.         │
  └────────────────────────────────────────────────────────────────┘
```

---

## Pruning: Visualised

```
  MiniMax (Level 3) — evaluates EVERYTHING:

  Root(MAX) ──── A ──── A1:+30   ← evaluates all
                 │──── A2:+50
                 │──── A3:+10
                 │
                 B ──── B1:+40
                 │──── B2:+20
                 └──── B3:+60


  Alpha-Beta (Level 4) — prunes subtrees that can't change outcome:

  Root(MAX) ──── A(MIN) ──── A1:+30  (α = -∞, β = +∞)
                  │           update α = 30
                  │──── A2:+50  (β cutoff? No — MIN wants lower)
                  │           update: A returns min(30,50,10) = 10
                  └──── A3:+10  A returns 10, root α = 10

             ──── B(MIN) ──── B1:+40  (α=10, β=+∞)
                  │           B could return ≤ 40
                  │──── B2:+20  B could return ≤ 20
                  │           B could return ≤ 20 < α (10)? No
                  └──── B3: ██ PRUNED ██  B already ≤ 20, root
                              won't pick B (root has A=10 ≥ any B≤20)
```

---

## Beta Cutoff — Step by Step

```
  Scenario: α = 30, β = 80 at a MAX node

  Step 1: Evaluate move X → score = 90
          ┌──────────────────────────────────────────────┐
          │  score (90) ≥ β (80)                         │
          │  The opponent at the parent MIN node already  │
          │  has a path scoring ≤ 80. This node would    │
          │  give the current player 90 — the opponent   │
          │  will NEVER allow us to reach this branch.   │
          │  → PRUNE all remaining siblings              │
          └──────────────────────────────────────────────┘

  Step 2: Return β (fail-hard) without evaluating remaining moves
          Saved: could be 20+ subtrees skipped
```

---

## Algorithm

```
function negamax(board, depth, α, β):
    if depth == 0 or isEndGame(board):
        return sign * evaluate(board)      // sign: +1 white, -1 black

    for each move in orderedMoves(board, depth):
        transition ← makeMove(move)
        if not transition.isDone(): continue

        score ← -negamax(transition.board, depth-1, -β, -α)

        if score ≥ β:
            return β            // β-cutoff: opponent will avoid this branch
        if score > α:
            α ← score           // new best for current player

    return α

function AlphaBetaMove(board):
    bestMove  ← NULL_MOVE
    bestScore ← -∞
    α ← -∞,  β ← +∞
    for each move in orderedMoves(board, DEPTH):
        transition ← makeMove(move)
        if transition.isDone():
            score ← -negamax(transition.board, DEPTH-1, -β, -α)
            if score > bestScore:
                bestScore ← score
                bestMove  ← move
                α ← max(α, score)
    return bestMove
```

### Score Negation Pattern
Each recursive call swaps the sign of the score and inverts `α`/`β`:
```
-negamax(child, depth-1, -β, -α)
```
This works because the child's best move for them is the worst outcome for us. Negating the child's score converts it to our perspective.

---

## Negamax Sign Inversion Explained

```
  Parent (my turn):          Child (opponent's turn):
  ┌────────────────────┐     ┌────────────────────────┐
  │  I want score > 0  │     │  Opponent wants < 0    │
  │  (positive = good) │     │  (negative = good for  │
  │                    │─────►   them = bad for me)   │
  │  child returns -X  │     │  child returns +X       │
  │  I negate → +X     │◄────│  (X is good for them)  │
  └────────────────────┘     └────────────────────────┘

  My α/β become -β/-α at the child node:
  ┌─────────────────────────────────────────────────┐
  │  Parent: [α=10, β=80]  → Child: [-80, -10]      │
  │  Child scores from -80 to -10 (opponent's range) │
  │  Parent negates: [10, 80]  (back to my range)    │
  └─────────────────────────────────────────────────┘
```

---

## Move Ordering: MVV-LVA

**Most Valuable Victim / Least Valuable Attacker** sorts captures so the best captures come first, triggering beta cutoffs earlier.

```
  score(capture) = 10000 + victimValue - attackerValue/100

  ┌─────────────────────────────────────────────────────────────┐
  │  Capture              │ Victim │ Attacker │  Score          │
  │  ─────────────────────┼────────┼──────────┼───────────────  │
  │  Pawn takes Queen     │  900   │   100    │  10899  ← best  │
  │  Knight takes Queen   │  900   │   320    │  10897          │
  │  Queen takes Queen    │  900   │   900    │  10891          │
  │  Pawn takes Rook      │  500   │   100    │  10499          │
  │  Knight takes Pawn    │  100   │   320    │  10097          │
  │  Queen takes Pawn     │  100   │   900    │  10091  ← worst │
  │  Any quiet move       │   —    │    —     │  < 9000         │
  └─────────────────────────────────────────────────────────────┘

  Captures are always searched BEFORE quiet moves.
  Free captures (cheap attacker) before risky ones.
```

---

## Performance vs MiniMax

```
  ┌──────────────────────────────────────────────────────────┐
  │              NODES SEARCHED (branch factor = 30)         │
  │                                                          │
  │  Depth │  MiniMax (L3) │ Alpha-Beta (L4) │  Speedup     │
  │  ──────┼───────────────┼────────────────┼──────────────  │
  │    3   │      27,000   │    ~1,600      │     17×       │
  │    4   │     810,000   │   ~10,000      │     81×       │
  │    5   │  24,300,000   │  ~300,000      │     81×       │
  │    6   │ 729,000,000   │ ~9,000,000     │     81×       │
  │                                                          │
  │  Best case (perfect ordering):  b^(d/2) nodes           │
  │  Worst case (no ordering):      b^(3d/4) nodes           │
  │  This implementation:           close to best case       │
  └──────────────────────────────────────────────────────────┘
```

---

## Implementation Details

### Fail-Soft vs Fail-Hard
This implementation uses **fail-hard**: on a beta cutoff, it returns exactly `β` rather than the score that caused the cutoff. This is simpler and safe for correctness.

### Move Ordering Reset
`MOVE_ORDERING.reset()` is called once per `execute()` call to clear the killer and history tables from the previous search. Without this, stale data from a previous position could bias move ordering.

### Sign Helper
```java
private int sign(final Board board) {
    return board.currentPlayer().getAlliance().isWhite() ? 1 : -1;
}
```
Converts the absolute `whiteScore - blackScore` evaluation into the current-player-relative score that negamax requires.

---

## Comparison with MiniMax

```
  ┌────────────────────────────────────────────────────────────────┐
  │                  │  MiniMax (Level 3)  │  Alpha-Beta (Level 4) │
  │  ────────────────┼─────────────────────┼─────────────────────  │
  │  Depth           │         3           │          4            │
  │  Nodes (avg)     │    27,000           │    ~10,000–40,000     │
  │  Pruning         │    None             │    Beta cutoffs       │
  │  Move ordering   │    None             │    MVV-LVA            │
  │  Speed           │    Slow             │    ~10–40× faster     │
  │  Strength        │    ~700 Elo         │    ~1100 Elo          │
  └────────────────────────────────────────────────────────────────┘
```
