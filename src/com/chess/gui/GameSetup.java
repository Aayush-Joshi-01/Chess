package com.chess.gui;
import com.chess.engine.player.ai.AIThinkTank;
import com.chess.engine.player.ai.MoveStrategy;
import javax.swing.*;
import java.awt.*;

public final class GameSetup extends JDialog {
    private PlayerType whitePlayerType = PlayerType.HUMAN;
    private PlayerType blackPlayerType = PlayerType.COMPUTER;
    private int aiDifficulty = 4; // Default Level 4 - Alpha-Beta
    private boolean confirmed = false;

    private static final String[] DIFFICULTY_NAMES = {
        "Beginner (Random)", "Novice (Greedy)", "Intermediate (MiniMax)",
        "Advanced (Alpha-Beta)", "Expert (Iterative Deepening)", "Master (Full Engine)"
    };

    public enum PlayerType { HUMAN, COMPUTER }

    public GameSetup(final JFrame parent) {
        super(parent, "Game Setup", true);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);
        getContentPane().setBackground(new Color(40, 40, 40));

        final JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(40, 40, 40));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // White player
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(styledLabel("White:"), gbc);
        final JComboBox<String> whiteCombo = styledCombo(new String[]{"Human", "Computer"});
        whiteCombo.setSelectedIndex(0);
        gbc.gridx = 1;
        mainPanel.add(whiteCombo, gbc);

        // Black player
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(styledLabel("Black:"), gbc);
        final JComboBox<String> blackCombo = styledCombo(new String[]{"Human", "Computer"});
        blackCombo.setSelectedIndex(1);
        gbc.gridx = 1;
        mainPanel.add(blackCombo, gbc);

        // Difficulty
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(styledLabel("AI Difficulty:"), gbc);
        final JComboBox<String> difficultyCombo = styledCombo(DIFFICULTY_NAMES);
        difficultyCombo.setSelectedIndex(3);
        gbc.gridx = 1;
        mainPanel.add(difficultyCombo, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Buttons
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(40, 40, 40));
        final JButton startBtn = new JButton("Start Game");
        styleButton(startBtn, new Color(80, 160, 80));
        final JButton cancelBtn = new JButton("Cancel");
        styleButton(cancelBtn, new Color(120, 60, 60));

        startBtn.addActionListener(e -> {
            whitePlayerType = whiteCombo.getSelectedIndex() == 0 ? PlayerType.HUMAN : PlayerType.COMPUTER;
            blackPlayerType = blackCombo.getSelectedIndex() == 0 ? PlayerType.HUMAN : PlayerType.COMPUTER;
            aiDifficulty    = difficultyCombo.getSelectedIndex() + 1;
            confirmed = true;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(startBtn);
        buttonPanel.add(cancelBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private JLabel styledLabel(final String text) {
        final JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return label;
    }

    private JComboBox<String> styledCombo(final String[] items) {
        final JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(new Color(60, 60, 60));
        combo.setForeground(Color.WHITE);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setPreferredSize(new Dimension(240, 28));
        return combo;
    }

    private void styleButton(final JButton btn, final Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(130, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public boolean isConfirmed() { return confirmed; }
    public PlayerType getWhitePlayerType() { return whitePlayerType; }
    public PlayerType getBlackPlayerType() { return blackPlayerType; }
    public int getAIDifficulty() { return aiDifficulty; }
    public MoveStrategy getAIStrategy() { return AIThinkTank.strategyForLevel(aiDifficulty); }
}
