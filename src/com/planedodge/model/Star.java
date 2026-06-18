package com.planedodge.model;

import com.planedodge.config.GameConfig;

import java.awt.*;
import java.util.Random;

/**
 * A single background star that twinkles with rich visual effects.
 * Supports multiple sizes, colours, twinkle speeds, and cross-shaped glow.
 */
public class Star {

    private static final Random RAND = new Random();

    // ---- Preset colour palette for stars ----
    private static final Color[] STAR_COLORS = {
        new Color(255, 255, 255),  // 纯白
        new Color(200, 220, 255),  // 淡蓝
        new Color(255, 230, 200),  // 淡橙
        new Color(200, 255, 220),  // 淡绿
        new Color(255, 200, 255),  // 淡紫
        new Color(255, 255, 200),  // 淡黄
    };

    private final int x;
    private final int y;
    private final int baseSize;          // 基础大小 1-4 px
    private final Color color;           // 星星颜色
    private final float twinkleSpeed;    // 闪烁速度（每个星星不同）
    private float brightness;            // 当前亮度 0.0 ~ 1.0
    private float delta;                 // 亮度变化步长
    private final boolean hasGlow;       // 是否带光晕（大星星才有）

    public Star(int x, int y) {
        this.x = x;
        this.y = y;
        // 大小分布：大部分小星星，少量大星星
        int r = RAND.nextInt(100);
        if (r < 60)       this.baseSize = 1;   // 60% 小星
        else if (r < 85)  this.baseSize = 2;   // 25% 中星
        else if (r < 97)  this.baseSize = 3;   // 12% 大星
        else              this.baseSize = 4;   //  3% 超大星

        this.color = STAR_COLORS[RAND.nextInt(STAR_COLORS.length)];
        this.brightness = 0.3f + RAND.nextFloat() * 0.7f;
        // 闪烁速度差异化：小星快闪，大星慢闪
        this.twinkleSpeed = 0.01f + RAND.nextFloat() * 0.04f;
        this.delta = RAND.nextBoolean() ? twinkleSpeed : -twinkleSpeed;
        this.hasGlow = baseSize >= 3;
    }

    /** Advance the twinkle animation one step. */
    public void update() {
        brightness += delta;
        if (brightness <= 0.08f || brightness >= 1.0f) {
            delta = -delta;
        }
    }

    public void draw(Graphics g) {
        int alpha = (int) (brightness * 255);
        alpha = Math.max(0, Math.min(255, alpha));

        Graphics2D g2d = (Graphics2D) g;
        Composite orig = g2d.getComposite();

        // ---- 大星星绘制十字光晕 ----
        if (hasGlow && brightness > 0.3f) {
            float glowAlpha = brightness * 0.4f;
            g2d.setComposite(AlphaComposite.SrcOver.derive(glowAlpha));
            g2d.setColor(color);
            int glowSize = baseSize * 3;
            // 水平光晕
            g2d.fillOval(x - (glowSize - baseSize) / 2, y, glowSize, baseSize);
            // 垂直光晕
            g2d.fillOval(x, y - (glowSize - baseSize) / 2, baseSize, glowSize);
        }

        // ---- 星星本体 ----
        g2d.setComposite(AlphaComposite.SrcOver.derive(alpha / 255f));
        g2d.setColor(color);
        int size = brightness > 0.9f ? baseSize + 1 : baseSize;  // 最亮时变大一点
        g2d.fillOval(x, y, size, size);

        g2d.setComposite(orig);
    }
}
