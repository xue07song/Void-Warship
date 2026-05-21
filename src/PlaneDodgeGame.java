import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class PlaneDodgeGame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private MenuPanel menuPanel;
    private GamePanel gamePanel;

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
    
    public void showGame() {
        gamePanel.resetGame();
        cardLayout.show(mainPanel, "game");
        gamePanel.requestFocusInWindow();
        gamePanel.startGame();
    }
    
    public void showMenu() {
        cardLayout.show(mainPanel, "menu");
        menuPanel.updateHighScore();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PlaneDodgeGame().setVisible(true);
        });
    }
}

class MenuPanel extends JPanel {
    private PlaneDodgeGame game;
    private JLabel highScoreLabel;
    private JComboBox<String> difficultyCombo;
    
    public MenuPanel(PlaneDodgeGame game) {
        this.game = game;
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(20, 20, 40));
        setLayout(new GridBagLayout());
        
        JLabel title = new JLabel("✈ 飞机躲避障碍物 ✈");
        title.setFont(new Font("微软雅黑", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        
        JLabel diffLabel = new JLabel("选择难度：");
        diffLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        diffLabel.setForeground(Color.WHITE);
        String[] difficulties = {"简单 (慢速)", "普通 (中速)", "困难 (快速)"};
        difficultyCombo = new JComboBox<>(difficulties);
        difficultyCombo.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        
        JButton startBtn = new JButton("开始游戏");
        startBtn.setFont(new Font("微软雅黑", Font.BOLD, 24));
        startBtn.setBackground(new Color(0, 150, 0));
        startBtn.setForeground(Color.WHITE);
        startBtn.addActionListener(e -> {
            int diff = difficultyCombo.getSelectedIndex();
            GamePanel.setDifficulty(diff);
            game.showGame();
        });
        
        JButton exitBtn = new JButton("退出游戏");
        exitBtn.setFont(new Font("微软雅黑", Font.BOLD, 20));
        exitBtn.addActionListener(e -> System.exit(0));
        
        int high = loadHighScore();
        highScoreLabel = new JLabel("🏆 最高分: " + high);
        highScoreLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        highScoreLabel.setForeground(Color.YELLOW);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0; gbc.gridy = 0;
        add(title, gbc);
        gbc.gridy = 1;
        add(diffLabel, gbc);
        gbc.gridy = 2;
        add(difficultyCombo, gbc);
        gbc.gridy = 3;
        add(startBtn, gbc);
        gbc.gridy = 4;
        add(highScoreLabel, gbc);
        gbc.gridy = 5;
        add(exitBtn, gbc);
    }
    
    private int loadHighScore() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("score.dat"))) {
            return (int) ois.readObject();
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void updateHighScore() {
        highScoreLabel.setText("🏆 最高分: " + loadHighScore());
    }
}

class GamePanel extends JPanel implements KeyListener, ActionListener {
    private PlaneDodgeGame game;
    private static int speedMs = 500;
    private javax.swing.Timer gameTimer;  // 关键修复：明确指定是 Swing 的 Timer
    
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_SIZE = 40;
    private static final int OBSTACLE_SIZE = 40;
    private static final int BULLET_SIZE = 10;
    
    private int playerX = WIDTH/2 - PLAYER_SIZE/2;
    private int playerY = HEIGHT - PLAYER_SIZE - 10;
    private int lives = 3;
    private int score = 0;
    private boolean gameRunning = true;
    
        private Set<Integer> pressedKeys = new HashSet<>();
    private static final double DIAGONAL_FACTOR = 0.7071067811865475; // 1/√2，斜向速度归一化
    
    private List<Rectangle> obstacles = new ArrayList<>();
    private List<Rectangle> bullets = new ArrayList<>();
    private Random rand = new Random();
    
