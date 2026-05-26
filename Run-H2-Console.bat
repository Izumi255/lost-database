@echo off
chcp 65001 > nul
title LOST — H2 Database Web Console
cls

echo ===================================================================
echo   🎮 LOST Game Database Web Console Launcher 🎮
echo ===================================================================
echo.
echo [ІНФОРМАЦІЯ] Цей скрипт збирає проект та запускає вбудований
echo             веб-сервер консолі бази даних H2 на порту 8082.
echo.
echo [ПАРАМЕТРИ ПІДКЛЮЧЕННЯ У БРАУЗЕРІ]:
echo -------------------------------------------------------------------
echo   🔗 Посилання:      http://localhost:8082
echo   🔌 Driver Class:   org.h2.Driver
echo   🔗 JDBC URL:       jdbc:h2:file:~/.lost-database/data/lostdb;AUTO_SERVER=TRUE
echo   👤 User Name:      sa
echo   🔑 Password:       (залиште порожнім)
echo -------------------------------------------------------------------
echo.
echo [Запуск] Збірка та старт через Maven...
echo.

call mvn compile exec:java -Dexec.mainClass="com.lost.database.DatabaseMigrator"

echo.
echo ===================================================================
echo [Кінець] Робота сервера завершена.
echo ===================================================================
pause
