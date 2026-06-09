package com.planedodge.ui;

import com.planedodge.config.GameConfig;
import com.planedodge.core.PlaneDodgeGame;
import com.planedodge.model.Star;
import com.planedodge.util.ScoreManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.BitSet;

/**
 * The main gameplay panel.
 * Handles rendering, input, collision detection, and all game logic.
 */
public class GamePanel extends JPanel implements KeyListener, ActionListener {

    // ---- Dependencies ----
    private final PlaneDodgeGame game;
    private final ScoreManager scoreManager = ScoreManager.getInstance();

    // ---- Game state ----
    private int playerX;
    private int playerY;
    private int lives;
    private int score;
    private boolean gameRunning;
    private boolean paused;
    private boolean gameOver;
    private boolean newHighScore;

    private long gameStartTime;
    private long totalPausedTime;
    private long pauseStartTime;
    private long invincibleUntil;
    private long lastShotTime;
    private long lastLogicUpdate;
    private long gameOverTime;

    // ---- Difficulty ----
    private int difficultyLevel;          // 0 = easy, 1 = normal, 2 = hard
    private int speedMs;                  // obstacle spawn interval (ms)
    private int obstacleStep;             // obstacle movement per logic tick
    private int playerStep;               // player movement per frame
    private float bulletBaseSpeed;        // bullet base velocity

    // ---- Smooth movement accumulators ----
    private float obstacleMovePerFrame;
    private float bulletMovePerFrame;
    private float obstacleAccumY;
    private float bulletAccumY;

    // ---- Input ----
    private final BitSet pressedKeys = new BitSet(256);

    // ---- Game objects ----
    private final List<Rectangle> obstacles = new ArrayList<>();
    private final List<Rectangle> bullets = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private final Random rand = new Random();

    // ---- Cached collision rect ----
    private final Rectangle playerRect = new Rectangle();

    // ---- Timers ----
    private javax.swing.Timer gameTimer;

    // ---- FPS counter ----
    private int frameCount;
    private int currentFps;
    private long lastFpsTime;

    // ---- Cached colours / fonts ----
    private final Font uiFont            = new Font("微软雅黑", Font.BOLD, 20);
    private final Font pauseFont         = new Font("微软雅黑", Font.BOLD, 48);
    private final Font pauseHintFont     = new Font("微软雅黑", Font.PLAIN, 24);
    private final Font gameOverFont      = new Font("微软雅黑", Font.BOLD, 52);
    private final Font gameOverSubFont   = new Font("微软雅黑", Font.PLAIN, 26);
    private final Font gameOverBtnFont   = new Font("微软雅黑", Font.BOLD, 22);

    // ---- Game-over button rectangles ----
    private final Rectangle restartBtnRect = new Rectangle();
    private final Rectangle menuBtnRect    = new Rectangle();
    private boolean hoverRestart;
    private boolean hoverMenu;

    // ---------- Constants ----------
    private static final int BTN_WIDTH  = 260;
    private static final int BTN_HEIGHT = 60;

    // ============================================================
    // Construction
    // ============================================================

    public GamePanel(PlaneDodgeGame game) {
        this.game = game;
        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        // Mouse support for game-over screen
        MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        initStars();

        // Safety: start with valid values so rendering doesn't NPE
        setDifficulty(0);
        resetGame();
    }

    // ============================================================
    // Difficulty
    // ============================================================

    public void setDifficulty(int diff) {
        difficultyLevel = Math.max(0, Math.min(2, diff));
        int[] p = GameConfig.DIFFICULTY_PRESETS[difficultyLevel];
        speedMs        = p[0];
        obstacleStep   = p[1];
        playerStep     = p[2];
        bulletBaseSpeed = p[3];
    }

    public int getDifficultyLevel() { return difficultyLevel; }

    // ============================================================
    // Lifecycle
    // ============================================================

