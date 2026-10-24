@echo off
cd /d "%~dp0"
java -Dloader.main=br.com.inbox.PasswordHash -cp backend\target\inbox-0.1.0.jar org.springframework.boot.loader.launch.PropertiesLauncher
pause
