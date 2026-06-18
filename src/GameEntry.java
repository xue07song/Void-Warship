/**
 * Thin entry-point that delegates to the refactored package version.
 * Compile with: javac -encoding UTF-8 GameEntry.java
 */
public class GameEntry {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(
            () -> new com.planedodge.core.PlaneDodgeGame().setVisible(true));
    }
}
