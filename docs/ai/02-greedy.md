# Level 2 — Greedy Strategy

**File:** `src/com/chess/engine/player/ai/GreedyStrategy.java`  
**Strength:** ~400 Elo equivalent  
**Search depth:** 1 ply (looks one move ahead)  
**Time per move:** < 5 ms  

---

## Overview

The Greedy strategy looks exactly one move ahead. It evaluates the resulting board position after each of its legal moves using `StandardBoardEvaluator` and picks the move that yields the best immediate score. It never considers the opponent's response.

---

## How It Works at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                     GREEDY STRATEGY                             │
└─────────────────────────────────────────────────────────────────┘

  Current Board (score = 0)
         │
         ├──── move: e2-e4 ────► Board A  → evaluate → +15  (pawn to good square)
         │
         ├──── move: Nf3  ────► Board B  → evaluate → +25  (knight development)
         │
         ├──── move: Qxf7 ────► Board C  → evaluate → +900 (queen takes pawn!) ← PICK
         │
         └──── move: a2-a3 ───► Board D  → evaluate → +5   (weak flank pawn)

  Returns the move with the single highest immediate score.
  Does NOT look at what the opponent will do after.
```

---

## Algorithm

```
function GreedyMove(board):
    bestMove  ← NULL_MOVE
    bestScore ← -∞

    for each move in board.currentPlayer().getLegalMoves():
        transitionBoard ← board.currentPlayer().makeMove(move)
        if transitionBoard.status == DONE:
            score ← evaluate(transitionBoard)   // from current player's perspective
            if score > bestScore:
                bestScore ← score
                bestMove  ← move

    return bestMove
```

The `evaluate()` call returns `whiteScore - blackScore`. For white, we use this directly; for black, we negate it so a higher value always means "better for the current player."

---

## Evaluation Perspective Diagram

```
  StandardBoardEvaluator always returns:  whiteScore - blackScore

  ┌──────────────────────────────────────────────────────────────┐
  │   Playing as WHITE                  Playing as BLACK         │
  │                                                              │
  │   score =  +300                     score = +300            │
  │   (white is winning)                (white is winning)      │
  │                                                              │
  │   WHITE uses score directly         BLACK negates score      │
  │   bestScore = +300  ✓               bestScore = -300        │
  │                                     (means black is losing) │
  │                                                              │
  │   ► Positive always means           ► Same formula, just    │
  │     "I am winning"                    flip the sign         │
  └──────────────────────────────────────────────────────────────┘
```

---

## Implementation Details

### Evaluation Perspective
```java
final int currentValue = board.currentPlayer().getAlliance().isWhite()
        ? EVALUATOR.evaluate(transition.getTransitionBoard(), 0)
        : -EVALUATOR.evaluate(transition.getTransitionBoard(), 0);
```
`StandardBoardEvaluator.evaluate()` always returns `whiteScore - blackScore` (positive = white is better). Negating for black converts it to "positive = current player is better."

### Why depth=0 in the evaluate call?
The depth parameter in the evaluator is used to scale the checkmate bonus (`CHECKMATE_BONUS + depth * DEPTH_BONUS`) so that shallower mates score higher. At depth 0 (leaf node), the bonus is just the flat `CHECKMATE_BONUS` value.

### Move Validity
`board.currentPlayer().makeMove(move)` can return `ILLEGAL_MOVE` or `LEAVES_PLAYER_IN_CHECK`. Only `DONE` transitions are evaluated — this correctly avoids moves that expose the king.

---

## Greedy vs Random: What Changes

```
  RANDOM (Level 1)                    GREEDY (Level 2)
  ──────────────────────────────────────────────────────
  All moves equal probability    │   Scores every resulting position
  No evaluation at all           │   Uses StandardBoardEvaluator
  0 ply lookahead                │   1 ply lookahead
  ~200 Elo                       │   ~400 Elo
  < 1 ms                         │   < 5 ms

  Random would take a protected  │   Greedy always takes the highest-
  piece 33% of the time (1/N)    │   value piece it can reach, even
  by accident                    │   if recapture loses material
```

---

## Strengths Over Random

- Will capture free pieces
- Won't walk into an immediately losing position (one move deep)
- Will deliver checkmate in one if available
- Avoids moves that leave its own king in check (enforced by engine)

---

## Weaknesses

- Has no concept of the opponent's next move
- Will happily take a piece that's protected (trades badly)
- Will not set up two-move tactics
- Can be trapped by simple one-move threats

---

## Example of Greedy Failure

```
  Position: White threatens Qxf7# (checkmate) next move

  8 ║ r . b q k b n r
  7 ║ p p p p . Q p p    ← White queen on f7 threatens mate
  6 ║ . . n . . . . .
  5 ║ . . . . p . . .
  4 ║ . . . . P . . .    ← Black pawn on e4 is FREE (unprotected)
  3 ║ . . . . . . . .
  2 ║ P P P P . P P P
  1 ║ R N B . K B N R
      a b c d e f g h

  Black Greedy evaluates:
  ┌─────────────────────────────────────────────────────────┐
  │  ...exd4 (take pawn):  score = +100  ← GREEDY PICKS   │
  │  ...Nf6  (block mate): score = +5                       │
  │  ...Ke7  (run away):   score = -50                      │
  └─────────────────────────────────────────────────────────┘

  Greedy takes the pawn (+100), White plays Qxf7# → CHECKMATE
  Greedy couldn't see 1 move further to notice the threat.
```
