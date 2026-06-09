package com.planedodge.core;

import com.planedodge.config.GameConfig;
import com.planedodge.ui.GamePanel;
import com.planedodge.ui.MenuPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window.
 * Hosts a CardLayout switching between MenuPanel and GamePanel.
 */
public class PlaneDodgeGame extends JFrame {

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final MenuPanel menuPanel;
    private final GamePanel gamePanel;

    public PlaneDodgeGame() {
        setTitle("飞机躲避障碍物");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        menuPanel = new MenuPanel(this);
        gamePanel = new GamePanel(this);

        mainPanel.add(menuPanel, "menu");
        mainPanel.add(gamePanel, "game");

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    /** Switch to the game panel with the given difficulty. */
    public void showGame(int difficulty) {
        gamePanel.setDifficulty(difficulty);
        gamePanel.resetGame();
        cardLayout.show(mainPanel, "game");
        gamePanel.requestFocusInWindow();
        gamePanel.startGame();
    }

    /** Switch back to the menu panel. */
    public void showMenu() {
        menuPanel.onShow();
        cardLayout.show(mainPanel, "menu");
    }

    // ----------------------------------------------------------------
    // Entry point
    // ----------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PlaneDodgeGame().setVisible(true));
    }
}
