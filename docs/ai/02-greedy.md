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

## Java Implementation

```java
// GreedyStrategy.java
public final class GreedyStrategy implements MoveStrategy {

    private static final BoardEvaluator EVALUATOR = new StandardBoardEvaluator();

    @Override
    public Move execute(final Board board) {
        Move bestMove  = Move.NULL_MOVE;
        int  bestScore = Integer.MIN_VALUE;

        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition transition =
                    board.currentPlayer().makeMove(move);

            // Only consider moves that don't leave us in check
            if (transition.getMoveStatus().isDone()) {
                // Negate for black so positive always means "I'm winning"
                final int currentValue =
                        board.currentPlayer().getAlliance().isWhite()
                        ? EVALUATOR.evaluate(transition.getTransitionBoard(), 0)
                        : -EVALUATOR.evaluate(transition.getTransitionBoard(), 0);

                if (currentValue > bestScore) {
                    bestScore = currentValue;
                    bestMove  = move;
                }
            }
        }
        return bestMove;
    }

    @Override
    public String getStrategyName() { return "Greedy"; }
}
```

### Sequence Diagram: Greedy Move Execution

```
  AIThinkTank        GreedyStrategy      Board/Player      StandardBoardEvaluator
       │                   │                  │                      │
       │─ execute(board) ─►│                  │                      │
       │                   │─ getLegalMoves()►│                      │
       │                   │◄─ List<Move> ────│                      │
       │                   │                  │                      │
       │                   │  ┌─ for each move in list ────────┐     │
       │                   │  │                                │     │
       │                   │  │─ makeMove(move) ──────────────►│     │
       │                   │  │◄─ MoveTransition ──────────────│     │
       │                   │  │                                │     │
       │                   │  │  if status == DONE:            │     │
       │                   │  │─ evaluate(transitionBoard) ─────────►│
       │                   │  │◄─ int score ───────────────────────── │
       │                   │  │                                │     │
       │                   │  │  if score > bestScore:         │     │
       │                   │  │    bestMove = move             │     │
       │                   │  └────────────────────────────────┘     │
       │                   │                                          │
       │◄─ bestMove ────────│                                          │
```

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

### How StandardBoardEvaluator Scores a Position

```
  evaluate(board, depth=0) =

    Σ for each WHITE piece:
        + pieceValue          (Pawn=100, Knight=320, Bishop=330, Rook=500, Queen=900)
        + pst[square]         (Piece-Square Table bonus for position)
        + mobilityBonus       (legalMoves.size() × 5)

    Σ for each BLACK piece:
        - pieceValue
        - pst[mirroredSquare]
        - mobilityBonus

    + checkBonus              (+50 if opponent is in check)
    + checkmateBonus          (+10000 + depth if opponent is in checkmate)
    - doublePawnPenalty       (−30 per doubled pawn)
    - isolatedPawnPenalty     (−20 per isolated pawn)
    + rookOpenFilebonus       (+25 if rook on open file)
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

```java
// Inside StandardBoardEvaluator:
private static int checkMateBonus(final Player player, final int depth) {
    // Prefer faster checkmates: depth 5 mate scores higher than depth 3 mate
    return player.getOpponent().isInCheckMate() ? CHECK_MATE_BONUS + (depth * DEPTH_BONUS) : 0;
}
// At Greedy depth=0: bonus = CHECK_MATE_BONUS + 0 = flat bonus
// At AlphaBeta depth=4: bonus at deeper node = CHECK_MATE_BONUS + 4*DEPTH_BONUS
```

### Move Validity
`board.currentPlayer().makeMove(move)` can return `ILLEGAL_MOVE` or `LEAVES_PLAYER_IN_CHECK`. Only `DONE` transitions are evaluated — this correctly avoids moves that expose the king.

```
  MoveStatus enum:
  ┌────────────────────────────────────────────────────────────┐
  │  DONE               → move is legal and executed           │
  │  ILLEGAL_MOVE       → physically impossible (shouldn't     │
  │                        happen if legalMoves() is correct)  │
  │  LEAVES_PLAYER_IN_CHECK → move exposes own king → skip     │
  └────────────────────────────────────────────────────────────┘
```

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

### Why Greedy Fails Here — Decision Tree

```
  Black to move, Greedy evaluates depth=1 only:

  [Current position]
       ├── ...exd4  → eval = +100  (captures pawn, looks great)  ← CHOSEN
       ├── ...Nf6   → eval = +5    (develops knight, blocks queen)
       └── ...Ke7   → eval = -50   (king runs)

  What MINIMAX (Level 3) would see at depth=2:
  [Current position]
       ├── ...exd4  → [White: Qxf7#] → eval = -10000  CHECKMATE  ← AVOIDED
       ├── ...Nf6   → [White: best]  → eval = -20               ← CHOSEN
       └── ...Ke7   → [White: best]  → eval = -40
```

---

## Progression to Level 3

```
  What Greedy is missing that MiniMax adds:
  ┌──────────────────────────────────────────────────────────────┐
  │                                                              │
  │  Greedy: evaluates board AFTER own move only (1 ply)        │
  │                                                              │
  │  MiniMax: evaluates sequences own→opponent→own (3 plies)     │
  │           models opponent as playing optimally               │
  │                                                              │
  │  Cost:   27,000 evaluate() calls instead of ~30             │
  │  Gain:   avoids 2-move traps, spots 3-move tactics           │
  │                                                              │
  └──────────────────────────────────────────────────────────────┘
```
