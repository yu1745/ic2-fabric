@echo off
rem 设置控制台为 UTF-8 编码，修复中文乱码
chcp 65001 >nul
call gradlew.bat runClient %*
