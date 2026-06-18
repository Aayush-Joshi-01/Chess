# Changelog

All notable changes to this project are documented here.  
Format: **[Version] — Date — Description**

---

## [1.5.0] — 2024-12-19 — Castling GUI Fix

### Fixed
- **Castling moves not triggering from the GUI** — `BoardPanel.getLegalMovesForPiece()` was calling `piece.calculatedLegalMoves()` which only returns the piece's own moves. Castle moves are added at the `Player` level (in `calculateKingCastles()`), not inside `King.calculatedLegalMoves()`, so no legal-move dots were shown on castling destination squares and the destination click was silently rejected.
  - Fix: filter `board.currentPlayer().getLegalMoves()` by the selected piece instead, which includes castle moves
- **Second click on own piece clears selection** instead of changing it — clicking a different friendly piece while one is already selected now re-selects the new piece rather than clearing the selection and forcing a re-click

### Files Changed
- `src/com/chess/gui/BoardPanel.java`

---

## [1.4.0] — 2024-12-19 — Piece Art

### Added
- `PieceArtGenerator.java` — programmatic generator for all 12 chess piece images using Java `Graphics2D`
  - Staunton-inspired piece silhouettes at 100×100 px with transparent background
  - Radial gradient fill (lighter at top-left, darker at bottom-right) for a 3D look
  - Specular sheen overlay on every piece
  - Drop shadow beneath each shape
  - Pieces: King (cross finial), Queen (5-ball crown), Rook (3 merlons), Bishop (orb + teardrop finial), Knight (horse-head bezier silhouette with eye, nostril, mane), Pawn
  - White palette: cream/gold. Black palette: dark brown with gold outline
- 12 PNG files generated to `src/com/chess/gui/art/`

### Files Changed
- `src/com/chess/gui/art/PieceArtGenerator.java` *(new)*
- `src/com/chess/gui/art/white_king.png` *(new)*
- `src/com/chess/gui/art/white_queen.png` *(new)*
- `src/com/chess/gui/art/white_rook.png` *(new)*
- `src/com/chess/gui/art/white_bishop.png` *(new)*
- `src/com/chess/gui/art/white_knight.png` *(new)*
- `src/com/chess/gui/art/white_pawn.png` *(new)*
- `src/com/chess/gui/art/black_king.png` *(new)*
- `src/com/chess/gui/art/black_queen.png` *(new)*
- `src/com/chess/gui/art/black_rook.png` *(new)*
- `src/com/chess/gui/art/black_bishop.png` *(new)*
- `src/com/chess/gui/art/black_knight.png` *(new)*
- `src/com/chess/gui/art/black_pawn.png` *(new)*

---

## [1.3.0] — 2024-12-19 — GUI

