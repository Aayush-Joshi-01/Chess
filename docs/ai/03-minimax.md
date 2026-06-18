# Level 3 — MiniMax Strategy

**File:** `src/com/chess/engine/player/ai/MiniMaxStrategy.java`  
**Strength:** ~700 Elo equivalent  
**Search depth:** 3 plies  
**Time per move:** 50 ms – 2 s (opening/middlegame)  

---

## Overview

MiniMax is the foundational algorithm of game-tree search. It models chess as a two-player zero-sum game: one player tries to **maximise** the evaluation score, the other tries to **minimise** it. The algorithm recursively explores all possible move sequences to a fixed depth, then back-propagates the best score.

---

## The Game Tree (Depth 3)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        MINIMAX SEARCH TREE                              │
└─────────────────────────────────────────────────────────────────────────┘

  Depth 0  ┌──────────────────────┐
  (Root)   │  Current Position    │  ← White to move: MAXIMISE
           │  score = ?           │
           └──────────────────────┘
                ╱         ╲
               ╱           ╲
  Depth 1  [move A]      [move B]         ← White's moves
           score=?        score=?            (pick MAX)
              │                │
         ┌────┴────┐      ┌────┴────┐
  Depth 2 │  │     │      │  │     │      ← Black replies
         [r1][r2][r3]    [r1][r2][r3]        (pick MIN)
          │              │
  Depth 3 ├──┬──┐        ├──┬──┐           ← White replies
         [w1][w2][w3]   [w1][w2][w3]           EVALUATE HERE
          ↑   ↑   ↑
        leaf nodes: call StandardBoardEvaluator

  Back-propagate:
    MIN nodes take the SMALLEST score from their children
    MAX nodes take the LARGEST score from their children
```

---

## Back-Propagation Example

```
  Depth 3 (leaves, evaluated):
                  +50   +20   -30   +80   +10   +40
                   │     │     │     │     │     │
  Depth 2 (MIN): ┌─────────────┐   ┌─────────────┐
                 │  min = -30  │   │  min = +10  │
                 └─────────────┘   └─────────────┘
                        │                 │
  Depth 1 (MAX):   ┌────────────────────────┐
                   │     max = max(-30, +10) = +10    │
                   └────────────────────────┘
                                │
  Depth 0 (root):    pick the move that led to +10
```

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

---

## Node Count at Each Depth

```
  Average branching factor ≈ 30 legal moves per position

  Depth │ Nodes to evaluate │ Time (approx)
  ──────┼───────────────────┼──────────────
    1   │               30  │   < 1 ms
    2   │              900  │   < 1 ms
    3   │           27,000  │  50–200 ms     ← Level 3 searches here
    4   │          810,000  │    1–5 s       ← too slow without pruning
    5   │       24,300,000  │   30–90 s      ← impractical
    6   │      729,000,000  │   hours        ← never finishes

  Alpha-Beta (Level 4) at the same depth explores only ~b^(d/2) nodes:
    depth 4 with pruning ≈ 30^2 = ~900 nodes  (vs 810,000 raw)
```

---

## Key Properties

```
  ┌────────────────────────────────────────────────────────────┐
  │  Property         │ Value                                  │
  │  ─────────────────┼──────────────────────────────────────  │
  │  Complete         │ Yes — always finds a move if one exists│
  │  Optimal          │ Yes — optimal vs a perfect opponent    │
  │                   │       given sufficient depth           │
  │  Time complexity  │ O(b^d)   b=branching, d=depth         │
  │  Space complexity │ O(d)     just the call stack           │
  │  Pruning          │ None — every node explored             │
  └────────────────────────────────────────────────────────────┘
```

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

## Minimax vs Greedy: What Changes

```
  GREEDY (Level 2)                     MINIMAX (Level 3)
  ─────────────────────────────────────────────────────────────
  1-ply: scores own move only    │   3-ply: scores after White,
                                 │   Black, White reply chains
  Can't see opponent response    │   Models opponent as optimal
  Walks into 1-move traps        │   Avoids 2-move tactics
  ~400 Elo                       │   ~700 Elo
  < 5 ms                         │   50–2000 ms
```

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