    /** Reset all state for a new game. */
    public void resetGame() {
        gameRunning    = true;
        paused         = false;
        gameOver       = false;
        newHighScore   = false;
        lives          = 3;
        score          = 0;
        obstacleAccumY = 0f;
        bulletAccumY   = 0f;
        frameCount     = 0;
        currentFps     = 0;
        lastFpsTime    = 0L;
        lastLogicUpdate = 0L;
        lastShotTime   = 0L;
        invincibleUntil = 0L;
        totalPausedTime = 0L;
        pauseStartTime  = 0L;
        gameOverTime    = 0L;
        hoverRestart = false;
        hoverMenu    = false;

        playerX = GameConfig.WINDOW_WIDTH  / 2 - GameConfig.PLAYER_SIZE / 2;
        playerY = GameConfig.WINDOW_HEIGHT - GameConfig.PLAYER_SIZE - 10;
        bullets.clear();
        obstacles.clear();
        pressedKeys.clear();

        // Re-init stars to reset their twinkle phases
        initStars();

        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
    }

    /** Start the game loop. */
    public void startGame() {
        gameStartTime = System.currentTimeMillis();
        calculatePerFrameMovement();
        gameTimer = new javax.swing.Timer(GameConfig.FRAME_INTERVAL_MS, this);
        gameTimer.start();
    }

    // ============================================================
    // Per-frame movement helpers
    // ============================================================

    private void calculatePerFrameMovement() {
        float pxPerMs = (float) obstacleStep / (float) Math.max(speedMs, 50);
        obstacleMovePerFrame = pxPerMs * GameConfig.FRAME_INTERVAL_MS;

        int currentSpeedMs = getCurrentSpeedMs();
        bulletMovePerFrame = (bulletBaseSpeed / Math.max(currentSpeedMs, 80))
                           * GameConfig.FRAME_INTERVAL_MS * 2.0f;
    }

    /** Effective elapsed time (pause-aware). */
    private long effectiveElapsed() {
        return System.currentTimeMillis() - gameStartTime - totalPausedTime;
    }

    // ============================================================
    // Dynamic difficulty (tiered)
    // ============================================================

    private int getCurrentSpeedMs() {
        long sec = effectiveElapsed() / 1000;
        int base = speedMs;
        int reduction = 0;

        if (score > 80) {
            reduction = Math.max(reduction,
                Math.min((score - 80) / 50, 8) * 25);
        }
        if (score > 250) {
            reduction += Math.min((score - 250) / 80, 5) * 15;
        }
        if (sec > 25) {
            reduction = Math.max(reduction,
                Math.min((int)(sec - 25) / 15, 8) * 25);
        }
        if (sec > 70) {
            reduction += Math.min((int)(sec - 70) / 25, 5) * 15;
        }

        int minMs = (difficultyLevel == 0) ? 150 :
                    (difficultyLevel == 1) ? 120 : 90;
        return Math.max(minMs, base - reduction);
    }

    private int getCurrentObstacleProb() {
        int base;
        switch (difficultyLevel) {
            case 0:  base = 22; break;
            case 1:  base = 16; break;
            default: base = 12;
        }
        long sec = effectiveElapsed() / 1000;
        int reduction = 0;

        if (score > 60)  reduction += (score - 60)  / 80;
        if (score > 200) reduction += (score - 200) / 120;
        if (sec > 20)    reduction  = Math.max(reduction, (int)(sec - 20) / 25);
        if (sec > 80)    reduction += (int)(sec - 80) / 35;

        int min = (difficultyLevel == 0) ? 5 :
                  (difficultyLevel == 1) ? 4 : 3;
        return Math.max(min, base - reduction);
    }

    private float getCurrentSpeedMultiplier() {
        long sec = effectiveElapsed() / 1000;
        float mul = 1.0f;

        if (score > 100) {
            mul += Math.min((score - 100) / 60, 8) * 0.10f;
        }
        if (score > 300) {
            mul += Math.min((score - 300) / 100, 5) * 0.08f;
        }

        float timeMul = 1.0f;
        if (sec > 30) {
            timeMul += Math.min((int)(sec - 30) / 20, 8) * 0.10f;
        }
        if (sec > 90) {
            timeMul += Math.min((int)(sec - 90) / 30, 5) * 0.08f;
        }
        mul = Math.max(mul, timeMul);
        return Math.min(2.2f, mul);
    }