### Added
- **`Table.java`** — Main JFrame singleton; menu bar (New Game, Flip Board, Undo, Exit, Animations); status bar showing whose turn it is, check alerts, and checkmate; keyboard shortcuts (Ctrl+N, Ctrl+Z, F); `TableGameAIWatcher` Observer that triggers AI moves automatically after each human move
- **`BoardPanel.java`** — 8×8 grid with custom `Graphics2D` painting; drag-and-drop piece movement (mousePressed → drag → mouseReleased); click-to-click fallback; legal move dot overlay (filled circle on empty squares, ring on captures); last-move highlight; check glow (red radial gradient on king tile); selection highlight; smooth 60fps slide animation via `javax.swing.Timer`; board flip support
- **`TilePanel.java`** — Per-tile rendering; loads PNG piece images from classpath with Unicode symbol fallback; draws rank/file coordinate labels on board edges; paints 50% opacity ghost on the tile being dragged; promotion chooser dialog (Q/R/B/N buttons)
- **`GameHistoryPanel.java`** — Scrollable `JTable` with columns (Move#, White, Black); auto-scrolls to latest move; dark theme with alternating row colours
- **`TakenPiecesPanel.java`** — Shows captured pieces sorted by value for each side; displays material advantage delta (+N)
- **`GameSetup.java`** — Modal dialog to choose Human/Computer for each side and AI difficulty level (1–6); styled dark theme with colour-coded buttons

### Files Changed
- `src/com/chess/gui/Table.java` *(new)*
- `src/com/chess/gui/BoardPanel.java` *(new)*
- `src/com/chess/gui/TilePanel.java` *(new)*
- `src/com/chess/gui/GameHistoryPanel.java` *(new)*
- `src/com/chess/gui/TakenPiecesPanel.java` *(new)*
- `src/com/chess/gui/GameSetup.java` *(new)*
- `src/com/chess/engine/Chessv2.java` *(updated — launches GUI instead of printing board)*

---

## [1.2.0] — 2024-12-19 — AI Engines

### Added
- **`BoardEvaluator.java`** — Interface defining `int evaluate(Board, int depth)`
- **`StandardBoardEvaluator.java`** — Static position evaluator: material (Pawn=100, Knight=320, Bishop=330, Rook=500, Queen=900), piece-square tables for all 6 piece types, mobility bonus (legal moves × 5), check/checkmate bonuses, doubled/isolated pawn penalties, rook open/half-open file bonus
- **`MoveOrdering.java`** — MVV-LVA capture ordering, 2 killer move slots per depth, history heuristic table (piece-to-square scores); `reset()` called at the start of each search
- **`ZobristHasher.java`** — 64-bit Zobrist hash: 64×12 random `long` keys (one per square×pieceType) XORed together; side-to-move key; statically initialised with a fixed seed for reproducibility
- **`TranspositionTable.java`** — 1M-entry fixed-size hash table (parallel arrays for keys, depths, scores, flags, encoded moves); always-replace scheme; stores EXACT/LOWER/UPPER bounds
- **`OpeningBook.java`** — HashMap of Zobrist hash → candidate move pairs; seeds 10 common openings (Ruy Lopez, Italian, Sicilian, French, Scandinavian, Caro-Kann, Queen's Gambit, King's Indian, London System, King's Gambit); random weighted selection; active for first 20 half-moves
- **`AIThinkTank.java`** — `SwingWorker<Move, Void>` wrapper; runs strategy on a background thread; consults opening book first; fires a `Consumer<Move>` callback on the EDT when done; `strategyForLevel(int)` factory method
- **`MoveStrategy.java`** — Interface: `Move execute(Board)`, `String getStrategyName()`
- **`RandomMoveStrategy.java`** — Level 1: uniform random legal move
- **`GreedyStrategy.java`** — Level 2: depth-1 material maximisation
- **`MiniMaxStrategy.java`** — Level 3: classic minimax (no pruning), depth 3
- **`AlphaBetaStrategy.java`** — Level 4: negamax with alpha-beta pruning, MVV-LVA move ordering, depth 4
- **`IterativeDeepeningStrategy.java`** — Level 5: iterative deepening (1→∞) within 2s budget; transposition table with Zobrist hashing; aspiration windows (±50cp); killer moves + history heuristic
- **`AdvancedAlphaBetaStrategy.java`** — Level 6: everything in Level 5 plus null-move pruning (R=2), late move reduction (LMR, threshold=4), quiescence search with stand-pat and delta pruning, futility pruning at depth 1–2; 3s time budget

### Files Changed
- `src/com/chess/engine/player/ai/BoardEvaluator.java` *(new)*
- `src/com/chess/engine/player/ai/StandardBoardEvaluator.java` *(new)*
- `src/com/chess/engine/player/ai/MoveOrdering.java` *(new)*
- `src/com/chess/engine/player/ai/ZobristHasher.java` *(new)*
- `src/com/chess/engine/player/ai/TranspositionTable.java` *(new)*
- `src/com/chess/engine/player/ai/OpeningBook.java` *(new)*
- `src/com/chess/engine/player/ai/AIThinkTank.java` *(new)*
- `src/com/chess/engine/player/ai/MoveStrategy.java` *(new)*
- `src/com/chess/engine/player/ai/RandomMoveStrategy.java` *(new)*
- `src/com/chess/engine/player/ai/GreedyStrategy.java` *(new)*
- `src/com/chess/engine/player/ai/MiniMaxStrategy.java` *(new)*
- `src/com/chess/engine/player/ai/AlphaBetaStrategy.java` *(new)*
- `src/com/chess/engine/player/ai/IterativeDeepeningStrategy.java` *(new)*
- `src/com/chess/engine/player/ai/AdvancedAlphaBetaStrategy.java` *(new)*

---

## [1.1.0] — 2024-12-19 — Engine Completion

### Added
- **`isFirstMove` constructor parameter** on all piece classes — pieces placed on the board initially carry `isFirstMove=true`; `movePiece()` passes `false` so moved pieces lose their first-move status
- **En passant** — `Pawn.calculatedLegalMoves()` checks `board.getEnPassantPawn()`; `PawnEnPassantAttackMove.execute()` removes the captured pawn from its actual square (not the destination square)
- **Pawn promotion** — `Move.PawnPromotion` wrapper class; detection in `Pawn.calculatedLegalMoves()` via `isPromotionSquare()`; `setPromotionPiece()` allows GUI to set the chosen piece; AI always promotes to Queen
- **Castling (complete)** — `KingSideCastleMove` and `QueenSideCastleMove` now carry `castleRook`, `castleRookStart`, `castleRookDestination` fields; `execute()` moves both king and rook; both pieces get `isFirstMove=false`
- **`WhitePlayer.calculateKingCastles()`** — fully implemented: checks king and rook `isFirstMove`, intermediate squares empty, intermediate squares not attacked
- **`BlackPlayer.calculateKingCastles()`** — fully implemented (mirror of White at ranks 0/1)
- **Castle moves included in legal move list** — `Player` constructor now appends `calculateKingCastles()` result to `legalMoves`
- **`Board.enPassantPawn` field + `getEnPassantPawn()` getter**
- **`BoardUtils.FIRST_ROW`, `EIGHTH_ROW`** — row arrays for promotion detection
- **`BoardUtils.getPositionAtCoordinate()` and `getCoordinateAtPosition()`** — algebraic notation helpers
- **`Alliance.getOppositeDirection()`** — used by en passant adjacent-pawn detection
- **`Tile.getTileCoordinate()`** — public getter for the protected field
- **`MoveStatus.isDone()`** — made public (was package-private)
- **`MoveTransition.getTransitionBoard()` and `getMove()`** — accessors needed by GUI and AI

### Fixed
- `Pawn.calculatedLegalMoves()`: guard was `if(isValid) continue` — inverted to `if(!isValid) continue`
- `Pawn.calculatedLegalMoves()`: advance condition was `if(isTileOccupied)` — inverted to `if(!isTileOccupied)`
- Pawn diagonal captures now produce `PawnAttackMove` (was `MajorMove`)
- `PawnJump` constructor was `private` — changed to package-accessible

### Files Changed
- `src/com/chess/engine/Alliance.java`
- `src/com/chess/engine/board/Board.java`
- `src/com/chess/engine/board/BoardUtils.java`
- `src/com/chess/engine/board/Move.java`
- `src/com/chess/engine/board/Tile.java`
- `src/com/chess/engine/pieces/Piece.java`
- `src/com/chess/engine/pieces/Pawn.java`
- `src/com/chess/engine/pieces/Knight.java`
- `src/com/chess/engine/pieces/Bishop.java`
- `src/com/chess/engine/pieces/Rook.java`
- `src/com/chess/engine/pieces/Queen.java`
- `src/com/chess/engine/pieces/King.java`
- `src/com/chess/engine/player/Player.java`
- `src/com/chess/engine/player/WhitePlayer.java`
- `src/com/chess/engine/player/BlackPlayer.java`
- `src/com/chess/engine/player/MoveStatus.java`
- `src/com/chess/engine/player/MoveTransition.java`

---

## [1.0.1] — 2024-12-19 — Critical Bug Fixes

### Fixed
- **`King.java:12`** — `{-9, -8 -7, ...}` was missing a comma; `-8 * -7 = 56` so the king had an illegal 56-square offset, crashing on any king move
- **`Board.java` `setMoveMaker()`** — `this.nextMoveMaker = nextMoveMaker` assigned the field to itself (always `null`); board always had `null` next-move maker, crashing at game start
- **`Pawn.java` validity guard** — `if(BoardUtils.isValidTileCoordinate(...)) continue` was inverted; the pawn skipped valid destinations and processed invalid ones, causing `ArrayIndexOutOfBoundsException`
- **`Pawn.java` advance condition** — `if(isTileOccupied) addMove(...)` was inverted; pawns could only advance onto occupied squares, blocking all pawn movement
- **`AttackMove.execute()`** — returned `null`; any capture immediately crashed the game

### Files Changed
- `src/com/chess/engine/pieces/King.java`
- `src/com/chess/engine/board/Board.java`
- `src/com/chess/engine/pieces/Pawn.java`
- `src/com/chess/engine/board/Move.java`

---

## [0.7.0] — 2024-11-03 — Castling Structure (Incomplete)

### Added
- `KingSideCastleMove` and `QueenSideCastleMove` class stubs in `Move.java`
- `WhitePlayer.calculateKingCastles()` skeleton with `null` placeholders

---

## [0.6.0] — 2024-10-17 — Pawn Jump

### Added
- `PawnJump` move class with `execute()` that sets `board.enPassantPawn`

---

## [0.5.0] — 2024-09-23 — Code Reformatting

### Changed
- Consistent formatting across all engine source files

---

## [0.4.0] — 2024-06-23 — Special Move Infrastructure

### Added
- Move subclass hierarchy: `CastleMove`, `PawnEnPassantAttackMove`, `PawnJump` stubs
- `MoveFactory` — creates moves by searching the legal move list

---

## [0.3.0] — 2024-03-03 — Player and Board Foundation

### Added
- `Player`, `WhitePlayer`, `BlackPlayer` — check, checkmate, stalemate detection
- `MoveTransition`, `MoveStatus`
- `Board.Builder` pattern for immutable board construction
- `Piece.isFirstMove` field (hardcoded to `false` — fixed in 1.1.0)

---

## [0.2.0] — 2024-01-15 — All Six Piece Types

### Added
- `Knight`, `Bishop`, `Rook`, `Queen` with sliding/jumping move generation
- `King` single-step move generation
- `Pawn` with single and double advance, diagonal capture structure

---

## [0.1.0] — 2024-01-01 — Initial Engine

### Added
- `Board`, `Tile` (EmptyTile / OccupiedTile), `BoardUtils`
- `Piece` abstract base class with `PieceType` enum
- `Alliance` enum (WHITE / BLACK)
- `Move` base class with `MajorMove` and `AttackMove`
