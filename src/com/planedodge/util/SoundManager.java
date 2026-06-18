package com.planedodge.util;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple sound effect manager.
 * Supports loading and playing .wav files.
 */
public class SoundManager {

    private static final Map<String, Clip> clips = new HashMap<>();
    private static boolean initialized = false;

    /** 初始化音效管理器，加载所有音效文件 */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // 搜索路径：兼容从 bin/ 或项目根目录运行
        String[] names = {"shoot", "explosion", "hit", "bgm"};
        String[] paths = {"imagines/", "../imagines/"};

        for (String name : names) {
            for (String base : paths) {
                String filePath = base + name + ".wav";
                File f = new File(filePath);
                if (f.exists()) {
                    try {
                        AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                        Clip clip = AudioSystem.getClip();
                        clip.open(ais);
                        clips.put(name, clip);
                    } catch (Exception e) { /* 静默跳过 */ }
                    break;
                }
            }
        }
    }

    /** 播放音效（从头开始播放） */
    public static void play(String name) {
        Clip clip = clips.get(name);
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /** 循环播放背景音乐 */
    public static void playBgm(String name) {
        Clip clip = clips.get(name);
        if (clip == null) return;
        if (clip.isRunning()) return;  // 已经在播放就不重复启动
        clip.stop();
        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    /** 停止指定音效 */
    public static void stop(String name) {
        Clip clip = clips.get(name);
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
    }

    /** 停止所有音效 */
    public static void stopAll() {
        for (Clip clip : clips.values()) {
            clip.stop();
            clip.setFramePosition(0);
        }
    }
}
