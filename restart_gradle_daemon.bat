@echo off
REM Script to restart Gradle daemon with new JVM arguments (Windows)

echo Stopping all Gradle daemons...

REM Stop all Gradle daemons
gradlew.bat --stop 2>nul

echo Gradle daemons stopped!
echo.
echo Next steps in Android Studio:
echo 1. Go to File ^> Sync Project with Gradle Files
echo 2. The daemon will restart automatically with the new JVM arguments
echo 3. If the error persists, close and reopen Android Studio

pause

