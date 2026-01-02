#!/bin/bash
# Script to clean Gradle cache and fix build issues

echo "🧹 Cleaning Gradle cache and build files..."

# Stop Gradle daemons
echo "Stopping Gradle daemons..."
./gradlew --stop 2>/dev/null || echo "Gradle wrapper not available, skipping..."

# Remove local build directories
echo "Removing local build directories..."
rm -rf .gradle
rm -rf app/build
rm -rf build

# Remove Gradle cache (user home)
echo "Removing Gradle cache from user home..."
rm -rf ~/.gradle/caches
rm -rf ~/.gradle/daemon

# Remove wrapper cache
echo "Removing Gradle wrapper cache..."
rm -rf ~/.gradle/wrapper

echo "✅ Clean complete!"
echo ""
echo "Next steps:"
echo "1. Open Android Studio"
echo "2. Go to File > Invalidate Caches / Restart > Invalidate and Restart"
echo "3. After restart, go to File > Sync Project with Gradle Files"
echo "4. Wait for Gradle to download dependencies (this may take a few minutes)"

