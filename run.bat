@echo off
chcp 65001 >nul
cd /d "%~dp0src"
echo 正在编译...
javac -encoding UTF-8 -d ..\bin GameEntry.java
if %errorlevel% neq 0 (
    echo 编译失败，请检查错误信息。
    pause
    exit /b 1
)
echo 编译成功，正在启动游戏...
cd ..\bin
start "" java GameEntry
echo 游戏已启动！
timeout /t 1 /nobreak >nul