    private boolean shouldSpawnExtra() {
        long sec = effectiveElapsed() / 1000;
        int level = Math.max(score / 100, (int)(sec / 30));

        if (level == 0) return rand.nextFloat() < 0.08f;
        if (level == 1) return rand.nextFloat() < 0.25f;
        if (level == 2) return rand.nextFloat() < 0.40f;
        return rand.nextFloat() < 0.55f;
    }

    private int getExtraSpawnCount() {
        long sec = effectiveElapsed() / 1000;
        int level = Math.max(score / 100, (int)(sec / 30));
        if (level >= 1 && rand.nextFloat() < 0.15f) return 2;
        if (level >= 3 && rand.nextFloat() < 0.20f) return 2;
        return 1;
    }

    /** Compute current difficulty stage index (0..5+) for UI. */
    private int getDifficultyStage() {
        return Math.min(8, Math.max(0,
            Math.max(score / 100, (int)(effectiveElapsed() / 1000 / 30))));
    }

    // ============================================================
    // Stars
    // ============================================================

    private void initStars() {
        stars.clear();
        for (int i = 0; i < GameConfig.STAR_COUNT; i++) {
            stars.add(new Star(
                rand.nextInt(GameConfig.WINDOW_WIDTH),
                rand.nextInt(GameConfig.WINDOW_HEIGHT)
            ));
        }
    }

    // ============================================================
    // Pause
    // ============================================================

