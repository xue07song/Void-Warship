package com.planedodge.model;

import java.awt.*;
import java.util.Random;

/**
 * A floating star used in the menu background with slow vertical drift.
 */
public class MenuStar {

    private static final Random RAND = new Random();

    private int x;
    private int y;
    private final int size;
    private float brightness;
    private float delta;
    private final int speed;          // vertical drift speed

    private final int worldWidth;
    private final int worldHeight;

    public MenuStar(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.x = RAND.nextInt(worldWidth);
        this.y = RAND.nextInt(worldHeight);
        this.size = RAND.nextInt(3) + 1;
        this.brightness = RAND.nextFloat();
        this.delta = RAND.nextBoolean() ? 0.015f : -0.015f;
        this.speed = RAND.nextInt(2) + 1;
    }

    public void update() {
        // Twinkle
        brightness += delta;
        if (brightness <= 0.1f || brightness >= 1.0f) {
            delta = -delta;
        }
        // Drift
        y += speed;
        if (y > worldHeight) {
            y = -5;
            x = RAND.nextInt(worldWidth);
        }
    }

    public void draw(Graphics g) {
        int alpha = (int) (brightness * 200 + 55);  // 55-255 range
        g.setColor(new Color(255, 255, 255, Math.min(alpha, 255)));
        g.fillOval(x, y, size, size);
    }
}
