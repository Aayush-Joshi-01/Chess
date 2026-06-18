# Level 1 — Random Move Strategy

**File:** `src/com/chess/engine/player/ai/RandomMoveStrategy.java`  
**Strength:** ~200 Elo equivalent  
**Search depth:** None (0 plies)  
**Time per move:** < 1 ms  

---

## Overview

The Random strategy makes no attempt to evaluate the position. It simply collects all legal moves available to the current player and picks one uniformly at random. It exists as the baseline difficulty — a player who has never seen chess before.

---

## How It Works at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                     RANDOM MOVE STRATEGY                        │
└─────────────────────────────────────────────────────────────────┘

  Current Board
       │
       ▼
  ┌─────────────────────────────────────────────────────────┐
  │  Get ALL legal moves (30 on average in a midgame)       │
  │                                                         │
  │  [e2-e4] [Ng1-f3] [d2-d4] [Nb1-c3] [Bf1-e2] ...      │
  └─────────────────────────────────────────────────────────┘
       │
       ▼
  ┌─────────────────────────────────────────────────────────┐
  │  Shuffle the list at random                             │
  │                                                         │
  │  [Ng1-f3] [d2-d4] [Bf1-e2] [e2-e4] [Nb1-c3] ...      │
  └─────────────────────────────────────────────────────────┘
       │
       ▼
  ┌─────────────────┐
  │  Return [0]     │  ← Ng1-f3 (this move)
  └─────────────────┘

  Each move has equal probability:  P(move) = 1 / N
  With 30 moves:                    P(move) = 3.3%
```

---

## Algorithm

```
function RandomMove(board):
    moves ← board.currentPlayer().getLegalMoves()
    shuffle(moves)                  // Fisher-Yates shuffle
    return moves[0]
```

No tree search, no evaluation, no lookahead. Every legal move has equal probability `1/N` of being selected, where `N` is the number of legal moves.

---

## Java Implementation

```java
// RandomMoveStrategy.java
public final class RandomMoveStrategy implements MoveStrategy {

    @Override
    public Move execute(final Board board) {
        // Collect all legal moves for the current player
        final List<Move> moves =
                new ArrayList<>(board.currentPlayer().getLegalMoves());

        // No moves → already in checkmate/stalemate (GUI handles this,
        // but guard against it defensively)
        if (moves.isEmpty()) {
            return Move.NULL_MOVE;
        }

        // Fisher-Yates shuffle via Collections utility
        Collections.shuffle(moves);

        // First element after shuffle is uniformly random
        return moves.get(0);
    }

    @Override
    public String getStrategyName() {
        return "Random";
    }
}
```

### Sequence Diagram: Random Move Execution

```
  GUI / AIThinkTank          RandomMoveStrategy           Board / Player
        │                            │                          │
        │── execute(board) ─────────►│                          │
        │                            │── getLegalMoves() ──────►│
        │                            │◄─ ImmutableList<Move> ───│
        │                            │                          │
        │                            │── new ArrayList(moves)   │
        │                            │── Collections.shuffle()  │
        │                            │── return moves.get(0)    │
        │                            │                          │
        │◄── Move ───────────────────│                          │
        │                            │                          │
        │── board.makeMove(move) ────────────────────────────►  │
        │◄── MoveTransition ─────────────────────────────────   │
```

---

## Decision Space Visualised

```
                    All legal moves (N = 28 here)
                    ┌──────────────────────────────┐
                    │  ● ● ● ● ● ● ● ● ● ● ● ● ● ●│
                    │  ● ● ● ● ● ● ● ● ● ● ● ● ● ●│
                    └──────────────────────────────┘
                           ↑ all equally likely

  Random             Greedy           AlphaBeta (L4)     Advanced (L6)
  ┌────────┐         ┌────────┐       ┌────────┐         ┌────────┐
  │●●●●●●●●│         │      ●●│       │        │         │        │
  │●●●●●●●●│         │       ●│       │        │         │       ●│
  │●●●●●●●●│         │      ●●│       │       ●│         │       ●│
  │●●●●●●●●│         │●       │       │      ●●│         │      ●●│
  └────────┘         └────────┘       └────────┘         └────────┘
  All moves equal   Best immediate  Searches 4-ply    Searches 8-12 ply
