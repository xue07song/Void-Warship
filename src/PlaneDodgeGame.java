import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Swing 图形版飞机躲避障碍物游戏
 * 玩法：使用 W/A/S/D 或方向键控制飞机移动，避开红色障碍物
 * 每成功躲避一个障碍物得 10 分，碰到障碍物游戏结束
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Swing 图形版飞机躲避障碍物游戏
 * 玩法：使用 W/A/S/D 或方向键控制飞机移动，避开红色障碍物
 * 每成功躲避一个障碍物得 10 分，碰到障碍物游戏结束
 */
public class PlaneDodgeGame extends JFrame implements KeyListener, ActionListener {
    // 游戏区域尺寸
    private static final int WIDTH = 600;
    private static final int HEIGHT = 500;
    
    // 飞机尺寸
    private static final int PLANE_WIDTH = 40;
    private static final int PLANE_HEIGHT = 40;
    
    // 障碍物尺寸
    private static final int OBSTACLE_WIDTH = 30;
    private static final int OBSTACLE_HEIGHT = 30;
    
    // 移动速度
    private static final int MOVE_SPEED = 10;
    private static final int OBSTACLE_SPEED = 5;
    
    // 游戏状态
    private int playerX;
    private int playerY;
    private int score = 0;
    private boolean gameRunning = true;
    
    // 障碍物列表：每个障碍物是一个(x, y)坐标
    private final List<int[]> obstacles = new ArrayList<>();
    private final Random rand = new Random();
    
    // 游戏定时器：每 50ms 更新一次
    private Timer gameTimer;
    
    // 按键状态
    private boolean keyUp = false;
    private boolean keyDown = false;
    private boolean keyLeft = false;
    private boolean keyRight = false;

    public PlaneDodgeGame() {
        // 初始化飞机位置（底部中央）
        playerX = (WIDTH - PLANE_WIDTH) / 2;
        playerY = HEIGHT - PLANE_HEIGHT - 10;
        
        // 设置窗口
        setTitle("飞机躲避障碍物");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // 添加键盘监听
        addKeyListener(this);
        setFocusable(true);
        
        // 创建定时器：每 50ms 更新一次游戏状态
        gameTimer = new Timer(50, this);
        gameTimer.start();
        
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;
        
        // 1. 根据按键状态移动飞机
        if (keyUp) playerY = Math.max(0, playerY - MOVE_SPEED);
        if (keyDown) playerY = Math.min(HEIGHT - PLANE_HEIGHT, playerY + MOVE_SPEED);
        if (keyLeft) playerX = Math.max(0, playerX - MOVE_SPEED);
        if (keyRight) playerX = Math.min(WIDTH - PLANE_WIDTH, playerX + MOVE_SPEED);
        
        // 2. 移动所有障碍物向下
        Iterator<int[]> iter = obstacles.iterator();
        while (iter.hasNext()) {
            int[] ob = iter.next();
            ob[1] += OBSTACLE_SPEED;
            if (ob[1] >= HEIGHT) {  // 超出底部，成功躲避
                iter.remove();
                score += 10;
            }
        }
        
        // 3. 随机生成新障碍物（约15%概率）
        if (rand.nextInt(7) == 0) {
            int newX = rand.nextInt(WIDTH - OBSTACLE_WIDTH);
            // 避免同一位置连续生成
            boolean alreadyHas = false;
            for (int[] ob : obstacles) {
                if (Math.abs(ob[0] - newX) < OBSTACLE_WIDTH && ob[1] < 60) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                obstacles.add(new int[]{newX, 0});
            }
        }
        
        // 4. 碰撞检测：飞机与障碍物矩形相交
        Rectangle playerRect = new Rectangle(playerX, playerY, PLANE_WIDTH, PLANE_HEIGHT);
        for (int[] ob : obstacles) {
            Rectangle obRect = new Rectangle(ob[0], ob[1], OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            if (playerRect.intersects(obRect)) {
                gameRunning = false;
                gameTimer.stop();
                JOptionPane.showMessageDialog(this, "游戏结束！最终得分：" + score);
                return;
            }
        }
        
        // 5. 刷新画面
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        // 清空画布，黑色背景
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 绘制得分和操作提示
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("得分: " + score, 10, 25);
        g.drawString("操作: W/A/S/D 或 方向键移动", 10, 45);
        
        // 绘制飞机：蓝色矩形 + 白色边框
        g.setColor(Color.BLUE);
        g.fillRect(playerX, playerY, PLANE_WIDTH, PLANE_HEIGHT);
        g.setColor(Color.WHITE);
        g.drawRect(playerX, playerY, PLANE_WIDTH, PLANE_HEIGHT);
        
        // 绘制障碍物：红色矩形
        g.setColor(Color.RED);
        for (int[] ob : obstacles) {
            g.fillRect(ob[0], ob[1], OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                keyUp = true;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                keyDown = true;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                keyLeft = true;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                keyRight = true;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                keyUp = false;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                keyDown = false;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                keyLeft = false;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                keyRight = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // 不需要处理
    }

    public static void main(String[] args) {
        new PlaneDodgeGame();
    }
}