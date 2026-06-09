package com.planedodge.ui;

import com.planedodge.config.GameConfig;
import com.planedodge.core.PlaneDodgeGame;
import com.planedodge.util.ScoreManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 主菜单面板 — 深邃星云背景、脉冲光效、艺术字体标题。
 * 背景特效与游戏内星光不重复：使用缓慢飘浮的光粒子 + 星云渐变。
 */
public class MenuPanel extends JPanel implements ActionListener {

    private final PlaneDodgeGame game;
    private final GameConfig config;
    private JLabel highScoreLabel;
    private JComboBox<String> difficultyCombo;

    // ---- 动画 ----
    private final Timer animTimer;
    private float animTime = 0f;
    private float titleGlowPhase = 0f;

    // ---- 光粒子（替代 MenuStar，避免与游戏内星光闪烁重复） ----
    private final List<LightParticle> particles = new ArrayList<>();

    // ---- 内置随机 ----
    private static final Random RAND = new Random();

    // ============================================================
    // 构造
    // ============================================================

    public MenuPanel(PlaneDodgeGame game) {
        this.game = game;
        this.config = GameConfig.getInstance();

        setPreferredSize(new Dimension(config.getWindowWidth(), config.getWindowHeight()));
        setBackground(GameConfig.COLOR_MENU_BG);
        setLayout(new GridBagLayout());

        initComponents();
        initParticles();

        animTimer = new Timer(30, this);
        animTimer.start();
    }

    // ============================================================
    // 组件初始化
    // ============================================================

    private void initComponents() {
        // ---- 标题（大幅增大字体 + 脉冲光效） ----
        JLabel title = createArtTitle();
        title.setFont(new Font(config.getFontName(), Font.BOLD, 52));
        title.setForeground(Color.WHITE);
        title.setPreferredSize(new Dimension(600, 80));

        // ---- 难度选择 ----
        JLabel diffLabel = new JLabel("— 选择难度 —");
        diffLabel.setFont(new Font(config.getFontName(), Font.PLAIN, 20));
        diffLabel.setForeground(new Color(200, 200, 255));
        diffLabel.setHorizontalAlignment(SwingConstants.CENTER);

        String[] difficulties = {"★ 简单 (慢速)", "★★ 普通 (中速)", "★★★ 困难 (快速)"};
        difficultyCombo = new JComboBox<>(difficulties);
        difficultyCombo.setFont(new Font(config.getFontName(), Font.PLAIN, 20));
        difficultyCombo.setBackground(new Color(30, 30, 60));
        difficultyCombo.setForeground(Color.WHITE);
        difficultyCombo.setPreferredSize(new Dimension(260, 36));
        ((JComponent) difficultyCombo.getRenderer()).setOpaque(false);

        // ---- 开始按钮 ----
        JButton startBtn = createArtButton("▶ 开始游戏",
                new Color(0, 130, 50), new Color(0, 220, 80));
        startBtn.setFont(new Font(config.getFontName(), Font.BOLD, 26));
        startBtn.addActionListener(e -> {
            int diff = difficultyCombo.getSelectedIndex();
            animTimer.stop();
            game.showGame(diff);
        });

        // ---- 退出按钮 ----
        JButton exitBtn = createArtButton("✕ 退出游戏",
                new Color(110, 30, 30), new Color(180, 50, 50));
        exitBtn.setFont(new Font(config.getFontName(), Font.BOLD, 20));
        exitBtn.addActionListener(e -> System.exit(0));

        // ---- 最高分 ----
        int high = ScoreManager.getInstance().getHighScore();
        highScoreLabel = new JLabel("🏆 最高分: " + high);
        highScoreLabel.setFont(new Font(config.getFontName(), Font.BOLD, 22));
        highScoreLabel.setForeground(GameConfig.COLOR_HIGH_SCORE);
        highScoreLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // ---- 布局 ----
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 6, 15);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.CENTER;

