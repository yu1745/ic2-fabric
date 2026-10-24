@echo off
rem 设置控制台为 UTF-8 编码，修复中文乱码
chcp 65001 >nul
rem Fabric Loom 1.15+ 需要 JDK 21+ 运行 Gradle（编译产物仍为 Java 17 兼容 MC 1.20.1）
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
call gradlew.bat :core:runClient %*