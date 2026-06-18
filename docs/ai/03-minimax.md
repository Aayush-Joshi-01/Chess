# Level 3 — MiniMax Strategy

**File:** `src/com/chess/engine/player/ai/MiniMaxStrategy.java`  
**Strength:** ~700 Elo equivalent  
**Search depth:** 3 plies  
**Time per move:** 50 ms – 2 s (opening/middlegame)  

---

## Overview

MiniMax is the foundational algorithm of game-tree search. It models chess as a two-player zero-sum game: one player tries to **maximise** the evaluation score, the other tries to **minimise** it. The algorithm recursively explores all possible move sequences to a fixed depth, then back-propagates the best score.

---

## The Game Tree

At depth 3 with an average branching factor of 30:

```
                     Root (White to move)
                    /        |        \
               move1       move2    ... ~30 moves
              /  | \       /  | \
           r1   r2  ...  r1  r2  ...   (Black replies, ~30 each)
          / \             / \
        w1  w2 ...      w1  w2 ...     (White replies, ~30 each)
        ↑   ↑             ↑   ↑
    leaf nodes — evaluated here (~27,000 positions)
```

Total nodes evaluated: ≈ 30³ = 27,000 (before move legality filtering).

---

## Algorithm

The classic MAX/MIN separation makes the logic explicit:

```
function max(board, depth):
    if depth == 0 or isEndGame(board):
        return evaluate(board)           // static evaluation

    best ← -∞
    for each move in board.currentPlayer().getLegalMoves():
        transition ← makeMove(move)
        if transition.isDone():
            best ← max(best, min(transition.board, depth - 1))
    return best

function min(board, depth):
    if depth == 0 or isEndGame(board):
        return evaluate(board)

    best ← +∞
    for each move in board.currentPlayer().getLegalMoves():
        transition ← makeMove(move)
        if transition.isDone():
            best ← min(best, max(transition.board, depth - 1))
    return best

function MiniMaxMove(board):
    bestMove  ← NULL_MOVE
    bestScore ← -∞
    for each move in board.currentPlayer().getLegalMoves():
        transition ← makeMove(move)
        if transition.isDone():
            // White maximises; MIN node comes next
            score ← (white to move) ? min(transition.board, DEPTH-1)
                                     : max(transition.board, DEPTH-1)
            if score > bestScore:
                bestScore ← score
                bestMove  ← move
    return bestMove
```

### Key Properties

| Property | Value |
|---|---|
| **Complete** | Yes — always finds a move if one exists |
| **Optimal** | Yes — optimal against a perfect opponent, given enough depth |
| **Complexity** | O(b^d) time, O(d) space (b=branching factor, d=depth) |
| **No pruning** | Explores every node — purely illustrative at this depth |

---

## Implementation Details

### `isEndGame(board)`
Returns `true` if the current player is in checkmate or stalemate. At these terminal nodes the evaluator returns:
- Checkmate: `10000 + depth` (high positive for the side that delivered it, scaled by depth so shallower mates rank higher)
- Stalemate: near-zero (slightly negative for the side that caused it)

### Evaluation is from White's absolute perspective
`StandardBoardEvaluator.evaluate()` always returns `whiteScore - blackScore`. The MAX node maximises this (White wants higher) and the MIN node minimises this (Black wants lower). This is correct because:
- White's MAX node picks the move that yields the largest `whiteScore - blackScore`
- Black's MIN node picks the move that yields the smallest `whiteScore - blackScore`

### Why separate MAX and MIN instead of negamax?
Pure educational value. The negamax formulation (used in Levels 4–6) collapses the two functions into one by negating the score on alternation, but is harder to read. MiniMax at Level 3 deliberately keeps the explicit asymmetry so the code mirrors the textbook algorithm exactly.

---

## Strengths Over Greedy

- Sees 3 moves ahead — avoids walking into simple tactics
- Recognises and avoids 2-move checkmate threats
- Will sacrifice material for a winning continuation
- Plays reasonable opening moves based on PST bonuses

## Weaknesses

- No pruning — evaluates every position, slow on deep positions
- 3-ply means it misses 4-move combinations
- No move ordering — evaluates moves in arbitrary order (no speedup from good moves first)
- Horizon effect: a forced loss just past depth 3 looks like a draw or win
