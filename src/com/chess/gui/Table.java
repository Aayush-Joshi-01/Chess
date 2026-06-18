package com.chess.gui;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveStatus;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.ai.AIThinkTank;
import com.chess.engine.player.ai.MoveStrategy;
import com.chess.engine.player.ai.OpeningBook;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("deprecation")
public final class Table extends Observable {
    private final JFrame gameFrame;
    private final BoardPanel boardPanel;
    private final GameHistoryPanel historyPanel;
    private final TakenPiecesPanel takenPanel;
    private final JLabel statusLabel;
    private final JLabel thinkingLabel;

    private Board chessBoard;
    private final List<Move> moveHistory = new ArrayList<>();

    private GameSetup.PlayerType whitePlayerType = GameSetup.PlayerType.HUMAN;
    private GameSetup.PlayerType blackPlayerType = GameSetup.PlayerType.COMPUTER;
    private MoveStrategy aiStrategy;
    private int aiDifficulty = 4;
    private boolean boardFlipped = false;
    private boolean animationEnabled = true;

    private static final Table INSTANCE = new Table();

    private Table() {
        this.chessBoard = Board.createStandardBoard();
        this.aiStrategy = AIThinkTank.strategyForLevel(aiDifficulty);

        this.gameFrame = new JFrame("Chess");
        this.gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.gameFrame.setLayout(new BorderLayout());
        this.gameFrame.getContentPane().setBackground(new Color(25, 25, 25));

        // Panels
        this.historyPanel = new GameHistoryPanel();
        this.takenPanel   = new TakenPiecesPanel();
        this.boardPanel   = new BoardPanel(this);

        this.gameFrame.add(historyPanel, BorderLayout.WEST);
        this.gameFrame.add(boardPanel,   BorderLayout.CENTER);
        this.gameFrame.add(takenPanel,   BorderLayout.EAST);

        // Status bar
        final JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(20, 20, 20));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 70, 70)));
        this.statusLabel = new JLabel("  White's turn", SwingConstants.LEFT);
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        this.thinkingLabel = new JLabel("", SwingConstants.RIGHT);
        thinkingLabel.setForeground(new Color(150, 200, 255));
        thinkingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        thinkingLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(thinkingLabel, BorderLayout.EAST);
        this.gameFrame.add(statusBar, BorderLayout.SOUTH);

        // Menu
        this.gameFrame.setJMenuBar(buildMenuBar());

        // Keyboard shortcuts
        final InputMap im = this.gameFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap am = this.gameFrame.getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        am.put("undo", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { undoMove(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "newGame");
        am.put("newGame", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { showGameSetup(); }
        });
        im.put(KeyStroke.getKeyStroke('f'), "flip");
        am.put("flip", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { flipBoard(); }
        });

        addObserver(new TableGameAIWatcher());

        this.gameFrame.setSize(860, 700);
        this.gameFrame.setMinimumSize(new Dimension(700, 600));
        this.gameFrame.setLocationRelativeTo(null);
    }

    public static Table get() { return INSTANCE; }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            Table.get().gameFrame.setVisible(true);
            Table.get().updateStatusLabel();
        });
    }

    private JMenuBar buildMenuBar() {
        final JMenuBar bar = new JMenuBar();
        bar.setBackground(new Color(45, 45, 45));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(70, 70, 70)));
        bar.add(buildGameMenu());
        bar.add(buildOptionsMenu());
        bar.add(buildHelpMenu());
        return bar;
    }

    private JMenu buildGameMenu() {
        final JMenu menu = styledMenu("Game");
        menu.add(styledItem("New Game (Ctrl+N)", e -> showGameSetup()));
        menu.add(styledItem("Flip Board (F)", e -> flipBoard()));
        menu.addSeparator();
        menu.add(styledItem("Undo Move (Ctrl+Z)", e -> undoMove()));
        menu.addSeparator();
        menu.add(styledItem("Exit", e -> System.exit(0)));
        return menu;
    }

    private JMenu buildOptionsMenu() {
        final JMenu menu = styledMenu("Options");
        final JCheckBoxMenuItem animItem = new JCheckBoxMenuItem("Animations");
        animItem.setSelected(animationEnabled);
        animItem.setForeground(Color.WHITE);
        animItem.setBackground(new Color(55, 55, 55));
        animItem.addActionListener(e -> animationEnabled = animItem.isSelected());
        menu.add(animItem);
        return menu;
    }

    private JMenu buildHelpMenu() {
        final JMenu menu = styledMenu("Help");
        menu.add(styledItem("About", e -> JOptionPane.showMessageDialog(gameFrame,
                "Chess Application\nEngine: Java + Guava\n6 AI Difficulty Levels\n© 2024",
                "About Chess", JOptionPane.INFORMATION_MESSAGE)));
        return menu;
    }

    private JMenu styledMenu(final String text) {
        final JMenu menu = new JMenu(text);
        menu.setForeground(Color.WHITE);
        menu.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        menu.getPopupMenu().setBackground(new Color(55, 55, 55));
        return menu;
    }

    private JMenuItem styledItem(final String text, final java.awt.event.ActionListener action) {
        final JMenuItem item = new JMenuItem(text);
        item.setForeground(Color.WHITE);
        item.setBackground(new Color(55, 55, 55));
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.addActionListener(action);
        return item;
    }

    private void showGameSetup() {
        final GameSetup setup = new GameSetup(gameFrame);
        setup.setVisible(true);
        if(setup.isConfirmed()) {
            whitePlayerType = setup.getWhitePlayerType();
            blackPlayerType = setup.getBlackPlayerType();
            aiDifficulty    = setup.getAIDifficulty();
            aiStrategy      = setup.getAIStrategy();
            resetGame();
        }
    }

    private void resetGame() {
        chessBoard = Board.createStandardBoard();
        moveHistory.clear();
        OpeningBook.get().reset();
        boardPanel.drawBoard(chessBoard);
        historyPanel.redo(moveHistory, chessBoard);
        takenPanel.redo(moveHistory);
        updateStatusLabel();
        setChanged();
        notifyObservers();
    }

    private void flipBoard() {
        boardFlipped = !boardFlipped;
        boardPanel.drawBoard(chessBoard);
    }

    private void undoMove() {
        if(moveHistory.size() >= 2) {
            // Undo both AI move and human move
            moveHistory.remove(moveHistory.size() - 1);
            moveHistory.remove(moveHistory.size() - 1);
        } else if(!moveHistory.isEmpty()) {
            moveHistory.remove(moveHistory.size() - 1);
        }
        // Replay from start
        chessBoard = Board.createStandardBoard();
        for(final Move move : moveHistory) {
            chessBoard = chessBoard.currentPlayer().makeMove(move).getTransitionBoard();
        }
        boardPanel.drawBoard(chessBoard);
        historyPanel.redo(moveHistory, chessBoard);
        takenPanel.redo(moveHistory);
        updateStatusLabel();
    }

    public void updateAfterMove(final Move move) {
        moveHistory.add(move);
        historyPanel.redo(moveHistory, chessBoard);
        takenPanel.redo(moveHistory);
        updateStatusLabel();
        setChanged();
        notifyObservers();
    }

    private void updateStatusLabel() {
        if(chessBoard.currentPlayer().isInCheckMate()) {
            final String winner = chessBoard.currentPlayer().getOpponent().getAlliance().isWhite() ? "White" : "Black";
            statusLabel.setText("  Checkmate! " + winner + " wins! 🏆");
            statusLabel.setForeground(new Color(255, 200, 100));
        } else if(chessBoard.currentPlayer().isInStaleMate()) {
            statusLabel.setText("  Stalemate — Draw!");
            statusLabel.setForeground(new Color(180, 180, 180));
        } else if(chessBoard.currentPlayer().isInCheck()) {
            final String player = chessBoard.currentPlayer().getAlliance().isWhite() ? "White" : "Black";
            statusLabel.setText("  " + player + " is in Check! ⚠");
            statusLabel.setForeground(new Color(255, 100, 100));
        } else {
            final String player = chessBoard.currentPlayer().getAlliance().isWhite() ? "White" : "Black";
            statusLabel.setText("  " + player + "'s turn");
            statusLabel.setForeground(new Color(200, 200, 200));
        }
    }

    // Getters used by BoardPanel and AIWatcher
    public Board getGameBoard() { return chessBoard; }
    public void setGameBoard(final Board board) { this.chessBoard = board; }
    public boolean isBoardFlipped() { return boardFlipped; }
    public boolean isAnimationEnabled() { return animationEnabled; }
    public JFrame getGameFrame() { return gameFrame; }
    public GameSetup.PlayerType getPlayerType(final com.chess.engine.Alliance alliance) {
        return alliance.isWhite() ? whitePlayerType : blackPlayerType;
    }
    public MoveStrategy getAIStrategy() { return aiStrategy; }
    public void setThinkingLabel(final String text) { thinkingLabel.setText(text + "  "); }

    // AI observer watcher
    private class TableGameAIWatcher implements Observer {
        @Override
        public void update(final Observable o, final Object arg) {
            // After each move, check if the current player is AI
            final Board board = getGameBoard();
            if(isAITurn(board)) {
                thinkingLabel.setText("AI thinking...  ");
                final AIThinkTank thinkTank = new AIThinkTank(board, aiStrategy, move -> {
                    if(move != null && move != Move.NULL_MOVE) {
                        final MoveTransition transition = board.currentPlayer().makeMove(move);
                        if(transition.getMoveStatus() == MoveStatus.DONE) {
                            setGameBoard(transition.getTransitionBoard());
                            boardPanel.drawBoard(getGameBoard());
                            updateAfterMove(move);
                        }
                    }
                    thinkingLabel.setText("");
                }, true);
                thinkTank.execute();
            }
        }

        private boolean isAITurn(final Board board) {
            if(board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStaleMate()) return false;
            return getPlayerType(board.currentPlayer().getAlliance()) == GameSetup.PlayerType.COMPUTER;
        }
    }
}
