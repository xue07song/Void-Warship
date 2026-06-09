package com.planedodge.model;

import com.planedodge.config.GameConfig;

import java.awt.*;
import java.util.Random;

/**
 * A single background star that twinkles.
 */
public class Star {

    private static final Random RAND = new Random();

    private final int x;
    private final int y;
    private final int size;
    private float brightness;
    private float delta;

    public Star(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = RAND.nextInt(3) + 1;          // 1-3 px
        this.brightness = RAND.nextFloat();
        this.delta = RAND.nextBoolean() ? 0.02f : -0.02f;
    }

    /** Advance the twinkle animation one step. */
    public void update() {
        brightness += delta;
        if (brightness <= 0.1f || brightness >= 1.0f) {
            delta = -delta;
        }
    }

    public void draw(Graphics g) {
        int alpha = (int) (brightness * 255);
        alpha = Math.max(0, Math.min(255, alpha));
        g.setColor(GameConfig.starColor(alpha));
        g.fillOval(x, y, size, size);
    }
}
