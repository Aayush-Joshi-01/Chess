# Level 2 — Greedy Strategy

**File:** `src/com/chess/engine/player/ai/GreedyStrategy.java`  
**Strength:** ~400 Elo equivalent  
**Search depth:** 1 ply (looks one move ahead)  
**Time per move:** < 5 ms  

---

## Overview

The Greedy strategy looks exactly one move ahead. It evaluates the resulting board position after each of its legal moves using `StandardBoardEvaluator` and picks the move that yields the best immediate score. It never considers the opponent's response.

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
White: Qd1-h5 (threatens mate on f7)
Black Greedy: takes a free pawn on e4 (best immediate score)
White: Qxf7# (checkmate)
```

Greedy sees taking the pawn as +100 but cannot see that White mates next move.