    public GamePanel(PlaneDodgeGame game) {
        this.game = game;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);
    }
    
    public static void setDifficulty(int diff) {
        switch(diff) {
            case 0: speedMs = 800; break;
            case 1: speedMs = 500; break;
            case 2: speedMs = 300; break;
            default: speedMs = 500;
        }
    }
    
    public void resetGame() {
        gameRunning = true;
        lives = 3;
        score = 0;
        playerX = WIDTH/2 - PLAYER_SIZE/2;
        playerY = HEIGHT - PLAYER_SIZE - 10;
        obstacles.clear();
        bullets.clear();
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }
    
    public void startGame() {
        gameTimer = new javax.swing.Timer(speedMs, this);
        gameTimer.start();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;
        
        // 移动障碍物
        Iterator<Rectangle> itObs = obstacles.iterator();
        while (itObs.hasNext()) {
            Rectangle ob = itObs.next();
            ob.y += 40;
            if (ob.y >= HEIGHT) {
                itObs.remove();
                score += 10;
            }
        }
        
        // 移动子弹
        Iterator<Rectangle> itBul = bullets.iterator();
        while (itBul.hasNext()) {
            Rectangle b = itBul.next();
            b.y -= 20;
            if (b.y + BULLET_SIZE < 0) {
                itBul.remove();
            }
        }
        
        // 子弹碰撞障碍物
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Rectangle bullet = bullets.get(i);
            for (int j = obstacles.size() - 1; j >= 0; j--) {
                Rectangle ob = obstacles.get(j);
                if (bullet.intersects(ob)) {
                    obstacles.remove(j);
                    bullets.remove(i);
                    score += 20;
                    break;
                }
            }
        }
        
        // 生成新障碍物
        int prob = (speedMs <= 400) ? 12 : 20;
        if (rand.nextInt(prob) == 0) {
            int x = rand.nextInt(WIDTH - OBSTACLE_SIZE);
            obstacles.add(new Rectangle(x, 0, OBSTACLE_SIZE, OBSTACLE_SIZE));
        }
        
                // 碰撞检测
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);
        Iterator<Rectangle> itColl = obstacles.iterator();
        boolean damaged = false;
        while (itColl.hasNext()) {
            Rectangle ob = itColl.next();
            if (playerRect.intersects(ob)) {
                itColl.remove();
                damaged = true;
            }
        }
        
        if (damaged) {
            lives--;
            if (lives <= 0) {
                gameRunning = false;
                gameTimer.stop();
                int high = loadHighScore();
                if (score > high) {
                    saveHighScore(score);
                }
                JOptionPane.showMessageDialog(this, "游戏结束！得分: " + score);
                game.showMenu();
                return;
            }
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 障碍物（红色方块）
        g.setColor(Color.RED);
        for (Rectangle ob : obstacles) {
            g.fillRect(ob.x, ob.y, OBSTACLE_SIZE, OBSTACLE_SIZE);
        }
        
        // 子弹（黄色圆形）
        g.setColor(Color.YELLOW);
        for (Rectangle b : bullets) {
            g.fillOval(b.x, b.y, BULLET_SIZE, BULLET_SIZE);
        }
        
                // 飞机（绿色三角形）
        g.setColor(Color.GREEN);
        int[] xPoints = {
            playerX + PLAYER_SIZE/2,
            playerX + PLAYER_SIZE - 5,
            playerX + 5
        };
        int[] yPoints = {
            playerY,
            playerY + PLAYER_SIZE - 5,
            playerY + PLAYER_SIZE - 5
        };
        g.fillPolygon(xPoints, yPoints, 3);
        
        // UI文字
        g.setColor(Color.WHITE);
        g.setFont(new Font("微软雅黑", Font.BOLD, 20));
        g.drawString("❤️ 生命: " + lives, 20, 40);
        g.drawString("⭐ 得分: " + score, 20, 80);
        g.drawString("🏆 最高分: " + loadHighScore(), 20, 120);
        g.drawString("🎮 操作: [WASD/方向键]移动  [J/空格]射击", 20, 160);
    }
    
                @Override
    public void keyPressed(KeyEvent e) {
        if (!gameRunning) return;
        int code = e.getKeyCode();
        // 射击逻辑保持不变
        if (code == KeyEvent.VK_J || code == KeyEvent.VK_SPACE) {
            int bulletX = playerX + PLAYER_SIZE/2 - BULLET_SIZE/2;
            bullets.add(new Rectangle(bulletX, playerY - 10, BULLET_SIZE, BULLET_SIZE));
            repaint();
            return;
        }
        // 方向键加入集合
        pressedKeys.add(code);
        updateMovement();
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        // 方向键从集合移除
        if (isDirectionKey(code)) {
            pressedKeys.remove(code);
            updateMovement();
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    private boolean isDirectionKey(int code) {
        return code == KeyEvent.VK_W || code == KeyEvent.VK_UP ||
               code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN ||
               code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT ||
               code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT;
    }
    
    private void updateMovement() {
        int step = 15;
        boolean moveUp = pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_UP);
        boolean moveDown = pressedKeys.contains(KeyEvent.VK_S) || pressedKeys.contains(KeyEvent.VK_DOWN);
        boolean moveLeft = pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_LEFT);
        boolean moveRight = pressedKeys.contains(KeyEvent.VK_D) || pressedKeys.contains(KeyEvent.VK_RIGHT);
        
        int dx = 0;
        int dy = 0;
        if (moveLeft) dx -= step;
        if (moveRight) dx += step;
        if (moveUp) dy -= step;
        if (moveDown) dy += step;
        
        // 斜向移动时速度归一化
        if (dx != 0 && dy != 0) {
            dx = (int) Math.round(dx * DIAGONAL_FACTOR);
            dy = (int) Math.round(dy * DIAGONAL_FACTOR);
        }
        
        playerX = Math.max(0, Math.min(WIDTH - PLAYER_SIZE, playerX + dx));
        playerY = Math.max(0, Math.min(HEIGHT - PLAYER_SIZE, playerY + dy));
        
        repaint();
    }
    
    private int loadHighScore() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("score.dat"))) {
            return (int) ois.readObject();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private void saveHighScore(int highScore) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("score.dat"))) {
            oos.writeObject(highScore);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}