package com.chess.gui;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.Piece;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public final class TilePanel extends JPanel {
    private final int tileId;
    private final BoardPanel boardPanel;
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();
    private static boolean imagesLoaded = false;

    // Coordinate label font
    private static final Font COORD_FONT = new Font("Segoe UI", Font.BOLD, 9);

    public TilePanel(final BoardPanel boardPanel, final int tileId) {
        super(new GridBagLayout());
        this.tileId = tileId;
        this.boardPanel = boardPanel;
        setOpaque(true);
        assignTileColor();
    }

    private void assignTileColor() {
        final int row = tileId / 8;
        final int col = tileId % 8;
        setBackground((row + col) % 2 == 0 ? BoardPanel.LIGHT_TILE : BoardPanel.DARK_TILE);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        final int w = getWidth(), h = getHeight();
        final Board board = boardPanel.getTable().getGameBoard();
        final Tile tile = board.getTile(tileId);

        drawTileBackground(g2, w, h, board, tile);
        drawCoordinateLabels(g2, w, h);
        drawPiece(g2, w, h, tile);
        drawLegalMoveDots(g2, w, h, board);
    }

    private void drawTileBackground(final Graphics2D g2, final int w, final int h,
                                     final Board board, final Tile tile) {
        // Base color
        assignTileColor();
        g2.setColor(getBackground());
        g2.fillRect(0, 0, w, h);

        // Last move highlight
        final Move lastMove = boardPanel.getLastMove();
        if(lastMove != null) {
            if(tileId == lastMove.getCurrentCoordinate() || tileId == lastMove.getDestinationCoordinate()) {
                g2.setColor(BoardPanel.LAST_MOVE);
                g2.fillRect(0, 0, w, h);
            }
        }

        // Selection highlight
        final Tile sourceTile = boardPanel.getSourceTile();
        if(sourceTile != null && sourceTile.getTileCoordinate() == tileId) {
            g2.setColor(BoardPanel.SELECT_COLOR);
            g2.fillRect(0, 0, w, h);
            // Glow border
            g2.setColor(new Color(200, 200, 0, 120));
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(1, 1, w - 3, h - 3);
        }

        // Check highlight
        if(board.currentPlayer().isInCheck()) {
            final Piece king = board.currentPlayer().getPlayerKing();
            if(king.getPiecePosition() == tileId) {
                drawRadialGlow(g2, w, h, new Color(220, 60, 60, 200));
            }
        }

        // Checkmate highlight
        if(board.currentPlayer().isInCheckMate()) {
            final Piece king = board.currentPlayer().getPlayerKing();
            if(king.getPiecePosition() == tileId) {
                drawRadialGlow(g2, w, h, new Color(180, 20, 20, 220));
            }
        }
    }

    private void drawRadialGlow(final Graphics2D g2, final int w, final int h, final Color color) {
        final RadialGradientPaint glow = new RadialGradientPaint(
                w / 2f, h / 2f, Math.max(w, h) / 2f,
                new float[]{0f, 1f},
                new Color[]{color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, w, h);
    }

    private void drawCoordinateLabels(final Graphics2D g2, final int w, final int h) {
        g2.setFont(COORD_FONT);
        final boolean flipped = boardPanel.getTable().isBoardFlipped();
        final int row = tileId / 8;
        final int col = tileId % 8;
        final Color labelColor = (row + col) % 2 == 0
                ? BoardPanel.DARK_TILE.darker()
                : BoardPanel.LIGHT_TILE;

        // File label on bottom row
        if((!flipped && row == 7) || (flipped && row == 0)) {
            g2.setColor(labelColor);
            g2.drawString(String.valueOf((char)('a' + col)), w - 9, h - 2);
        }
        // Rank label on first column
        if((!flipped && col == 0) || (flipped && col == 7)) {
            g2.setColor(labelColor);
            g2.drawString(String.valueOf(8 - row), 2, 11);
        }
    }

    private void drawPiece(final Graphics2D g2, final int w, final int h, final Tile tile) {
        if(!tile.isTileOccupied()) return;
        final Piece piece = tile.getPiece();
        // Don't draw piece being dragged at its home tile
        final Piece dragged = boardPanel.getHumanMovedPiece();
        final Tile src = boardPanel.getSourceTile();
        if(dragged != null && src != null && src.getTileCoordinate() == tileId) {
            // Draw at 50% opacity to indicate it's being lifted
            final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f);
            g2.setComposite(ac);
        }
        final Image img = getPieceImage(piece, Math.min(w, h));
        if(img != null) {
            final int pad = Math.min(w, h) / 10;
            g2.drawImage(img, pad, pad, w - 2 * pad, h - 2 * pad, null);
        } else {
            drawFallbackPiece(g2, piece, w, h);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawFallbackPiece(final Graphics2D g2, final Piece piece, final int w, final int h) {
        final String symbol = pieceUnicode(piece);
        final Font font = new Font("Segoe UI Symbol", Font.PLAIN, (int)(Math.min(w, h) * 0.65));
        g2.setFont(font);
        final FontMetrics fm = g2.getFontMetrics();
        final int sw = fm.stringWidth(symbol);
        final int sy = (h + fm.getAscent() - fm.getDescent()) / 2;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(symbol, (w - sw) / 2 + 1, sy + 1);
        // Piece
        g2.setColor(piece.getPieceAlliance().isWhite() ? new Color(255, 250, 240) : new Color(20, 20, 20));
        g2.drawString(symbol, (w - sw) / 2, sy);
    }

    private void drawLegalMoveDots(final Graphics2D g2, final int w, final int h, final Board board) {
        final Piece selected = boardPanel.getHumanMovedPiece();
        if(selected == null) return;
        final Collection<Move> legals = boardPanel.getLegalMovesForPiece(selected);
        for(final Move move : legals) {
            if(move.getDestinationCoordinate() != tileId) continue;
            if(move.isAttack()) {
                // Ring around occupied tile
                g2.setColor(BoardPanel.LEGAL_RING);
                g2.setStroke(new BasicStroke(4));
                g2.drawOval(3, 3, w - 7, h - 7);
            } else {
                // Small dot on empty tile
                final int dotSize = Math.min(w, h) / 3;
                final int x = (w - dotSize) / 2;
                final int y = (h - dotSize) / 2;
                g2.setColor(BoardPanel.LEGAL_DOT);
                g2.fill(new Ellipse2D.Float(x, y, dotSize, dotSize));
            }
        }
    }

    static Image getPieceImage(final Piece piece, final int size) {
        ensureImagesLoaded();
        final String key = imageKey(piece);
        final Image img = IMAGE_CACHE.get(key);
        if(img == null) return null;
        return img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }

    private static void ensureImagesLoaded() {
        if(imagesLoaded) return;
        imagesLoaded = true;
        final String[] alliances = {"white", "black"};
        final String[] types     = {"king", "queen", "rook", "bishop", "knight", "pawn"};
        for(final String alliance : alliances) {
            for(final String type : types) {
                final String name = alliance + "_" + type;
                final String path = "/com/chess/gui/art/" + name + ".png";
                try {
                    final InputStream is = TilePanel.class.getResourceAsStream(path);
                    if(is != null) {
                        final BufferedImage img = ImageIO.read(is);
                        IMAGE_CACHE.put(name, img);
                        is.close();
                    }
                } catch(Exception ignored) {
                    // Fall back to Unicode rendering
                }
            }
        }
    }

    private static String imageKey(final Piece piece) {
        final String alliance = piece.getPieceAlliance().isWhite() ? "white" : "black";
        final String type = switch(piece.getPieceType()) {
            case KING   -> "king";
            case QUEEN  -> "queen";
            case ROOK   -> "rook";
            case BISHOP -> "bishop";
            case KNIGHT -> "knight";
            case PAWN   -> "pawn";
        };
        return alliance + "_" + type;
    }

    private static String pieceUnicode(final Piece piece) {
        final boolean white = piece.getPieceAlliance().isWhite();
        return switch(piece.getPieceType()) {
            case KING   -> white ? "♔" : "♚";
            case QUEEN  -> white ? "♕" : "♛";
            case ROOK   -> white ? "♖" : "♜";
            case BISHOP -> white ? "♗" : "♝";
            case KNIGHT -> white ? "♘" : "♞";
            case PAWN   -> white ? "♙" : "♟";
        };
    }

    public int getTileId() { return tileId; }
}
