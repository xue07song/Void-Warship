import java.util.*;
import java.util.concurrent.*;

/**
 * 控制台飞机躲避障碍物游戏
 * 玩法：飞机用 '^' 表示，障碍物用 '#' 表示，按 A/D 后按回车左右移动飞机
 * 每成功躲避一个障碍物（障碍物移出屏幕底部）得 10 分
 * 碰到障碍物游戏结束
 */
public class PlaneDodgeGame {
    // 游戏区域尺寸
    private static final int WIDTH = 20;
    private static final int HEIGHT = 10;      // 飞机固定在底部，所以障碍物活动高度为 HEIGHT-1
    private static final int PLAYER_Y = HEIGHT - 1;  // 飞机固定在最下面一行

    // 游戏状态
    private static int playerX = WIDTH / 2;      // 飞机水平位置
    private static int score = 0;
    private static boolean gameRunning = true;

    // 障碍物列表：每个障碍物是一个(x, y)坐标，y从0开始向下增加
    private static final List<int[]> obstacles = new ArrayList<>();
    private static final Random rand = new Random();

    // 定时器：每 500 毫秒更新一次障碍物位置并重绘
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        System.out.println("=== 飞机躲避障碍物游戏 ===");
        System.out.println("规则：飞机在底部(^)，障碍物(#)从上往下落。");
        System.out.println("按 A (左) / D (右) 然后回车移动飞机，避开障碍物。");
        System.out.println("每成功躲避一个障碍物得10分。碰到障碍物游戏结束。");
        System.out.println("按任意回车开始游戏...");
        new Scanner(System.in).nextLine();

        // 启动游戏定时器：每0.5秒更新并重绘
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new GameUpdater(), 0, 500, TimeUnit.MILLISECONDS);

        // 主线程负责读取玩家输入
        try (Scanner scanner = new Scanner(System.in)) {
            while (gameRunning) {
                String input = scanner.nextLine().trim();
                if (!gameRunning) break;

                // 处理移动
                if (input.equalsIgnoreCase("A")) {
                    if (playerX > 0) playerX--;
                } else if (input.equalsIgnoreCase("D")) {
                    if (playerX < WIDTH - 1) playerX++;
                }
                // 移动后立即重绘（让玩家立刻看到位置变化）
                drawGame();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 游戏结束，停止定时器
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        System.out.println("\n游戏结束！最终得分：" + score);
    }

    /**
     * 游戏逻辑更新（由定时器调用）
     */
    static class GameUpdater implements Runnable {
        @Override
        public void run() {
            if (!gameRunning) return;

            // 1. 移动所有障碍物向下
            Iterator<int[]> iter = obstacles.iterator();
            while (iter.hasNext()) {
                int[] ob = iter.next();
                ob[1]++;   // y坐标增加
                if (ob[1] >= HEIGHT) {  // 超出底部，说明被成功躲避
                    iter.remove();
                    score += 10;
                }
            }

            // 2. 随机生成新障碍物（约20%概率，且同一列最多一个）
            if (rand.nextInt(5) == 0) {
                int newX = rand.nextInt(WIDTH);
                // 避免同一列连续过多障碍物（可选，让游戏更公平）
                boolean alreadyHas = false;
                for (int[] ob : obstacles) {
                    if (ob[0] == newX && ob[1] < 2) { // 顶部附近已有，暂不生成
                        alreadyHas = true;
                        break;
                    }
                }
                if (!alreadyHas) {
                    obstacles.add(new int[]{newX, 0}); // 从顶部出现
                }
            }

            // 3. 碰撞检测：飞机与任何障碍物重合
            for (int[] ob : obstacles) {
                if (ob[0] == playerX && ob[1] == PLAYER_Y) {
                    gameRunning = false;
                    scheduler.shutdownNow();
                    drawGame();  // 最后一次显示画面
                    return;
                }
            }

            // 4. 刷新画面
            drawGame();
        }
    }

    /**
     * 清屏并绘制当前游戏画面
     */
    private static void drawGame() {
        // 使用 ANSI 转义序列清屏 (大多数现代终端支持，如Windows Terminal、CMD、Linux、macOS)
        System.out.print("\033[H\033[2J");
        System.out.flush();

        // 构建游戏画布 (二维字符数组)
        char[][] canvas = new char[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            Arrays.fill(canvas[y], ' ');
        }

        // 绘制障碍物
        for (int[] ob : obstacles) {
            int x = ob[0];
            int y = ob[1];
            if (y >= 0 && y < HEIGHT && x >= 0 && x < WIDTH) {
                canvas[y][x] = '#';
            }
        }

        // 绘制飞机
        if (playerX >= 0 && playerX < WIDTH) {
            canvas[PLAYER_Y][playerX] = '^';
        }

        // 输出画布和分数
        StringBuilder sb = new StringBuilder();
        sb.append("Score: ").append(score).append("\n");
        sb.append("+").append("-".repeat(WIDTH)).append("+\n");
        for (int y = 0; y < HEIGHT; y++) {
            sb.append("|");
            for (int x = 0; x < WIDTH; x++) {
                sb.append(canvas[y][x]);
            }
            sb.append("|\n");
        }
        sb.append("+").append("-".repeat(WIDTH)).append("+\n");
        sb.append("Controls: [A] left  [D] right  (press Enter after each move)\n");

        System.out.print(sb.toString());
    }
}