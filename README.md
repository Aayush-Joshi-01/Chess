# Chess

A complete Java chess application with a polished animated Swing GUI, full rule enforcement, and six progressively stronger AI difficulty levels.

---

## Features

- Full chess rule enforcement (castling, en passant, pawn promotion, check, checkmate, stalemate)
- Drag-and-drop or click-to-click piece movement with smooth animations
- 6 AI difficulty levels using distinct real-world chess engine algorithms
- Opening book covering 10+ common chess openings
- Move history panel with Standard Algebraic Notation (SAN)
- Captured pieces panel with material advantage display
- Board flip (play from Black's perspective)
- Undo move (Ctrl+Z), New Game (Ctrl+N), Flip Board (F)
- Unicode piece rendering with optional PNG piece image support

---

## Module Overview

```
src/com/chess/
├── engine/
│   ├── Alliance.java              — WHITE/BLACK enum with direction helpers
│   ├── Chessv2.java               — Application entry point
│   ├── board/
│   │   ├── Board.java             — Immutable board state (Builder pattern)
│   │   ├── BoardUtils.java        — Column/row arrays, coordinate helpers, SAN
│   │   ├── Move.java              — Move hierarchy (MajorMove, AttackMove, Castle, Pawn variants)
│   │   └── Tile.java              — EmptyTile / OccupiedTile
│   ├── pieces/
│   │   ├── Piece.java             — Abstract base (PieceType enum with material values)
│   │   ├── Pawn.java              — Pawn with en passant and promotion
│   │   ├── Knight.java
│   │   ├── Bishop.java
│   │   ├── Rook.java
│   │   ├── Queen.java
│   │   └── King.java
│   └── player/
│       ├── Player.java            — Abstract player (check/checkmate/stalemate detection)
│       ├── WhitePlayer.java       — Castling logic for White
│       ├── BlackPlayer.java       — Castling logic for Black
│       ├── MoveStatus.java        — DONE / ILLEGAL_MOVE / LEAVES_PLAYER_IN_CHECK
│       ├── MoveTransition.java    — Result of makeMove()
│       └── ai/
│           ├── BoardEvaluator.java             — Evaluation interface
│           ├── StandardBoardEvaluator.java     — PST + material + structure
│           ├── MoveOrdering.java               — MVV-LVA, killer moves, history heuristic
│           ├── ZobristHasher.java              — 64-bit Zobrist hashing
│           ├── TranspositionTable.java         — Fixed-size TT (1M entries)
│           ├── OpeningBook.java                — Common opening repertoire
│           ├── AIThinkTank.java                — SwingWorker wrapper
│           ├── MoveStrategy.java               — Strategy interface
│           ├── RandomMoveStrategy.java         — Level 1 — Random
│           ├── GreedyStrategy.java             — Level 2 — Greedy (depth 1)
│           ├── MiniMaxStrategy.java            — Level 3 — MiniMax (depth 3)
│           ├── AlphaBetaStrategy.java          — Level 4 — Alpha-Beta (depth 4)
│           ├── IterativeDeepeningStrategy.java — Level 5 — ID + TT (2s budget)
│           └── AdvancedAlphaBetaStrategy.java  — Level 6 — Full engine
└── gui/
    ├── Table.java             — Main JFrame singleton, game loop
    ├── BoardPanel.java        — 8×8 grid with drag-and-drop and animations
    ├── TilePanel.java         — Individual tile: rendering + legal move dots
    ├── GameHistoryPanel.java  — SAN move log (scrollable table)
    ├── TakenPiecesPanel.java  — Captured pieces + material advantage
    ├── GameSetup.java         — New game dialog (human/AI, difficulty)
    └── art/                   — Optional PNG piece images (white_king.png, etc.)
```

---

## Chess Rules Implemented

| Rule | Status |
|------|--------|
| Basic piece movement (all 6 pieces) | ✅ |
| Pawn single advance | ✅ |
| Pawn double advance from starting rank | ✅ |
| Pawn diagonal capture | ✅ |
| En passant | ✅ |
| Pawn promotion (Q/R/B/N choice in GUI, Q for AI) | ✅ |
| Kingside castling | ✅ |
| Queenside castling | ✅ |
| Cannot castle through check or while in check | ✅ |
| Check detection | ✅ |
| Checkmate detection | ✅ |
| Stalemate detection | ✅ |
| Cannot move into check | ✅ |

---

## AI Difficulty Levels

### Level 1 — Beginner (Random)
Picks a uniformly random legal move. No evaluation.

### Level 2 — Novice (Greedy)
Evaluates all legal moves at depth 1 and picks the best immediate score. No lookahead.

### Level 3 — Intermediate (MiniMax)
Classic minimax algorithm without pruning, fixed depth 3. Explores all possible game trees separately for MAX and MIN players.

### Level 4 — Advanced (Alpha-Beta)
Negamax with alpha-beta pruning at depth 4. Prunes branches that cannot affect the final result, enabling much deeper search. Includes MVV-LVA move ordering for better pruning efficiency.

### Level 5 — Expert (Iterative Deepening + Transposition Table)
Searches increasing depths (1, 2, 3…) within a 2-second time budget. Uses:
- **Zobrist hashing** for fast board fingerprinting
- **Transposition table** (1M entry hash table) to avoid re-searching identical positions
- **Aspiration windows** (±50cp) to narrow the search window
- **Killer move heuristic** and **history heuristic** for move ordering

### Level 6 — Master (Full Engine)
All Level 5 techniques plus:
- **Null-move pruning**: detects when skipping a move still exceeds beta (R=2 reduction)
- **Late Move Reduction (LMR)**: reduces depth for quiet moves ordered late in the list
- **Quiescence search**: extends search for captures/promotions at leaf nodes to avoid the horizon effect
- **Futility pruning**: skips moves near the horizon when the static eval cannot reach alpha
- **Delta pruning**: inside quiescence, skips captures that cannot improve alpha

---

## Opening Book

The opening book covers these systems (from either side):

| Opening | Variation |
|---------|-----------|
| 1. e4 e5 | Ruy Lopez (Bb5), Italian Game (Bc4) |
| 1. e4 c5 | Sicilian Defense (Nc3, d4) |
| 1. e4 e6 | French Defense (d4, Nc3) |
| 1. e4 d5 | Scandinavian Defense |
| 1. e4 c6 | Caro-Kann Defense |
| 1. d4 d5 c4 | Queen's Gambit Accepted / Declined |
| 1. d4 Nf6 c4 | King's Indian Defense |
| 1. d4 Nf6 Bf4 | London System |
| 1. e4 e5 f4 | King's Gambit |

The book is active for the first 10 moves (20 half-moves), then the current difficulty engine takes over.

---

## Evaluation Function (`StandardBoardEvaluator`)

Scores positions from White's perspective:

| Component | Description |
|-----------|-------------|
| Material | Pawn=100, Knight=320, Bishop=330, Rook=500, Queen=900 |
| Piece-Square Tables | Positional bonuses/penalties for each piece on each square |
| Mobility | legal move count × 5 centipawns |
| Check bonus | +50 if opponent is in check |
| Checkmate | +10000 + depth (prefers faster mates) |
| Doubled pawns | −30 per doubled pawn file |
| Isolated pawns | −20 per isolated pawn |
| Rook on open file | +25; half-open file: +10 |

Score = whiteScore − blackScore. Negated for Black's turn in negamax.

---

## Architecture

```
  Chessv2 (main)
      │ SwingUtilities.invokeLater
      ▼
  Table (JFrame singleton)
  ├── BoardPanel (8×8 grid)
  │   └── TilePanel ×64 (renders piece, highlights, legal dots)
  ├── GameHistoryPanel (SAN move log)
  └── TakenPiecesPanel (captures + advantage)

  Human click → BoardPanel → MoveFactory.createMove()
             → Player.makeMove() → MoveTransition (DONE)
             → Table notifies TableGameAIWatcher (Observer)
             → AIThinkTank (SwingWorker)
             → MoveStrategy.execute(board)
             → AI move applied → GUI redrawn
```

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+N | New Game |
| Ctrl+Z | Undo Move |
| F | Flip Board |
