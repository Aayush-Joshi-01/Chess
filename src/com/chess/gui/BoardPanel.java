package com.chess.gui;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.Piece;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class BoardPanel extends JPanel {
    private final List<TilePanel> boardTiles = new ArrayList<>();
    private final Table table;

    // State for drag-and-drop / click-to-click
    private Tile sourceTile = null;
    private Tile destinationTile = null;
    private Piece humanMovedPiece = null;
    private Point dragPoint = null;
    private Image dragImage = null;

    // Animation state
    private Move lastMove = null;
    private Move animatingMove = null;
    private float animProgress = 0f;
    private javax.swing.Timer animTimer;

    static final Color LIGHT_TILE = new Color(240, 217, 181);
    static final Color DARK_TILE  = new Color(181, 136, 99);
    static final Color SELECT_COLOR  = new Color(246, 246, 105, 180);
    static final Color LAST_MOVE     = new Color(205, 210, 106, 140);
    static final Color CHECK_COLOR   = new Color(220, 60, 60, 180);
    static final Color LEGAL_DOT     = new Color(0, 0, 0, 80);
    static final Color LEGAL_RING    = new Color(0, 0, 0, 80);

    public BoardPanel(final Table table) {
        super(new GridLayout(8, 8));
        this.table = table;
        setBackground(new Color(25, 25, 25));
        setBorder(BorderFactory.createLineBorder(new Color(100, 80, 60), 3));

        for(int i = 0; i < 64; i++){
            final TilePanel tile = new TilePanel(this, i);
            boardTiles.add(tile);
            add(tile);
        }

        // Global mouse listeners for drag-and-drop
        final MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)) {
                    final int tileId = getTileAt(e.getPoint());
                    if(tileId < 0) return;
                    handleTileClick(tileId);
                } else if(SwingUtilities.isRightMouseButton(e)) {
                    clearSelection();
                }
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                if(dragImage != null && SwingUtilities.isLeftMouseButton(e)) {
                    final int tileId = getTileAt(e.getPoint());
                    if(tileId >= 0 && sourceTile != null) {
                        handleDropOn(tileId);
                    }
                    dragImage = null;
                    dragPoint = null;
                    repaint();
                }
            }
        };
        final MouseMotionAdapter motionHandler = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                if(humanMovedPiece != null && dragImage != null) {
                    dragPoint = e.getPoint();
                    repaint();
                }
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(motionHandler);
    }

    private void handleTileClick(final int tileId) {
        final Board board = table.getGameBoard();
        if(isHumanTurn()) {
            if(sourceTile == null) {
                // First click — select piece
                final Tile clicked = board.getTile(tileId);
                if(clicked.isTileOccupied() &&
                        clicked.getPiece().getPieceAlliance() == board.currentPlayer().getAlliance()) {
                    sourceTile = clicked;
                    humanMovedPiece = clicked.getPiece();
                    // Prepare drag image
                    dragImage = TilePanel.getPieceImage(humanMovedPiece, getTileSize());
                    repaint();
                }
            } else {
                // Second click: if user clicks their own piece, change selection instead of attempting move
                final Tile clicked = board.getTile(tileId);
                if(clicked.isTileOccupied() &&
                        clicked.getPiece().getPieceAlliance() == board.currentPlayer().getAlliance()) {
                    sourceTile = clicked;
                    humanMovedPiece = clicked.getPiece();
                    dragImage = TilePanel.getPieceImage(humanMovedPiece, getTileSize());
                    repaint();
                } else {
                    handleDropOn(tileId);
                }
            }
        }
    }

    private void handleDropOn(final int tileId) {
        if(sourceTile == null || humanMovedPiece == null) return;
        final Board board = table.getGameBoard();
        destinationTile = board.getTile(tileId);

        final Move move = Move.MoveFactory.createMove(board,
                sourceTile.getTileCoordinate(), destinationTile.getTileCoordinate());

        if(move != Move.NULL_MOVE) {
            // Check for promotion
            final Move finalMove = handlePromotion(move, board);
            final com.chess.engine.player.MoveTransition transition = board.currentPlayer().makeMove(finalMove);
            if(transition.getMoveStatus().isDone()) {
                table.setGameBoard(transition.getTransitionBoard());
                lastMove = finalMove;
                if(table.isAnimationEnabled()) {
                    animateMove(finalMove, transition.getTransitionBoard());
                } else {
                    drawBoard(table.getGameBoard());
                    table.updateAfterMove(finalMove);
                }
            }
        }
        clearSelection();
    }

    private Move handlePromotion(final Move move, final Board board) {
        if(move instanceof Move.PawnPromotion promo) {
            final String[] options = {"Queen ♛", "Rook ♜", "Bishop ♝", "Knight ♞"};
            final int choice = JOptionPane.showOptionDialog(table.getGameFrame(),
                    "Promote pawn to:", "Pawn Promotion",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            if(choice > 0) {
                final com.chess.engine.Alliance alliance = humanMovedPiece.getPieceAlliance();
                final int pos = move.getDestinationCoordinate();
                final com.chess.engine.pieces.Piece promotionPiece = switch(choice) {
                    case 1 -> new com.chess.engine.pieces.Rook(alliance, pos, false);
                    case 2 -> new com.chess.engine.pieces.Bishop(alliance, pos, false);
                    case 3 -> new com.chess.engine.pieces.Knight(alliance, pos, false);
                    default -> new com.chess.engine.pieces.Queen(alliance, pos, false);
                };
                promo.setPromotionPiece(promotionPiece);
            }
            return promo;
        }
        return move;
    }

    private void clearSelection() {
        sourceTile = null;
        destinationTile = null;
        humanMovedPiece = null;
        dragImage = null;
        dragPoint = null;
        repaint();
    }

    private void animateMove(final Move move, final Board newBoard) {
        if(animTimer != null && animTimer.isRunning()) animTimer.stop();
        animatingMove = move;
        animProgress = 0f;
        animTimer = new javax.swing.Timer(16, e -> { // ~60fps
            animProgress += 0.1f;
            if(animProgress >= 1.0f) {
                animProgress = 1.0f;
                animatingMove = null;
                ((javax.swing.Timer)e.getSource()).stop();
                drawBoard(newBoard);
                table.updateAfterMove(move);
            }
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        // Drag image overlay drawn after children
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Draw dragged piece
        if(dragImage != null && dragPoint != null) {
            final int size = getTileSize();
            g2.drawImage(dragImage, dragPoint.x - size / 2, dragPoint.y - size / 2, size, size, null);
        }
    }

    public void drawBoard(final Board board) {
        removeAll();
        boardTiles.clear();
        final boolean flipped = table.isBoardFlipped();
        for(int i = 0; i < 64; i++){
            final int tileId = flipped ? 63 - i : i;
            final TilePanel tile = new TilePanel(this, tileId);
            boardTiles.add(tile);
            add(tile);
        }
        validate();
        repaint();
    }

    public Collection<Move> getLegalMovesForPiece(final Piece piece) {
        if(piece == null) return Collections.emptyList();
        // Use the player's full legal move set (includes castle moves) filtered by this piece,
        // rather than piece.calculatedLegalMoves() which never contains castle moves.
        final java.util.List<Move> moves = new java.util.ArrayList<>();
        for(final Move m : table.getGameBoard().currentPlayer().getLegalMoves()) {
            if(m.getMovedPiece().equals(piece)) moves.add(m);
        }
        return moves;
    }

    int getTileSize() {
        return Math.min(getWidth(), getHeight()) / 8;
    }

    private int getTileAt(final Point p) {
        final int size = getTileSize();
        if(size == 0) return -1;
        final int col = p.x / size;
        final int row = p.y / size;
        if(col < 0 || col >= 8 || row < 0 || row >= 8) return -1;
        return table.isBoardFlipped() ? 63 - (row * 8 + col) : row * 8 + col;
    }

    private boolean isHumanTurn() {
        final Board board = table.getGameBoard();
        if(board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStaleMate()) return false;
        return table.getPlayerType(board.currentPlayer().getAlliance()) == GameSetup.PlayerType.HUMAN;
    }

    // Accessors for TilePanel
    Tile getSourceTile() { return sourceTile; }
    Piece getHumanMovedPiece() { return humanMovedPiece; }
    Move getLastMove() { return lastMove; }
    Move getAnimatingMove() { return animatingMove; }
    float getAnimProgress() { return animProgress; }
    Table getTable() { return table; }
}
