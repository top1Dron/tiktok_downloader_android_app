@echo off
REM Script to clean Gradle cache and fix build issues (Windows)

echo Cleaning Gradle cache and build files...

REM Stop Gradle daemons
echo Stopping Gradle daemons...
gradlew.bat --stop 2>nul

REM Remove local build directories
echo Removing local build directories...
if exist .gradle rmdir /s /q .gradle
if exist app\build rmdir /s /q app\build
if exist build rmdir /s /q build

REM Remove Gradle cache (user home)
echo Removing Gradle cache from user home...
if exist "%USERPROFILE%\.gradle\caches" rmdir /s /q "%USERPROFILE%\.gradle\caches"
if exist "%USERPROFILE%\.gradle\daemon" rmdir /s /q "%USERPROFILE%\.gradle\daemon"

REM Remove wrapper cache
echo Removing Gradle wrapper cache...
if exist "%USERPROFILE%\.gradle\wrapper" rmdir /s /q "%USERPROFILE%\.gradle\wrapper"

echo Clean complete!
echo.
echo Next steps:
echo 1. Open Android Studio
echo 2. Go to File ^> Invalidate Caches / Restart ^> Invalidate and Restart
echo 3. After restart, go to File ^> Sync Project with Gradle Files
echo 4. Wait for Gradle to download dependencies (this may take a few minutes)

pause

