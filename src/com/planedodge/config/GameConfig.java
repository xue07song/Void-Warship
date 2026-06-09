package com.planedodge.config;

import java.awt.*;

/**
 * Central configuration for the Plane Dodge game.
 * Holds all tuning constants and theme colors.
 */
public class GameConfig {

    private static final GameConfig INSTANCE = new GameConfig();

    // ---- Window ----
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;

    // ---- Rendering ----
    public static final int FRAME_INTERVAL_MS = 8;           // ~120 FPS
    public static final int STAR_COUNT = 150;

    // ---- Player ----
    public static final int PLAYER_SIZE = 40;
    public static final int OBSTACLE_SIZE = 40;
    public static final int BULLET_SIZE = 10;
    public static final long INVINCIBLE_DURATION = 1200;     // ms
    public static final long SHOT_COOLDOWN_MS = 200;
    public static final double DIAGONAL_FACTOR = 0.7071067811865475; // 1/√2

    // ---- Difficulty presets ----
    // Each preset: {spawnIntervalMs, obstacleStep, playerStep, bulletBaseSpeed}
    public static final int[][] DIFFICULTY_PRESETS = {
        {450, 38, 6, 25},    // Easy   - 飞行器每帧移动6像素
        {250, 50, 9, 40},    // Normal - 飞行器每帧移动9像素
        {150, 68, 13, 58}    // Hard   - 飞行器每帧移动13像素
    };

    // ---- Colours (theme) ----
    public static final Color COLOR_BACKGROUND_TOP   = new Color(10, 10, 30);
    public static final Color COLOR_BACKGROUND_BOTTOM = new Color(0, 0, 10);
    public static final Color COLOR_MENU_BG          = new Color(20, 20, 40);
    public static final Color COLOR_HIGH_SCORE       = Color.YELLOW;
    public static final Color COLOR_INVINCIBLE       = new Color(0, 255, 0, 80);
    public static final Color COLOR_GLOW_BLUE        = new Color(30, 60, 140);
    public static final Color COLOR_GLOW_ORANGE      = new Color(180, 80, 30);
    public static final Color COLOR_MENU_BORDER      = new Color(50, 80, 140);

    // ---- Rainbow title colours ----
    public static final Color[] TITLE_RAINBOW = {
        new Color(255, 100, 100),
        new Color(255, 180, 80),
        new Color(255, 255, 80),
        new Color(100, 255, 100),
        new Color(80, 180, 255),
        new Color(180, 100, 255)
    };

    // ---- Star colour lookup table (pre-cached) ----
    private static final Color[] STAR_COLOR_CACHE = new Color[256];
    static {
        for (int i = 0; i < 256; i++) {
            STAR_COLOR_CACHE[i] = new Color(255, 255, 255, i);
        }
    }

    // ---- Difficulty stage labels ----
    public static final String[] DIFFICULTY_NAMES = {
        "★ 新手", "★★ 入门", "★★★ 挑战", "★★★★ 困难", "★★★★★ 地狱"
    };

    // ---- Misc ----
    public static final int MENU_STAR_COUNT = 60;

    private GameConfig() { }

    public static GameConfig getInstance() { return INSTANCE; }

    // ---- Accessors ----
    public int getWindowWidth()  { return WINDOW_WIDTH; }
    public int getWindowHeight() { return WINDOW_HEIGHT; }
    public String getFontName()  { return "微软雅黑"; }
    public int getMenuStarCount() { return MENU_STAR_COUNT; }

    public static Color starColor(int alpha) {
        return STAR_COLOR_CACHE[Math.min(alpha, 255)];
    }

    public static int getDifficultySpeedMs(int diff) {
        return DIFFICULTY_PRESETS[diff][0];
    }

    public static int getDifficultyObstacleStep(int diff) {
        return DIFFICULTY_PRESETS[diff][1];
    }

    public static int getDifficultyPlayerStep(int diff) {
        return DIFFICULTY_PRESETS[diff][2];
    }

    public static int getDifficultyBulletBaseSpeed(int diff) {
        return DIFFICULTY_PRESETS[diff][3];
    }
}