        gbc.gridy = 0; gbc.insets.bottom = 25;
        add(title, gbc);
        gbc.gridy = 1; gbc.insets.bottom = 6;
        add(diffLabel, gbc);
        gbc.gridy = 2; gbc.insets.bottom = 12;
        add(difficultyCombo, gbc);
        gbc.gridy = 3; gbc.insets.bottom = 8;
        add(startBtn, gbc);
        gbc.gridy = 4; gbc.insets.bottom = 6;
        add(highScoreLabel, gbc);
        gbc.gridy = 5; gbc.insets.bottom = 12;
        add(exitBtn, gbc);
    }

    /** 面板重新可见时调用 */
    public void onShow() {
        highScoreLabel.setText("🏆 最高分: " + ScoreManager.getInstance().getHighScore());
        animTimer.start();
    }

    // ============================================================
    // 动画循环
    // ============================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        animTime += 0.05f;
        titleGlowPhase = (animTime * 0.03f) % 1.0f;

        for (LightParticle p : particles) p.update();
        repaint();
    }

    // ============================================================
    // 自定义绘制
    // ============================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // ---- 深邃星云背景（红→紫→蓝） ----
        float phase = (float) Math.sin(animTime * 0.08f) * 0.15f;
        Color top    = new Color(10 + (int)(phase * 30), 5, 35);
        Color mid    = new Color(20, 8 + (int)(phase * 20), 50);
        Color bottom = new Color(5, 5, 15);
        g2d.setPaint(new LinearGradientPaint(0, 0, 0, h,
                new float[]{0f, 0.5f, 1f},
                new Color[]{top, mid, bottom}));
        g2d.fillRect(0, 0, w, h);

        // ---- 光粒子（缓慢飘浮，不闪烁） ----
        for (LightParticle p : particles) p.draw(g2d);

        // ---- 星云雾气效果 ----
        drawNebula(g2d, w, h);

        g2d.dispose();
    }

    // ============================================================
    // 星云特效（脉冲光晕，与游戏内星星闪烁完全不同）
    // ============================================================

    private void drawNebula(Graphics2D g2d, int w, int h) {
        Composite orig = g2d.getComposite();

        // 底部蓝色星云
        float nbAlpha = (float) Math.sin(animTime * 0.25f) * 0.08f + 0.18f;
        g2d.setComposite(AlphaComposite.SrcOver.derive(nbAlpha));
        g2d.setColor(new Color(40, 80, 200));
        g2d.fillOval(w / 2 - 250, h - 180, 500, 250);

        // 顶部紫色星云
        float puAlpha = (float) Math.sin(animTime * 0.35f + 1.5f) * 0.06f + 0.14f;
        g2d.setComposite(AlphaComposite.SrcOver.derive(puAlpha));
        g2d.setColor(new Color(120, 40, 180));
        g2d.fillOval(80, -60, 300, 200);

        // 右侧青色星云（缓慢呼吸）
        float cyAlpha = (float) Math.sin(animTime * 0.2f + 2.0f) * 0.05f + 0.10f;
        g2d.setComposite(AlphaComposite.SrcOver.derive(cyAlpha));
        g2d.setColor(new Color(30, 160, 180));
        g2d.fillOval(w - 280, h / 3, 350, 220);

        // 边框装饰（脉冲光边界）
        float borderPhase = (float) Math.sin(animTime * 0.5f) * 0.08f + 0.12f;
        g2d.setComposite(AlphaComposite.SrcOver.derive(borderPhase));
        g2d.setColor(new Color(80, 120, 200));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(12, 12, w - 24, h - 24, 20, 20);
        g2d.setStroke(new BasicStroke(1f));

        g2d.setComposite(orig);
    }

    // ============================================================
    // 艺术按钮（脉冲光晕 + 流光）
    // ============================================================

    private JButton createArtButton(String text, Color normal, Color hover) {
        return new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int bw = getWidth(), bh = getHeight();
                boolean rollover = getModel().isRollover();

                // ---- 外发光（脉冲呼吸） ----
                float pulse = (float) Math.sin(animTime * 0.4f) * 0.12f + 0.15f;
                if (rollover) pulse += 0.1f;
                g2d.setComposite(AlphaComposite.SrcOver.derive(pulse));
                g2d.setColor(rollover ? hover : normal);
                g2d.fillRoundRect(-6, -4, bw + 12, bh + 8, 20, 20);
                g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f));

                // ---- 主体 ----
                g2d.setColor(rollover ? hover : normal);
                g2d.fillRoundRect(0, 0, bw, bh, 14, 14);

                // ---- 高光（上半部分） ----
                g2d.setComposite(AlphaComposite.SrcOver.derive(0.25f));
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(3, 2, bw - 6, bh / 2 - 3, 12, 12);
                g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f));

                // ---- 流光扫描（悬停时） ----
                if (rollover) {
                    float scanPos = (animTime * 80) % (bw + 60) - 30;
                    g2d.setComposite(AlphaComposite.SrcOver.derive(0.20f));
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect((int) scanPos, 0, 40, bh);
                    g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f));
                }

                // ---- 文字（带阴影） ----
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int tx = (bw - fm.stringWidth(text)) / 2;
                int ty = (bh + fm.getAscent() - fm.getDescent()) / 2;
                g2d.setColor(new Color(0, 0, 0, 80));
                g2d.drawString(text, tx + 1, ty + 1);
                g2d.setColor(Color.WHITE);
                g2d.drawString(text, tx, ty);

                g2d.dispose();
            }
        };
    }

    // ============================================================
    // 艺术标题（脉冲彩虹 + 发光 + 大号）
    // ============================================================

    private JLabel createArtTitle() {
        return new JLabel("飞机躲避") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                String text = getText();
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int textW = fm.stringWidth(text);
                int startX = (getWidth() - textW) / 2;
                int y = fm.getAscent() + 8;

                // ---- 外发光（脉冲呼吸） ----
                float glowPulse = (float) Math.sin(animTime * 0.3f) * 0.5f + 0.7f;
                for (int r = 1; r <= 8; r++) {
                    float a = (1.0f - r / 8f) * glowPulse * 0.15f;
                    g2d.setComposite(AlphaComposite.SrcOver.derive(a));
                    g2d.setColor(new Color(255, 200, 100));
                    g2d.drawString(text, startX + r * 2 - 8, y + r * 2 - 8);
                }
                g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f));

                // ---- 逐字彩虹 + 阴影 ----
                int x = startX;
                for (int i = 0; i < text.length(); i++) {
                    char ch = text.charAt(i);
                    float huePhase = (float) i / text.length() + titleGlowPhase;
                    int colorIdx = ((int) (huePhase * GameConfig.TITLE_RAINBOW.length))
                            % GameConfig.TITLE_RAINBOW.length;
                    if (colorIdx < 0) colorIdx += GameConfig.TITLE_RAINBOW.length;

                    // 阴影
                    g2d.setColor(new Color(0, 0, 0, 130));
                    g2d.drawString(String.valueOf(ch), x + 3, y + 3);

                    // 主色
                    g2d.setColor(GameConfig.TITLE_RAINBOW[colorIdx]);
                    g2d.drawString(String.valueOf(ch), x, y);

                    x += fm.charWidth(ch);
                }

                // ---- 底部装饰线 ----
                float lineAlpha = (float) Math.sin(animTime * 0.5f) * 0.15f + 0.4f;
                g2d.setComposite(AlphaComposite.SrcOver.derive(lineAlpha));
                g2d.setColor(new Color(255, 200, 100));
                int lx = (getWidth() - 280) / 2;
                g2d.drawLine(lx, getHeight() - 4, lx + 280, getHeight() - 4);

                g2d.dispose();
            }
        };
    }

    // ============================================================
    // 光粒子系统（缓慢飘浮，非闪烁，与游戏内星光区分）
    // ============================================================

    private void initParticles() {
        for (int i = 0; i < 40; i++) {
            particles.add(new LightParticle(
                    RAND.nextInt(config.getWindowWidth()),
                    RAND.nextInt(config.getWindowHeight()),
                    1.5f + RAND.nextFloat() * 2.5f
            ));
        }
    }

    /**
     * 光粒子 — 缓慢沿斜线飘浮，有淡淡拖尾效果。
     * 与游戏内的 Star（闪烁）完全不同。
     */
    private static class LightParticle {
        float x, y;
        final float size;
        float speedX, speedY;
        float alpha;
        float alphaDelta;

        LightParticle(float x, float y, float size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speedX = -0.2f - RAND.nextFloat() * 0.3f;  // 向左漂
            this.speedY = -0.1f - RAND.nextFloat() * 0.2f;  // 向上漂
            this.alpha = 0.3f + RAND.nextFloat() * 0.5f;
            this.alphaDelta = (RAND.nextBoolean() ? 0.003f : -0.003f);
        }

        void update() {
            x += speedX;
            y += speedY;
            // 边界回卷
            if (x < -10)  x = 810;
            if (y < -10)  y = 610;
            if (x > 810)  x = -10;
            if (y > 610)  y = -10;

            // 缓慢呼吸（非闪烁）
            alpha += alphaDelta;
            if (alpha <= 0.3f || alpha >= 0.8f) alphaDelta = -alphaDelta;
        }

        void draw(Graphics2D g2d) {
            Composite orig = g2d.getComposite();
            g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g2d.setColor(new Color(180, 200, 255));
            g2d.fill(new Ellipse2D.Float(x, y, size, size));
            g2d.setComposite(orig);
        }
    }
}