    private void togglePause() {
        if (gameOver || !gameRunning) return;
        if (paused) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime;
            paused = false;
        } else {
            pauseStartTime = System.currentTimeMillis();
            pressedKeys.clear();
            paused = true;
        }
    }

    // ============================================================
    // Game-over helpers
    // ============================================================

    private void restartGame() {
        gameOver = false;
        newHighScore = false;
        hoverRestart = false;
        hoverMenu    = false;
        setCursor(Cursor.getDefaultCursor());
        game.showGame(difficultyLevel);
    }

    private void backToMenu() {
        gameOver = false;
        newHighScore = false;
        hoverRestart = false;
        hoverMenu    = false;
        setCursor(Cursor.getDefaultCursor());
        game.showMenu();
    }

    private void handleGameOverClick(int mx, int my) {
        if (!gameOver) return;
        if (restartBtnRect.contains(mx, my)) restartGame();
        else if (menuBtnRect.contains(mx, my)) backToMenu();
    }

    private void handleGameOverHover(int mx, int my) {
        if (!gameOver) return;
        boolean prevR = hoverRestart;
        boolean prevM = hoverMenu;
        hoverRestart = restartBtnRect.contains(mx, my);
        hoverMenu    = menuBtnRect.contains(mx, my);
        if (hoverRestart || hoverMenu) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // ============================================================
    // Timer callback (frame tick)
    // ============================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();

        // ---- Game-over: only animate stars ----
        if (gameOver) {
            updateStars();
            repaint();
            return;
        }
        if (!gameRunning) return;

        // ---- Paused: only animate stars ----
        if (paused) {
            updateStars();
            repaint();
            return;
        }

        // ---- FPS ----
        frameCount++;
        if (now - lastFpsTime >= 1000) {
            currentFps = frameCount;
            frameCount = 0;
            lastFpsTime = now;
        }

        // ---- Player movement (every frame) ----
        updateMovement();

        // ---- Smooth obstacle / bullet movement (every frame) ----
        moveObstaclesSmoothly();
        moveBulletsAndCheckCollision();

        // ---- Logic update (at dynamic interval) ----
        int interval = getCurrentSpeedMs();
        if (now - lastLogicUpdate >= interval) {
            lastLogicUpdate = now;
            updateGameLogic();
        }

        // ---- Stars ----
        updateStars();

        repaint();
    }

    private void updateStars() {
        for (Star s : stars) s.update();
    }

    // ============================================================
    // Smooth movement
    // ============================================================

    private void moveObstaclesSmoothly() {
        if (obstacles.isEmpty()) return;
        obstacleAccumY += obstacleMovePerFrame;
        int step = (int) obstacleAccumY;
        if (step <= 0) return;
        obstacleAccumY -= step;
        for (Rectangle ob : obstacles) ob.y += step;
    }

    /**
     * Move bullets every frame and immediately check collision
     * so no bullet can "skip" over an obstacle.
     */
    private void moveBulletsAndCheckCollision() {
        if (bullets.isEmpty()) return;
        bulletAccumY += bulletMovePerFrame;
        int step = (int) bulletAccumY;
        if (step <= 0) return;
        bulletAccumY -= step;

        Iterator<Rectangle> bit = bullets.iterator();
        while (bit.hasNext()) {
            Rectangle b = bit.next();
            b.y -= step;

            boolean hit = false;
            Iterator<Rectangle> oit = obstacles.iterator();
            while (oit.hasNext()) {
                Rectangle ob = oit.next();
                if (b.intersects(ob)) {
                    oit.remove();           // obstacle destroyed
                    bit.remove();           // bullet consumed
                    score += 20;
                    hit = true;
                    break;
                }
            }
            if (hit) continue;

            // Remove off-screen bullets
            if (b.y + GameConfig.BULLET_SIZE < 0) {
                bit.remove();
            }
        }
    }

    // ============================================================
    // Game logic (spawn, score, collision)
    // ============================================================

    private void updateGameLogic() {
        long now = System.currentTimeMillis();

        // ---- Remove obstacles that left the screen, add score ----
        Iterator<Rectangle> it = obstacles.iterator();
        while (it.hasNext()) {
            Rectangle ob = it.next();
            if (ob.y >= GameConfig.WINDOW_HEIGHT) {
                it.remove();
                score += 10;
            }
        }

        // ---- Spawn obstacles (tiered) ----
        int prob = getCurrentObstacleProb();
        if (rand.nextInt(prob) == 0) {
            obstacles.add(new Rectangle(
                rand.nextInt(GameConfig.WINDOW_WIDTH - GameConfig.OBSTACLE_SIZE),
                0,
                GameConfig.OBSTACLE_SIZE, GameConfig.OBSTACLE_SIZE
            ));
        }
        if (shouldSpawnExtra()) {
            int cnt = getExtraSpawnCount();
            for (int i = 0; i < cnt; i++) {
                obstacles.add(new Rectangle(
                    rand.nextInt(GameConfig.WINDOW_WIDTH - GameConfig.OBSTACLE_SIZE),
                    0,
                    GameConfig.OBSTACLE_SIZE, GameConfig.OBSTACLE_SIZE
                ));
            }
        }

        // ---- Dynamic speed adjustment ----
        float mul = getCurrentSpeedMultiplier();
        int cSpeedMs = getCurrentSpeedMs();
        obstacleMovePerFrame = ((float) obstacleStep / Math.max(cSpeedMs, 50))
                             * GameConfig.FRAME_INTERVAL_MS * mul;
        bulletMovePerFrame = (bulletBaseSpeed / Math.max(cSpeedMs, 80))
                           * GameConfig.FRAME_INTERVAL_MS * 3.0f * mul;

        // ---- Player collision (invincibility window) ----
        if (now > invincibleUntil) {
            updatePlayerRect();
            boolean damaged = false;
            Iterator<Rectangle> oit = obstacles.iterator();
            while (oit.hasNext()) {
                Rectangle ob = oit.next();
                if (playerRect.intersects(ob)) {
                    oit.remove();
                    damaged = true;
                }
            }

            if (damaged) {
                lives--;
                invincibleUntil = now + GameConfig.INVINCIBLE_DURATION;
                if (lives <= 0) {
                    gameRunning = false;
                    gameTimer.stop();
                    newHighScore = scoreManager.saveIfNew(score);
                    gameOver = true;
                    gameOverTime = now;
                }
            }
        }
    }

    /** Keep playerRect synchronised with player position every time it's used. */
    private void updatePlayerRect() {
        playerRect.setBounds(playerX, playerY,
                             GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
    }

    // ============================================================
    // Player movement
    // ============================================================

    private void updateMovement() {
        boolean up    = pressedKeys.get(KeyEvent.VK_W) || pressedKeys.get(KeyEvent.VK_UP);
        boolean down  = pressedKeys.get(KeyEvent.VK_S) || pressedKeys.get(KeyEvent.VK_DOWN);
        boolean left  = pressedKeys.get(KeyEvent.VK_A) || pressedKeys.get(KeyEvent.VK_LEFT);
        boolean right = pressedKeys.get(KeyEvent.VK_D) || pressedKeys.get(KeyEvent.VK_RIGHT);

        int dx = 0, dy = 0;
        if (left)  dx -= playerStep;
        if (right) dx += playerStep;
        if (up)    dy -= playerStep;
        if (down)  dy += playerStep;

        // Diagonal normalisation
        if (dx != 0 && dy != 0) {
            dx = (int) Math.round(dx * GameConfig.DIAGONAL_FACTOR);
            dy = (int) Math.round(dy * GameConfig.DIAGONAL_FACTOR);
        }

        playerX = clamp(playerX + dx, 0, GameConfig.WINDOW_WIDTH  - GameConfig.PLAYER_SIZE);
        playerY = clamp(playerY + dy, 0, GameConfig.WINDOW_HEIGHT - GameConfig.PLAYER_SIZE);
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    // ============================================================
    // Rendering
    // ============================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        // ---- Background ----
        GradientPaint grad = new GradientPaint(
            0, 0, GameConfig.COLOR_BACKGROUND_TOP,
            0, GameConfig.WINDOW_HEIGHT, GameConfig.COLOR_BACKGROUND_BOTTOM);
        g2d.setPaint(grad);
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        // ---- Stars ----
        for (Star s : stars) s.draw(g2d);

        // ---- Obstacles ----
        g2d.setColor(Color.RED);
        for (Rectangle ob : obstacles) {
            g2d.fillRect(ob.x, ob.y, GameConfig.OBSTACLE_SIZE, GameConfig.OBSTACLE_SIZE);
        }

        // ---- Bullets ----
        g2d.setColor(Color.YELLOW);
        for (Rectangle b : bullets) {
            g2d.fillOval(b.x, b.y, GameConfig.BULLET_SIZE, GameConfig.BULLET_SIZE);
        }

        // ---- Player (invincibility blink) ----
        long now = System.currentTimeMillis();
        boolean inv = now < invincibleUntil;
        if (inv && (now / 100) % 2 == 0) {
            g2d.setColor(GameConfig.COLOR_INVINCIBLE);
        } else {
            g2d.setColor(Color.GREEN);
        }
        int[] xp = {
            playerX + GameConfig.PLAYER_SIZE / 2,
            playerX + GameConfig.PLAYER_SIZE - 5,
            playerX + 5
        };
        int[] yp = {
            playerY,
            playerY + GameConfig.PLAYER_SIZE - 5,
            playerY + GameConfig.PLAYER_SIZE - 5
        };
        g2d.fillPolygon(xp, yp, 3);

        // ---- HUD ----
        g2d.setColor(Color.WHITE);
        g2d.setFont(uiFont);
        g2d.drawString("生命: " + lives, 20, 40);
        g2d.drawString("得分: " + score, 20, 80);
        g2d.drawString("最高:  " + scoreManager.getHighScore(), 20, 120);
        g2d.drawString("[WASD/方向键]移动  [J/空格]射击  [P]暂停", 20, 160);

        // ---- Difficulty stage ----
        int stage = getDifficultyStage();
        String stageName;
        if (stage >= 5) stageName = "Master";
        else stageName = GameConfig.DIFFICULTY_NAMES[stage];
        g2d.setColor(new Color(255, 200, 0));
        g2d.drawString("阶段: " + stageName, GameConfig.WINDOW_WIDTH - 220, 80);
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.drawString("障碍物: " + obstacles.size(), GameConfig.WINDOW_WIDTH - 220, 115);
        int cSpd = getCurrentSpeedMs();
        float mul = getCurrentSpeedMultiplier();
        g2d.drawString("间隔: " + cSpd + "ms ×" + String.format("%.1f", mul),
                       GameConfig.WINDOW_WIDTH - 220, 150);

        // ---- FPS ----
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.drawString("FPS: " + currentFps, GameConfig.WINDOW_WIDTH - 120, 30);

        // ---- Overlays ----
        if (paused)  drawPauseOverlay(g2d);
        if (gameOver) drawGameOverOverlay(g2d);

        g2d.dispose();
    }

    // ============================================================
    // Pause overlay
    // ============================================================

    private void drawPauseOverlay(Graphics2D g2d) {
        Composite orig = g2d.getComposite();

        g2d.setComposite(AlphaComposite.SrcOver.derive(0.65f));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        g2d.setComposite(orig);

        // Title
        g2d.setFont(pauseFont);
        g2d.setColor(new Color(255, 215, 0));
        String title = "暂停";
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (GameConfig.WINDOW_WIDTH - fm.stringWidth(title)) / 2;
        int ty = GameConfig.WINDOW_HEIGHT / 2 - 60;
        g2d.drawString(title, tx, ty);

        g2d.setFont(pauseHintFont);
        g2d.setColor(Color.WHITE);
        String hint = "按 P 键继续";
        fm = g2d.getFontMetrics();
        g2d.drawString(hint, (GameConfig.WINDOW_WIDTH - fm.stringWidth(hint)) / 2, ty + 50);

        g2d.setFont(uiFont);
        g2d.setColor(new Color(200, 200, 255));
        String info = "生命: " + lives + "  得分: " + score + "  障碍物: " + obstacles.size();
        fm = g2d.getFontMetrics();
        g2d.drawString(info, (GameConfig.WINDOW_WIDTH - fm.stringWidth(info)) / 2, ty + 100);

        g2d.setComposite(orig);
    }

    // ============================================================
    // Game-over overlay
    // ============================================================

    private void drawGameOverOverlay(Graphics2D g2d) {
        Composite orig = g2d.getComposite();

        // Dark overlay
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.72f));
        g2d.setColor(new Color(10, 0, 0));
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        g2d.setComposite(orig);

        long now = System.currentTimeMillis();
        long elapsed = now - gameOverTime;

        // Title
        g2d.setFont(gameOverFont);
        String title = "游戏结束";
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (GameConfig.WINDOW_WIDTH - fm.stringWidth(title)) / 2;
        int titleY = 140;

        // Shadow
        g2d.setColor(new Color(80, 0, 0));
        g2d.drawString(title, tx + 3, titleY + 3);
        // Blinking main
        boolean blink = (elapsed / 400) % 2 == 0;
        g2d.setColor(blink ? new Color(255, 60, 60) : new Color(255, 120, 80));
        g2d.drawString(title, tx, titleY);

        // Score panel
        int pw = 380, ph = 180;
        int px = (GameConfig.WINDOW_WIDTH - pw) / 2;
        int py = titleY + 20;

        g2d.setComposite(AlphaComposite.SrcOver.derive(0.25f));
        g2d.setColor(new Color(20, 20, 50));
        g2d.fillRoundRect(px, py, pw, ph, 20, 20);
        g2d.setComposite(orig);

        g2d.setColor(new Color(100, 100, 180, 120));
        g2d.drawRoundRect(px, py, pw, ph, 20, 20);

        g2d.setFont(gameOverSubFont);
        FontMetrics fm2 = g2d.getFontMetrics();
        g2d.setColor(Color.WHITE);
        String scoreT = "最终得分: " + score;
        g2d.drawString(scoreT, (GameConfig.WINDOW_WIDTH - fm2.stringWidth(scoreT)) / 2, py + 45);

        g2d.setColor(new Color(255, 215, 0));
        String bestT = "最高纪录: " + scoreManager.getHighScore();
        g2d.drawString(bestT, (GameConfig.WINDOW_WIDTH - fm2.stringWidth(bestT)) / 2, py + 85);

        if (newHighScore) {
            boolean nb = (elapsed / 300) % 2 == 0;
            g2d.setColor(nb ? new Color(255, 255, 0, 230) : new Color(255, 200, 0, 150));
            g2d.setFont(new Font("微软雅黑", Font.BOLD, 28));
            String nr = "新纪录!";
            FontMetrics fm3 = g2d.getFontMetrics();
            g2d.drawString(nr, (GameConfig.WINDOW_WIDTH - fm3.stringWidth(nr)) / 2, py + 135);
        }

        // Buttons
        int btnY = py + ph + 50;
        int spacing = 40;
        int btnX = (GameConfig.WINDOW_WIDTH - BTN_WIDTH * 2 - spacing) / 2;

        restartBtnRect.setBounds(btnX, btnY, BTN_WIDTH, BTN_HEIGHT);
        menuBtnRect.setBounds(btnX + BTN_WIDTH + spacing, btnY, BTN_WIDTH, BTN_HEIGHT);

        drawButton(g2d, restartBtnRect, "再来一次",
                   new Color(0, 160, 60), new Color(0, 200, 80), hoverRestart);
        drawButton(g2d, menuBtnRect, "返回菜单",
                   new Color(160, 60, 0), new Color(200, 80, 0), hoverMenu);

        // Key hints (blinking)
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        g2d.setColor(new Color(255, 255, 255, 130));
        boolean kbBlink = (elapsed / 500) % 2 == 0;
        if (kbBlink) {
            String hint = "[ Enter / R ] 重新开始    [ Esc / M ] 返回菜单";
            fm = g2d.getFontMetrics();
            g2d.drawString(hint, (GameConfig.WINDOW_WIDTH - fm.stringWidth(hint)) / 2,
                           GameConfig.WINDOW_HEIGHT - 40);
        }

        g2d.setComposite(orig);
    }

    private void drawButton(Graphics2D g2d, Rectangle rect, String text,
                            Color normal, Color hover, boolean isHover) {
        Composite orig = g2d.getComposite();

        float alpha = isHover ? 0.95f : 0.80f;
        g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2d.setColor(isHover ? hover : normal);
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 16, 16);
        g2d.setComposite(orig);

        if (isHover) {
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2.5f));
        } else {
            g2d.setColor(new Color(255, 255, 255, 80));
            g2d.setStroke(new BasicStroke(1.5f));
        }
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 16, 16);
        g2d.setStroke(new BasicStroke(1f));

        g2d.setFont(gameOverBtnFont);
        FontMetrics fm = g2d.getFontMetrics();
        int tx = rect.x + (rect.width  - fm.stringWidth(text)) / 2;
        int ty = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;

        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawString(text, tx + 1, ty + 1);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, tx, ty);
    }

    // ============================================================
    // Input
    // ============================================================

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (gameOver) {
            if (code == KeyEvent.VK_R || code == KeyEvent.VK_ENTER) restartGame();
            else if (code == KeyEvent.VK_M || code == KeyEvent.VK_ESCAPE) backToMenu();
            return;
        }
        if (!gameRunning) return;

        // Pause toggle
        if (code == KeyEvent.VK_P) {
            togglePause();
            return;
        }
        if (paused) return;

        // Shoot
        if (code == KeyEvent.VK_J || code == KeyEvent.VK_SPACE) {
            long now = System.currentTimeMillis();
            if (now - lastShotTime >= GameConfig.SHOT_COOLDOWN_MS) {
                bullets.add(new Rectangle(
                    playerX + GameConfig.PLAYER_SIZE / 2 - GameConfig.BULLET_SIZE / 2,
                    playerY - 10,
                    GameConfig.BULLET_SIZE, GameConfig.BULLET_SIZE
                ));
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
        if (isDirectionKey(code)) {
            pressedKeys.clear(code);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    private boolean isDirectionKey(int code) {
        return code == KeyEvent.VK_W || code == KeyEvent.VK_UP  ||
               code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN ||
               code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT ||
               code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT;
    }

    // ============================================================
    // Mouse handler (inner class)
    // ============================================================

    private class MouseHandler extends MouseAdapter implements MouseMotionListener {
        @Override public void mouseClicked(MouseEvent e) { handleGameOverClick(e.getX(), e.getY()); }
        @Override public void mousePressed(MouseEvent e) { handleGameOverClick(e.getX(), e.getY()); }
        @Override public void mouseMoved(MouseEvent e)   { handleGameOverHover(e.getX(), e.getY()); }
        @Override public void mouseDragged(MouseEvent e) { handleGameOverHover(e.getX(), e.getY()); }
    }
}