```

---

## Comparison With Doing Nothing

```
  Strategy      Looks ahead?   Evaluates?   Material-aware?   King-safe?
  ──────────────────────────────────────────────────────────────────────
  None            ✗              ✗              ✗               ✗
  Random (L1)     ✗              ✗              ✗               ✓ *
  Greedy (L2)     1 ply          ✓              ✓               ✓

  * king safety enforced by the legal move generator, not the AI
```

---

## Implementation Details

### Move Collection
`board.currentPlayer().getLegalMoves()` returns an `ImmutableList<Move>` built during board construction. This list already includes all special moves (castling, en passant, promotions) because they are added at the `Player` level, not just the piece level.

```
  ImmutableList<Move> structure for a typical opening position:
  ┌─────────────────────────────────────────────────────────────┐
  │  Index │ Move      │ Type              │ Source             │
  │  ──────┼───────────┼───────────────────┼──────────────────  │
  │    0   │ a2-a3     │ PawnMove          │ Pawn.legalMoves()  │
  │    1   │ a2-a4     │ PawnJump          │ Pawn.legalMoves()  │
  │    2   │ b2-b3     │ PawnMove          │ Pawn.legalMoves()  │
  │   ...  │ ...       │ ...               │ ...                │
  │   19   │ Ng1-f3    │ MajorMove         │ Knight.legalMoves()│
  │   20   │ Ng1-h3    │ MajorMove         │ Knight.legalMoves()│
  │   ...  │ ...       │ ...               │ ...                │
  │   28   │ O-O       │ KingSideCastle    │ Player.castles()   │
  └─────────────────────────────────────────────────────────────┘
  Note: castles only appear once king & rook isFirstMove = true
```

### Shuffle
`Collections.shuffle(legalMoves)` uses Java's default `Random` (seeded from `System.nanoTime()`), giving each game a different move sequence. The list is copied to an `ArrayList` first since `ImmutableList` doesn't support in-place shuffling.

```java
// Why we copy to ArrayList first:
// ImmutableList.shuffle() → UnsupportedOperationException!
final List<Move> moves = new ArrayList<>(board.currentPlayer().getLegalMoves());
Collections.shuffle(moves);   // mutates the ArrayList, not the ImmutableList
```

### Edge Case
If `legalMoves` is empty (checkmate or stalemate), `NULL_MOVE` is returned. The GUI detects this state before ever calling the AI, but the guard is there for safety.

```java
// NULL_MOVE sentinel in Move.java
public static final Move NULL_MOVE = new NullMove();

private static final class NullMove extends Move {
    @Override public Board execute() { throw new RuntimeException("Null move!"); }
    @Override public String toString() { return "null"; }
}
```

---

## Weaknesses

- Will throw away queens, hang pieces, ignore checks
- Will castle randomly without strategic reason
- Has no concept of material, development, or king safety
- Occasionally plays "brilliant" moves by accident

---

## Why It's Useful

- Useful for teaching absolute beginners who need a non-threatening opponent
- Useful for testing move generation: if Random plays 1000 games without crashing, the legal move generator is likely correct
- Provides a measurable lower bound on AI strength

---

## Progression to Level 2

```
  What Random is missing that Greedy adds:
  ┌──────────────────────────────────────────────────────────────┐
  │                                                              │
  │  Random: picks move[0] after shuffle — no evaluation        │
  │                                                              │
  │  Greedy: for EACH move, calls StandardBoardEvaluator         │
  │          and picks the move with the best score             │
  │                                                              │
  │  Cost:   30 evaluate() calls instead of 0                   │
  │  Gain:   always captures free pieces, delivers 1-move mates  │
  │                                                              │
  └──────────────────────────────────────────────────────────────┘
```
