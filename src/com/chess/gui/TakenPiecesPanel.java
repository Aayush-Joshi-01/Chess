package com.chess.gui;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public final class TakenPiecesPanel extends JPanel {
    private static final Color BG = new Color(30, 30, 30);
    private final JPanel topPanel;
    private final JPanel bottomPanel;
    private final JLabel advantageLabel;

    public TakenPiecesPanel() {
        super(new BorderLayout(0, 2));
        setBackground(BG);
        setPreferredSize(new Dimension(100, 0));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(70, 70, 70)));

        final JLabel title = new JLabel("Captures", SwingConstants.CENTER);
        title.setForeground(new Color(200, 180, 130));
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setOpaque(true);
        title.setBackground(new Color(50, 50, 50));
        title.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        add(title, BorderLayout.NORTH);

        final JPanel center = new JPanel(new GridLayout(2, 1, 0, 2));
        center.setBackground(BG);
        this.topPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 2));
        this.topPanel.setBackground(new Color(35, 35, 35));
        this.topPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                "Black captured", 0, 0, new Font("Segoe UI", Font.PLAIN, 10), Color.GRAY));
        this.bottomPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 2));
        this.bottomPanel.setBackground(new Color(35, 35, 35));
        this.bottomPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                "White captured", 0, 0, new Font("Segoe UI", Font.PLAIN, 10), Color.GRAY));
        center.add(topPanel);
        center.add(bottomPanel);
        add(center, BorderLayout.CENTER);

        this.advantageLabel = new JLabel("", SwingConstants.CENTER);
        advantageLabel.setForeground(Color.WHITE);
        advantageLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        advantageLabel.setOpaque(true);
        advantageLabel.setBackground(new Color(40, 40, 40));
        add(advantageLabel, BorderLayout.SOUTH);
    }

    public void redo(final List<Move> moveHistory) {
        topPanel.removeAll();
        bottomPanel.removeAll();

        final List<Piece> whiteCaptured = new ArrayList<>();
        final List<Piece> blackCaptured = new ArrayList<>();

        for(final Move move : moveHistory) {
            if(move.isAttack() && move.getAttackedPiece() != null) {
                final Piece taken = move.getAttackedPiece();
                if(taken.getPieceAlliance().isWhite()) {
                    blackCaptured.add(taken);
                } else {
                    whiteCaptured.add(taken);
                }
            }
        }

        final Comparator<Piece> byValue = Comparator.comparingInt(Piece::getPieceValue).reversed();
        whiteCaptured.sort(byValue);
        blackCaptured.sort(byValue);

        // Black took white pieces → shown in top panel
        addPieceLabels(topPanel, whiteCaptured, new Color(240, 220, 180));
        // White took black pieces → shown in bottom panel
        addPieceLabels(bottomPanel, blackCaptured, new Color(80, 60, 60));

        final int whiteAdv = whiteCaptured.stream().mapToInt(Piece::getPieceValue).sum();
        final int blackAdv = blackCaptured.stream().mapToInt(Piece::getPieceValue).sum();
        final int diff = whiteAdv - blackAdv;
        if(diff > 0)      advantageLabel.setText("+" + diff + " ♙");
        else if(diff < 0) advantageLabel.setText("+" + (-diff) + " ♟");
        else              advantageLabel.setText("Equal");

        topPanel.revalidate();
        topPanel.repaint();
        bottomPanel.revalidate();
        bottomPanel.repaint();
    }

    private void addPieceLabels(final JPanel panel, final List<Piece> pieces, final Color fg) {
        for(final Piece piece : pieces) {
            final JLabel lbl = new JLabel(pieceSymbol(piece));
            lbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
            lbl.setForeground(fg);
            panel.add(lbl);
        }
    }

    private String pieceSymbol(final Piece piece) {
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

    /** Simple wrap layout helper to flow pieces in available width. */
    private static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }
        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }
        private Dimension layoutSize(final Container target, final boolean preferred) {
            synchronized(target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if(targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                final Insets insets = target.getInsets();
                final int maxWidth = targetWidth - (insets.left + insets.right + getHgap() * 2);
                int x = 0, y = insets.top + getVgap(), rowHeight = 0;
                for(final Component m : target.getComponents()) {
                    final Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if(x == 0 || (x + d.width) <= maxWidth) {
                        x += d.width + getHgap();
                        rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        y += rowHeight + getVgap();
                        x = d.width + getHgap();
                        rowHeight = d.height;
                    }
                }
                y += rowHeight + insets.bottom + getVgap();
                return new Dimension(targetWidth, y);
            }
        }
    }
}
