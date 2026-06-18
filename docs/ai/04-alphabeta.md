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

## Alpha and Beta

Two bounds are maintained throughout the search:

| Variable | Meaning |
|---|---|
| `α` (alpha) | The best score the **maximising** player is **guaranteed** so far (lower bound) |
| `β` (beta)  | The best score the **minimising** player is **guaranteed** so far (upper bound) |

The window `[α, β]` narrows as the search progresses. When `α ≥ β`, the current subtree cannot improve upon an already-found result — it is **pruned** (skipped entirely).

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

## Pruning: When and Why It Works

### Beta Cutoff (most common)
```
Our node: α = 30, β = 80
We find a move scoring 90.
Since 90 ≥ β (80), the opponent would never allow us to reach this node
(they already have a path keeping score ≤ 80). Prune all remaining siblings.
```

### Alpha Update
```
Our node: α = 30
We find a move scoring 45. Update α = 45.
Future siblings must beat 45 to matter.
```

### Savings
With perfect move ordering, alpha-beta reduces the search from O(b^d) to O(b^(d/2)) — effectively doubling the search depth for the same computation. With random ordering it reduces to O(b^(3d/4)). This implementation uses MVV-LVA ordering which is close to the ideal case.

---

## Move Ordering: MVV-LVA

**Most Valuable Victim / Least Valuable Attacker** sorts captures so the best captures come first, triggering beta cutoffs earlier.

```
score(capture) = 10000 + victimValue - attackerValue/100
```

| Capture | Score |
|---|---|
| Pawn×Queen (P takes Q) | 10000 + 900 - 1 = 10899 |
| Queen×Queen (Q takes Q) | 10000 + 900 - 9 = 10891 |
| Queen×Pawn (Q takes P) | 10000 + 100 - 9 = 10091 |
| Quiet moves | < 9000 |

This ensures captures are searched before quiet moves, and free captures (low-value attacker) before risky captures.

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

| | MiniMax (Level 3) | Alpha-Beta (Level 4) |
|---|---|---|
| Depth | 3 | 4 |
| Nodes at d=4 (avg branch 30) | 810,000 | ~10,000–40,000 |
| Pruning | None | Beta cutoffs |
| Move ordering | None | MVV-LVA |
| Speed | Slow | ~10–40× faster |
| Strength | ~700 Elo | ~1100 Elo |
