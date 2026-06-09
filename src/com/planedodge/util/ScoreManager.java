package com.planedodge.util;

import java.io.*;

/**
 * Thread-safe singleton that manages high-score persistence.
 * Stores scores in a simple binary file via Java serialisation.
 */
public class ScoreManager {

    private static final ScoreManager INSTANCE = new ScoreManager();
    private static final String SCORE_FILE = "score.dat";

    private int cachedHighScore;
    private boolean dirty = true;   // forces re-read next time

    private ScoreManager() {
        load();
    }

    public static ScoreManager getInstance() { return INSTANCE; }

    /** Returns the cached high-score (lazy-loaded). */
    public int getHighScore() {
        if (dirty) load();
        return cachedHighScore;
    }

    /**
     * Attempts to save a new high score.
     * @return true if it was a new record.
     */
    public boolean saveIfNew(int score) {
        int current = getHighScore();
        if (score > current) {
            cachedHighScore = score;
            persist();
            return true;
        }
        return false;
    }

    /** Force a reload from disk. */
    public void invalidateCache() { dirty = true; }

    // ----------------------------------------------------------------
    // Persistence
    // ----------------------------------------------------------------

    private void load() {
        dirty = false;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(SCORE_FILE)))) {
            cachedHighScore = (int) ois.readObject();
        } catch (Exception ignored) {
            cachedHighScore = 0;
        }
    }

    private void persist() {
        File tmpFile = new File(SCORE_FILE + ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
            oos.writeObject(cachedHighScore);
            oos.flush();
            // Atomic rename (works on same filesystem)
            tmpFile.renameTo(new File(SCORE_FILE));
        } catch (Exception e) {
            // fallback: write directly
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(SCORE_FILE)))) {
                oos.writeObject(cachedHighScore);
            } catch (Exception ignored) { }
        }
    }
}
