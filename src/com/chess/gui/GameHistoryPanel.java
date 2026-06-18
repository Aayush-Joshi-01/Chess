package com.chess.gui;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class GameHistoryPanel extends JPanel {
    private final DataModel model;
    private final JScrollPane scrollPane;
    private static final Color BG = new Color(30, 30, 30);
    private static final Color HEADER_BG = new Color(50, 50, 50);
    private static final Color ROW_BG1 = new Color(40, 40, 40);
    private static final Color ROW_BG2 = new Color(35, 35, 35);
    private static final Color FG = new Color(220, 220, 220);

    public GameHistoryPanel() {
        super(new BorderLayout());
        setBackground(BG);
        setPreferredSize(new Dimension(180, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(70, 70, 70)));

        final JLabel title = new JLabel("Move History", SwingConstants.CENTER);
        title.setForeground(new Color(200, 180, 130));
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setOpaque(true);
        title.setBackground(new Color(50, 50, 50));
        title.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        add(title, BorderLayout.NORTH);

        this.model = new DataModel();
        final JTable table = new JTable(model);
        table.setBackground(ROW_BG1);
        table.setForeground(FG);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setGridColor(new Color(60, 60, 60));
        table.setRowHeight(22);
        table.setSelectionBackground(new Color(80, 120, 180));
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(true);
        table.getTableHeader().setBackground(HEADER_BG);
        table.getTableHeader().setForeground(FG);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Alternating rows
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(80, 120, 180) : (row % 2 == 0 ? ROW_BG1 : ROW_BG2));
                setForeground(FG);
                setFont(new Font("Monospaced", Font.PLAIN, 12));
                return this;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);

        this.scrollPane = new JScrollPane(table);
        scrollPane.setBackground(BG);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    public void redo(final List<Move> moveHistory, final Board board) {
        model.clear();
        int currentRow = 0;
        final List<Row> rows = new ArrayList<>();
        for(int i = 0; i < moveHistory.size(); i++) {
            final Move move = moveHistory.get(i);
            if(i % 2 == 0) {
                rows.add(new Row((i / 2) + 1));
            }
            final String san = toSAN(move, board);
            final Row row = rows.get(rows.size() - 1);
            if(i % 2 == 0) {
                row.setWhiteMove(san);
            } else {
                row.setBlackMove(san);
            }
            // suppress unused variable warning — row mutated above
        }
        for(final Row row : rows) {
            model.addRow(new Object[]{row.moveNumber(), row.whiteMove(), row.blackMove()});
        }
        // Scroll to bottom
        final JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private String toSAN(final Move move, final Board board) {
        return move.toString();
    }

    private static class DataModel extends DefaultTableModel {
        DataModel() {
            super(new String[]{"#", "White", "Black"}, 0);
        }
        public void clear() { setRowCount(0); }
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    }

    private static final class Row {
        private final int moveNumber;
        private String whiteMove = "";
        private String blackMove = "";
        Row(int moveNumber) { this.moveNumber = moveNumber; }
        void setWhiteMove(String s) { this.whiteMove = s; }
        void setBlackMove(String s) { this.blackMove = s; }
        int moveNumber() { return moveNumber; }
        String whiteMove() { return whiteMove; }
        String blackMove() { return blackMove; }
    }
}
