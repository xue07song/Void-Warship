import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.BitSet;

public class PlaneDodgeGame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private MenuPanel menuPanel;
    GamePanel gamePanel;

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
            game.gamePanel.setDifficultyLevel(diff); // ★ 记录难度等级
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
        private static int speedMs = 500;        // 障碍物生成间隔（毫秒）
    private static int obstacleStep = 40;    // ★ 障碍物每次移动像素，按难度调整
    private javax.swing.Timer gameTimer;
    
    // ★ 新增：每帧增量移动（实现平滑移动）
    private static final int FRAME_INTERVAL_MS = 8;      // 约 120 FPS 刷新
    private float obstacleMovePerFrame = 0f;              // 障碍物每帧移动像素（浮点累加）
    private float bulletMovePerFrame = 0f;                // 子弹每帧移动像素（浮点累加）
    
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
    
    // 缓存玩家碰撞矩形，避免每帧创建
    private Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);
    
    // 使用BitSet替代HashSet，提升按键检测性能
    private BitSet pressedKeys = new BitSet(256);
    private static final double DIAGONAL_FACTOR = 0.7071067811865475; // 1/√2，斜向速度归一化
    
    private List<Rectangle> obstacles = new ArrayList<>();
    private List<Rectangle> bullets = new ArrayList<>();
    private List<Star> stars = new ArrayList<>();
    private Random rand = new Random();
    
                private int cachedHighScore = -1; // 缓存最高分，避免每帧读文件
    
        // 渲染缓存 - 避免每帧创建字体和颜色对象
    private Font uiFont = new Font("微软雅黑", Font.BOLD, 20);
    private Color greenColor = new Color(0, 255, 0);
    private Color redColor = new Color(255, 0, 0);
    private Color yellowColor = new Color(255, 255, 0);
    // ★ 新增：缓存渐变背景和星星颜色数组，避免每帧创建对象
    private GradientPaint cachedGradient;
    private Color[] starColorCache;  // 预计算星星颜色查找表
    
    private long lastLogicUpdate = 0; // 控制逻辑更新频率
    
    // 射击冷却（毫秒），防止键盘重复触发导致子弹连射
        private long lastShotTime = 0;
    private static final long SHOT_COOLDOWN_MS = 250;
    
    // ★ 新增：帧率计数器
    private long lastFpsTime = 0;
    private int frameCount = 0;
    private int currentFps = 0;
    private boolean showFps = true;  // 可改为 false 隐藏
    
        // ★ 新增：障碍物/子弹的浮点位置累加器（实现亚像素级平滑移动）
    private float obstacleAccumY = 0f;
    private float bulletAccumY = 0f;
    
    // ★ 障碍物随时间增多：记录游戏开始时间，用于动态难度
    private long gameStartTime = 0;
    private int difficultyLevel = 0; // 0=简单, 1=普通, 2=困难
    
    // 无敌时间（被击中后短暂无敌，防止一次性扣完所有生命）
    private long invincibleUntil = 0;
    private static final long INVINCIBLE_DURATION = 1200;
        private static final Color INVINCIBLE_COLOR = new Color(0, 255, 0, 80); // ★ 缓存无敌闪烁颜色
    
    // ★ 暂停功能相关
    private boolean paused = false;              // 是否处于暂停状态
    private long pauseStartTime = 0;             // 暂停开始时间
    private long totalPausedTime = 0;            // 累计暂停时间（毫秒）
    private Font pauseFont = new Font("微软雅黑", Font.BOLD, 48);
    private Font pauseHintFont = new Font("微软雅黑", Font.PLAIN, 24);
    private Color pauseOverlay = new Color(0, 0, 0, 180);  // 半透明黑色遮罩
    
    // 星星类 - 用于星光闪烁特效
    private class Star {
        int x, y;        // 位置
        int size;        // 大小
        float brightness;// 亮度 (0.0f - 1.0f)
        float delta;     // 亮度变化方向 (+/-)
        
        Star(int x, int y) {
            this.x = x;
            this.y = y;
            this.size = rand.nextInt(3) + 1;  // 1-3像素
            this.brightness = rand.nextFloat();
            this.delta = (rand.nextBoolean() ? 0.02f : -0.02f);
        }
        
        void update() {
            brightness += delta;
            // 边界检测，反转方向
            if (brightness <= 0.1f || brightness >= 1.0f) {
                delta = -delta;
            }
        }
        
                void draw(Graphics g) {
            int alpha = (int) (brightness * 255);
            // ★ 使用预缓存颜色表，避免每帧创建 Color 对象
            g.setColor(starColorCache[Math.min(alpha, 255)]);
            g.fillOval(x, y, size, size);
        }
    }
    
                public GamePanel(PlaneDodgeGame game) {
        this.game = game;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);
        // ★ 初始化渲染缓存
        cachedGradient = new GradientPaint(0, 0, new Color(10, 10, 30), 
                                           0, HEIGHT, new Color(0, 0, 10));
        starColorCache = new Color[256];
        for (int i = 0; i < 256; i++) {
            starColorCache[i] = new Color(255, 255, 255, i);
        }
        initStars();
        loadCachedHighScore(); // 预先加载最高分到缓存
    }
    
    // 星星更新整合到主循环，不再需要独立定时器
    
    // 初始化星星背景
    private void initStars() {
        stars.clear();
        for (int i = 0; i < 150; i++) {
            int x = rand.nextInt(WIDTH);
            int y = rand.nextInt(HEIGHT);
            stars.add(new Star(x, y));
        }
    }
    
                                public static void setDifficulty(int diff) {
        switch(diff) {
            case 0: speedMs = 800; obstacleStep = 25; break; // 简单：生成慢，移动慢
            case 1: speedMs = 500; obstacleStep = 40; break; // 普通：适中
            case 2: speedMs = 300; obstacleStep = 55; break; // 困难：生成快，移动快
            default: speedMs = 500; obstacleStep = 40;
        }
        // ★ 重置实例的每帧移动量（由 startGame 重新计算）
    }
    
    // ★ 新增：记录当前难度等级，用于动态难度计算
    public void setDifficultyLevel(int diff) {
        this.difficultyLevel = diff;
    }
    
        // ★ 新增：根据当前 FPS 和 obstacleStep 计算每帧移动量
        private void calculatePerFrameMovement() {
        // obstacleStep 是每次逻辑更新的移动距离，现在要分摊到每帧
        // 假设障碍物每 speedMs 毫秒需要移动 obstacleStep 像素
        // 那么每毫秒移动 obstacleStep / speedMs 像素
        // 每帧移动 (obstacleStep / speedMs) * FRAME_INTERVAL_MS 像素
        float pixelsPerMs = (float)obstacleStep / (float)speedMs;
        obstacleMovePerFrame = pixelsPerMs * FRAME_INTERVAL_MS;
        // 子弹速度也随动态间隔变化（更快刷新间隔 = 每帧移动更多）
        int currentSpeedMs = getCurrentSpeedMs();
        bulletMovePerFrame = (20f / Math.max(currentSpeedMs, 100)) * FRAME_INTERVAL_MS * 1.5f;
    }
    
        // ★ 获取有效的经过时间（扣除暂停时间）
    private long getEffectiveElapsedMs() {
        return System.currentTimeMillis() - gameStartTime - totalPausedTime;
    }
    
    // ★★★ 大幅增强：根据得分和经过时间计算当前障碍物生成概率分母
    private int getCurrentObstacleProb() {
        // 基础概率：按难度设定
        int baseProb;
        switch (difficultyLevel) {
            case 0: baseProb = 25; break;  // 简单
            case 1: baseProb = 20; break;  // 普通
            case 2: baseProb = 12; break;  // 困难
            default: baseProb = 20;
        }
        
                long elapsedSec = getEffectiveElapsedMs() / 1000;
        
        // ★★★ 大幅增强动态障碍物密度 ★★★
        // 每50分概率分母-5（原为每100分-2）
        int scoreReduction = (score / 50) * 5;
        // 每8秒概率分母-2（原为每30秒-1）
        int timeReduction = (int)(elapsedSec / 8) * 2;
        // 结合两种方案，取较大的降低量
        int totalReduction = Math.max(scoreReduction, timeReduction);
        // 最低不低于3（原为6），让后期障碍物非常密集
        return Math.max(3, baseProb - totalReduction);
    }
    
    // ★★★ 大幅增强：根据得分和经过时间计算当前障碍物移动速度倍率
    private float getCurrentSpeedMultiplier() {
                long elapsedSec = getEffectiveElapsedMs() / 1000;
        // 每60分提升 20% 速度（原为每200分8%）
        int scoreBonus = score / 60;
        // 每12秒提升 20%（原为每45秒8%）
        int timeBonus = (int)(elapsedSec / 12);
        int totalBonus = Math.max(scoreBonus, timeBonus);
        // 上限：速度不超过原始的 3.0 倍（原为1.4倍）
        return 1.0f + Math.min(totalBonus, 10) * 0.20f;
    }
    
    // ★★ 新增：根据得分和经过时间计算当前逻辑更新间隔（障碍物生成频率）
    private int getCurrentSpeedMs() {
                long elapsedSec = getEffectiveElapsedMs() / 1000;
        // 基础间隔（按难度原始值）
        int baseMs = speedMs;
        // 每20秒 speedMs 缩减 80ms（最低不低于120ms）
        int timeReduction = (int)(elapsedSec / 20) * 80;
        // 每100分 speedMs 缩减 80ms
        int scoreReduction = (score / 100) * 80;
        int totalReduction = Math.max(timeReduction, scoreReduction);
        return Math.max(120, baseMs - totalReduction);
    }
    
    // ★★ 新增：是否应该生成额外的障碍物（实现多波生成）
    private boolean shouldSpawnExtra() {
                long elapsedSec = getEffectiveElapsedMs() / 1000;
        int scoreThreshold = score / 60; // 每60分增加一级
        int timeThreshold = (int)(elapsedSec / 15); // 每15秒增加一级
        int extraLevel = Math.max(scoreThreshold, timeThreshold);
        // 每1级有约30%概率额外生成一个障碍物，最多90%
        if (extraLevel <= 0) return false;
        float chance = Math.min(0.90f, extraLevel * 0.30f);
        return rand.nextFloat() < chance;
    }
    
                                                                public void resetGame() {
        gameRunning = true;
        lives = 3;
        score = 0;
        playerX = WIDTH/2 - PLAYER_SIZE/2;
        playerY = HEIGHT - PLAYER_SIZE - 10;
        obstacles.clear();
        bullets.clear();
        pressedKeys.clear(); // ★ 重置按键状态
        if (gameTimer != null) {
            gameTimer.stop();
        }
        lastLogicUpdate = 0;
        lastShotTime = 0; // ★ 重置射击冷却
        obstacleAccumY = 0f;   // ★ 重置浮点累加器
        bulletAccumY = 0f;
        lastFpsTime = 0;       // ★ 重置FPS计数器
        frameCount = 0;
        gameStartTime = System.currentTimeMillis(); // ★ 记录游戏开始时间
        // ★ 暂停状态重置
        paused = false;
        totalPausedTime = 0;
        pauseStartTime = 0;
        loadCachedHighScore();
    }
    
                public void startGame() {
        // ★ 提升到约120FPS（8ms），实现更灵敏的操控和更平滑的动画
        calculatePerFrameMovement();  // 根据当前难度计算每帧移动量
        gameTimer = new javax.swing.Timer(FRAME_INTERVAL_MS, this);
        gameTimer.start();
    }
    
                                        // ★★★ 新增：暂停/恢复切换方法
        private void togglePause() {
            if (paused) {
                                // ★ 恢复游戏：累加暂停时间
                                totalPausedTime += System.currentTimeMillis() - pauseStartTime;
                                paused = false;
            } else {
                                // ★ 暂停游戏：记录暂停开始时间
                                pauseStartTime = System.currentTimeMillis();
                                // ★ 暂停时清除所有按键状态，防止恢复后出现"幽灵按键"
                                pressedKeys.clear();
                                paused = true;
            }
        }
    
                                @Override
                                public void actionPerformed(ActionEvent e) {
                    if (!gameRunning) return;
        
                    long now = System.currentTimeMillis();
        
                    // ★ 如果处于暂停状态，只绘制画面（但不更新任何游戏逻辑）
                    if (paused) {
                        // 暂停期间星星继续闪烁，让画面有生机
                        for (Star star : stars) {
                            star.update();
                        }
                        repaint();
                        return;
                    }
        
                    // ★ FPS计数
                    frameCount++;
                    if (now - lastFpsTime >= 1000) {
                        currentFps = frameCount;
                        frameCount = 0;
                        lastFpsTime = now;
                    }
        
                    // ★ 每帧更新飞机移动，实现平滑操控（原本就是这样）
                    updateMovement();
        
        // ★ 核心优化：障碍物和子弹改为每帧增量移动（浮点累加），实现平滑动画
        //   障碍物生成、碰撞检测等逻辑依然按 speedMs 间隔执行
        moveObstaclesSmoothly();
        moveBulletsSmoothly();
        
                // ★ 使用动态间隔执行游戏逻辑更新（生成障碍物、碰撞检测等）
        int currentSpeedMs = getCurrentSpeedMs();
        if (now - lastLogicUpdate >= currentSpeedMs) {
            lastLogicUpdate = now;
            updateGameLogic();
        }
        
        // 每帧更新星星闪烁
        for (Star star : stars) {
            star.update();
        }
        
        repaint();
    }
    
        // ★ 新增：每帧平滑移动障碍物（使用浮点累加器，实现亚像素级平滑移动）
        // 注意：分数累加已在 updateGameLogic 中处理，这里只移动障碍物
        private void moveObstaclesSmoothly() {
            if (obstacles.isEmpty()) return;
            obstacleAccumY += obstacleMovePerFrame;
            int intStep = (int) obstacleAccumY;
            if (intStep <= 0) return;  // 累积不够1像素就不动，保持平滑
            obstacleAccumY -= intStep;
        
            for (Rectangle ob : obstacles) {
                ob.y += intStep;
            }
            // 障碍物移出屏幕的移除和加分统一在 updateGameLogic 中处理
        }
    
    // ★ 新增：每帧平滑移动子弹（使用浮点累加器）
    private void moveBulletsSmoothly() {
        if (bullets.isEmpty()) return;
        bulletAccumY += bulletMovePerFrame;
        int intStep = (int) bulletAccumY;
        if (intStep <= 0) return;
        bulletAccumY -= intStep;
        
        Iterator<Rectangle> it = bullets.iterator();
        while (it.hasNext()) {
            Rectangle b = it.next();
            b.y -= intStep;
            if (b.y + BULLET_SIZE < 0) {
                it.remove();
            }
        }
    }
    
        private void updateGameLogic() {
        // ★ 障碍物和子弹移动已改为每帧平滑移动（moveObstaclesSmoothly/moveBulletsSmoothly）
        // 这里只负责：生成障碍物、子弹碰撞障碍物、玩家碰撞检测
        
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
        
                // ★ 移除移出屏幕的障碍物并加分（统一在逻辑更新中处理）
                Iterator<Rectangle> itRemove = obstacles.iterator();
                while (itRemove.hasNext()) {
                    Rectangle ob = itRemove.next();
                    if (ob.y >= HEIGHT) {
                        itRemove.remove();
                        score += 10;
                    }
                }
        
                                // ★★★ 生成新障碍物 — 动态概率，大幅增强
                int prob = getCurrentObstacleProb();
                // ★ 主障碍物生成（每次逻辑更新至少生成1个的概率大幅提升）
                if (rand.nextInt(prob) == 0) {
                    int x = rand.nextInt(WIDTH - OBSTACLE_SIZE);
                    obstacles.add(new Rectangle(x, 0, OBSTACLE_SIZE, OBSTACLE_SIZE));
                }
                // ★★ 额外障碍物生成（多波生成，得分/时间越高越容易生成多个）
                if (shouldSpawnExtra()) {
                    // 有时生成第2个，有时生成第2+3个
                    int extraCount = rand.nextBoolean() ? 1 : 2;
                    for (int e = 0; e < extraCount; e++) {
                        int x = rand.nextInt(WIDTH - OBSTACLE_SIZE);
                        obstacles.add(new Rectangle(x, 0, OBSTACLE_SIZE, OBSTACLE_SIZE));
                    }
                }
        
                                // ★★★ 动态调整障碍物移动速度（每帧移动量）— 使用动态speedMs
                float speedMultiplier = getCurrentSpeedMultiplier();
                int currentSpeedMs = getCurrentSpeedMs();
                float pixelsPerMs = (float)obstacleStep / (float)currentSpeedMs;
                obstacleMovePerFrame = pixelsPerMs * FRAME_INTERVAL_MS * speedMultiplier;
                // ★ 子弹速度也随之动态提升
                bulletMovePerFrame = (20f / Math.max(currentSpeedMs, 100)) * FRAME_INTERVAL_MS * 1.5f;
        
                // 碰撞检测 - 无敌期间不受伤害
        long now = System.currentTimeMillis();
        if (now > invincibleUntil) {
            playerRect.setBounds(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);
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
                invincibleUntil = now + INVINCIBLE_DURATION; // 进入无敌状态
                if (lives <= 0) {
                    gameRunning = false;
                    gameTimer.stop();
                    if (score > cachedHighScore) {
                        saveHighScore(score);
                    }
                    JOptionPane.showMessageDialog(this, "游戏结束！得分: " + score);
                    game.showMenu();
                    return;
                }
            }
        }
    }
    
        @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // ★ 绘制渐变背景（使用缓存，避免每帧创建对象）
        ((Graphics2D)g).setPaint(cachedGradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 绘制星星背景
        for (Star star : stars) {
            star.draw(g);
        }
        
        // 障碍物（红色方块）
        g.setColor(redColor);
        for (Rectangle ob : obstacles) {
            g.fillRect(ob.x, ob.y, OBSTACLE_SIZE, OBSTACLE_SIZE);
        }
        
        // 子弹（黄色圆形）
        g.setColor(yellowColor);
        for (Rectangle b : bullets) {
            g.fillOval(b.x, b.y, BULLET_SIZE, BULLET_SIZE);
        }
        
                // 飞机（绿色三角形，无敌时闪烁半透明）
                long now = System.currentTimeMillis();
                boolean invincible = now < invincibleUntil;
                if (invincible && (now / 100) % 2 == 0) {
                    g.setColor(INVINCIBLE_COLOR);  // ★ 使用缓存颜色
                } else {
                    g.setColor(greenColor);
                }
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
      
                // UI文字 - 使用缓存的最高分和字体
        g.setColor(Color.WHITE);
        g.setFont(uiFont);
        g.drawString("❤️ 生命: " + lives, 20, 40);
        g.drawString("⭐ 得分: " + score, 20, 80);
        g.drawString("🏆 最高分: " + cachedHighScore, 20, 120);
        g.drawString("🎮 操作: [WASD/方向键]移动  [J/空格]射击  [P]暂停", 20, 160);
                // ★ 显示当前难度等级（让玩家感知动态难度）
        long elapsedSec = getEffectiveElapsedMs() / 1000;
        int diffLevel = Math.min(10, Math.max(score / 80, (int)(elapsedSec / 15)));
        g.setColor(new Color(255, 200, 0));
        String diffStars = "";
        for (int i = 0; i < diffLevel && i < 5; i++) diffStars += "★";
        if (diffLevel > 5) diffStars += "+";
        g.drawString("🔥 难度: " + (diffLevel == 0 ? "★" : diffStars), WIDTH - 200, 80);
        // ★ 显示障碍物数量（让玩家感知压力）
        g.setColor(new Color(255, 255, 255, 150));
        g.drawString("障碍物: " + obstacles.size(), WIDTH - 200, 120);
                // ★ FPS显示（只显示在右上角，方便调试）
        if (showFps) {
            g.setColor(new Color(255, 255, 255, 100));
            g.drawString("FPS: " + currentFps, WIDTH - 120, 30);
        }
        
        // ★★★ 暂停状态绘制半透明遮罩和暂停提示
        if (paused) {
            drawPauseOverlay(g);
        }
    }
    
    // ★★★ 新增：绘制暂停遮罩
    private void drawPauseOverlay(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        // 保存原始合成规则
        Composite originalComposite = g2d.getComposite();
        
        // 1. 半透明黑色遮罩覆盖整个画面
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.65f));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 2. 恢复合成模式绘制文字
        g2d.setComposite(originalComposite);
        
        // 3. 绘制暂停标题（居中大号、金色、闪烁效果）
        g2d.setFont(pauseFont);
        g2d.setColor(new Color(255, 215, 0));  // 金色
        String pauseTitle = "⏸ 游戏暂停";
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (WIDTH - fm.stringWidth(pauseTitle)) / 2;
        int titleY = HEIGHT / 2 - 60;
        g2d.drawString(pauseTitle, titleX, titleY);
        
        // 4. 绘制提示文字（白色，稍小字体）
        g2d.setFont(pauseHintFont);
        g2d.setColor(Color.WHITE);
        String hintText = "按  P  键继续游戏";
        fm = g2d.getFontMetrics();
        int hintX = (WIDTH - fm.stringWidth(hintText)) / 2;
        g2d.drawString(hintText, hintX, titleY + 50);
        
        // 5. 绘制当前游戏状态信息
        g2d.setFont(uiFont);
        g2d.setColor(new Color(200, 200, 255));
        String info1 = "❤️ 生命: " + lives + "    ⭐ 得分: " + score;
        String info2 = "🚧 障碍物数量: " + obstacles.size() + "    🏆 最高分: " + cachedHighScore;
        fm = g2d.getFontMetrics();
        int info1X = (WIDTH - fm.stringWidth(info1)) / 2;
        int info2X = (WIDTH - fm.stringWidth(info2)) / 2;
        g2d.drawString(info1, info1X, titleY + 100);
        g2d.drawString(info2, info2X, titleY + 135);
        
        // 6. 底部提示
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        g2d.setColor(new Color(255, 255, 255, 120));
        String footText = "[ 按 P 键暂停或继续  ]";
        fm = g2d.getFontMetrics();
        int footX = (WIDTH - fm.stringWidth(footText)) / 2;
        g2d.drawString(footText, footX, HEIGHT - 30);
    }
    
                                    
                @Override
    public void keyPressed(KeyEvent e) {
        if (!gameRunning) return;
        int code = e.getKeyCode();
        
        // ★★★ 暂停切换：P 键（无论是否暂停状态都响应）
        if (code == KeyEvent.VK_P) {
            togglePause();
            return;
        }
        
        // ★ 暂停期间所有其他按键无效
        if (paused) return;
        
        // 射击（带冷却控制，防止键盘重复触发生成大量子弹）
        if (code == KeyEvent.VK_J || code == KeyEvent.VK_SPACE) {
            long now = System.currentTimeMillis();
            if (now - lastShotTime >= SHOT_COOLDOWN_MS) {
                int bulletX = playerX + PLAYER_SIZE/2 - BULLET_SIZE/2;
                bullets.add(new Rectangle(bulletX, playerY - 10, BULLET_SIZE, BULLET_SIZE));
                lastShotTime = now;
            }
            return;
        }
        if (isDirectionKey(code)) {
            pressedKeys.set(code);
        }
    }
    
        @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        // 方向键清除位（BitSet使用clear方法）
        // ★ 暂停时也允许清除方向键，确保恢复后状态干净
        if (isDirectionKey(code)) {
            pressedKeys.clear(code);
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
        int step = 12;  // ★ 120FPS下步长12 ≈ 60FPS下步长24的等效速度，但更平滑
        boolean moveUp = pressedKeys.get(KeyEvent.VK_W) || pressedKeys.get(KeyEvent.VK_UP);
        boolean moveDown = pressedKeys.get(KeyEvent.VK_S) || pressedKeys.get(KeyEvent.VK_DOWN);
        boolean moveLeft = pressedKeys.get(KeyEvent.VK_A) || pressedKeys.get(KeyEvent.VK_LEFT);
        boolean moveRight = pressedKeys.get(KeyEvent.VK_D) || pressedKeys.get(KeyEvent.VK_RIGHT);
        
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
    }
    
    private void loadCachedHighScore() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("score.dat"))) {
            cachedHighScore = (int) ois.readObject();
        } catch (Exception e) {
            cachedHighScore = 0;
        }
    }
    
    private void saveHighScore(int highScore) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("score.dat"))) {
            oos.writeObject(highScore);
            cachedHighScore = highScore; // 更新缓存
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}