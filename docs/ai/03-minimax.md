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

## Java Implementation

```java
// MiniMaxStrategy.java
public final class MiniMaxStrategy implements MoveStrategy {

    private static final BoardEvaluator EVALUATOR = new StandardBoardEvaluator();
    private static final int DEPTH = 3;

    @Override
    public Move execute(final Board board) {
        Move bestMove  = Move.NULL_MOVE;
        int  bestScore = Integer.MIN_VALUE;

        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition transition =
                    board.currentPlayer().makeMove(move);

            if (transition.getMoveStatus().isDone()) {
                // After our move, it's the opponent's turn.
                // White maximises, so after white moves it's black's turn (MIN).
                // Black minimises, so after black moves it's white's turn (MAX).
                final int score = board.currentPlayer().getAlliance().isWhite()
                        ? min(transition.getTransitionBoard(), DEPTH - 1)
                        : max(transition.getTransitionBoard(), DEPTH - 1);

                if (score > bestScore) {
                    bestScore = score;
                    bestMove  = move;
                }
            }
        }
        return bestMove;
    }

    // MAX node: current player wants highest score (White's perspective)
    private int max(final Board board, final int depth) {
        if (depth == 0 || isEndGame(board)) {
            return EVALUATOR.evaluate(board, depth);
        }
        int best = Integer.MIN_VALUE;
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition transition =
                    board.currentPlayer().makeMove(move);
            if (transition.getMoveStatus().isDone()) {
                best = Math.max(best,
                        min(transition.getTransitionBoard(), depth - 1));
            }
        }
        return best;
    }

    // MIN node: opponent wants lowest score (minimise white's advantage)
    private int min(final Board board, final int depth) {
        if (depth == 0 || isEndGame(board)) {
            return EVALUATOR.evaluate(board, depth);
        }
        int best = Integer.MAX_VALUE;
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition transition =
                    board.currentPlayer().makeMove(move);
            if (transition.getMoveStatus().isDone()) {
                best = Math.min(best,
                        max(transition.getTransitionBoard(), depth - 1));
            }
        }
        return best;
    }

    private static boolean isEndGame(final Board board) {
        return board.currentPlayer().isInCheckMate()
            || board.currentPlayer().isInStaleMate();
    }

    @Override
    public String getStrategyName() { return "MiniMax"; }
}
```

---

## Sequence Diagram: MiniMax Recursion

```
  execute()           max()               min()          Evaluator
     │                  │                   │                │
     │─ makeMove(A) ───►│ (depth=2 WHITE)   │                │
     │                  │─ makeMove(A.r1) ──►│(depth=1 BLACK)│
     │                  │                   │─ makeMove(r1.w1)►(depth=0)
     │                  │                   │                │─ evaluate()
     │                  │                   │◄────────────────│ +50
     │                  │                   │─ makeMove(r1.w2)►(depth=0)
     │                  │                   │                │─ evaluate()
     │                  │                   │◄────────────────│ +20
     │                  │                   │  min(50,20)=-∞? │
     │                  │                   │  best = +20     │
     │                  │◄─ min returns +20 ─│                │
     │                  │─ makeMove(A.r2) ──►│                │
     │                  │       ...         │                │
     │                  │  max(20, ...) = X  │                │
     │◄─ score X ───────│                   │                │
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

### Node Growth Visualised

```
  Nodes:  30          900         27,000       810,000
           │           │             │              │
  Depth:  [1]─────────[2]──────────[3]────────────[4]
           ■           ■■■          ■■■■■■■■■■     ■■■■■■■■■■■■■■■■
                                    ^L3 stops here  ^would need AB pruning
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

```java
// Why depth matters in checkmate scoring:
// Mate in 1 (depth=2 remaining): bonus = 10000 + 2 = 10002
// Mate in 3 (depth=0 remaining): bonus = 10000 + 0 = 10000
// MiniMax prefers 10002 over 10000 → always plays the faster mate
```

### Evaluation is from White's absolute perspective
`StandardBoardEvaluator.evaluate()` always returns `whiteScore - blackScore`. The MAX node maximises this (White wants higher) and the MIN node minimises this (Black wants lower). This is correct because:
- White's MAX node picks the move that yields the largest `whiteScore - blackScore`
- Black's MIN node picks the move that yields the smallest `whiteScore - blackScore`

```
  Evaluation polarity at each depth:
  ┌──────────────────────────────────────────────────────────────┐
  │  Depth 0 (root, White):  MAX → wants evaluate() > 0         │
  │  Depth 1 (Black):        MIN → wants evaluate() < 0         │
  │  Depth 2 (White):        MAX → wants evaluate() > 0         │
  │  Depth 3 (leaf, Black):  MIN → evaluate() returned as-is    │
  └──────────────────────────────────────────────────────────────┘
```

### Why separate MAX and MIN instead of negamax?
Pure educational value. The negamax formulation (used in Levels 4–6) collapses the two functions into one by negating the score on alternation, but is harder to read. MiniMax at Level 3 deliberately keeps the explicit asymmetry so the code mirrors the textbook algorithm exactly.

```
  MiniMax (Level 3):         Negamax (Level 4+):
  ──────────────────         ───────────────────
  max(board, depth)          negamax(board, depth, α, β)
  min(board, depth)              score = -negamax(child, ...)
  2 separate functions       1 function, sign negated on recurse
  Easier to read             Harder to read, but more compact
  White/Black asymmetric     Always "current player" perspective
```

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

---

## Progression to Level 4

```
  What MiniMax is missing that Alpha-Beta adds:
  ┌──────────────────────────────────────────────────────────────┐
  │                                                              │
  │  MiniMax: evaluates ALL 810,000 nodes at depth 4            │
  │                                                              │
  │  Alpha-Beta: skips subtrees that CANNOT change the result   │
  │              evaluates only ~10,000–40,000 nodes at depth 4 │
  │                                                              │
  │  Result: IDENTICAL move choice, but 20–80× faster           │
  │          Speed gain used to search 1 ply deeper (depth 4)   │
  │                                                              │
  └──────────────────────────────────────────────────────────────┘
```
