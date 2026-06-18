package com.chess.engine;
import com.chess.gui.Table;
import javax.swing.SwingUtilities;

public class Chessv2 {
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(Table.get()::show);
    }
}
