# WayfindR Build Guide

This document describes how to build the WayfindR Android application from source.

## Prerequisites

### Required Software
- **Java 17 JDK** (required by Android Gradle Plugin 8.10.0)
- **Gradle 8.11.1** (included via Gradle wrapper)
- **Android SDK** with API Level 35

### System Requirements
- **Ubuntu 22.04** (or compatible Linux distribution)
- **Minimum 2GB RAM** available for build process
- **~500MB** free disk space for build artifacts

## Initial Setup

### 1. Install Java 17

If you don't have Java 17 installed:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
```

Verify installation:
```bash
java -version
# Should show: openjdk version "17.0.x"
```

### 2. Configure Gradle

The project includes a `gradle.properties` file with optimized settings for building on Ubuntu 22.04:

```properties
# JVM Settings - Configured for stability and performance
org.gradle.jvmargs=-Xmx1536m -Xms512m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8

# Java Home - Points to Java 17
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64

# Build Optimizations
org.gradle.daemon=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

**Resource Limits Explained:**
- `Xmx1536m` - Maximum heap size (prevents system crashes on systems with limited RAM)
- `Xms512m` - Initial heap size
- `MaxMetaspaceSize=512m` - Limits metadata memory usage
- `HeapDumpOnOutOfMemoryError` - Creates dump file if build runs out of memory

### 3. Make Gradle Wrapper Executable

```bash
chmod +x gradlew.sh
```

## Building the APK

### Debug Build (Recommended for Testing)

Build a debug APK with the following command:

```bash
./gradlew.sh assembleDebug
```

**Build Output:**
- Location: `app/build/outputs/apk/debug/`
- File Name: `WayfindR-debug-v1.0.apk`
- Size: ~64MB
- Build Time: ~1-2 minutes (first build may take longer)

### Release Build (For Production)

Build a release APK:

```bash
./gradlew.sh assembleRelease
```

**Note:** Release builds require signing configuration. The unsigned APK will be generated but needs to be signed before distribution.

### Clean Build

To clean previous build artifacts and rebuild from scratch:

```bash
./gradlew.sh clean assembleDebug
```

## Build Configuration

### App Information
- **Package Name:** `com.example.wayfindr`
- **Min SDK:** 30 (Android 11)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35
- **Version Code:** 1
- **Version Name:** 1.0

### APK Naming Convention

APKs are automatically named using the pattern:
```
WayfindR-{buildType}-v{versionName}.apk
```

Examples:
- `WayfindR-debug-v1.0.apk`
- `WayfindR-release-v1.0.apk`

To change the version:
1. Open `app/build.gradle.kts`
2. Update `versionCode` and `versionName` in the `defaultConfig` section
3. Rebuild

## Build Variants

The project supports the following build types:

### Debug
- **Debugging:** Enabled
- **Minification:** Disabled
- **Obfuscation:** Disabled
- **Signing:** Debug keystore (auto-generated)

### Release
- **Debugging:** Disabled
- **Minification:** Disabled (can be enabled by setting `isMinifyEnabled = true`)
- **Obfuscation:** Available via ProGuard
- **Signing:** Requires release keystore configuration

## Troubleshooting

### Build Fails with "Java 11" Error

**Error:** `Android Gradle plugin requires Java 17 to run. You are currently using Java 11.`

**Solution:**
1. Verify Java 17 is installed: `java -version`
2. Ensure `gradle.properties` points to Java 17:
   ```
   org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64
   ```

### System Crashes During Build

**Symptom:** Ubuntu becomes unresponsive during build

**Solution:** Reduce memory allocation in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx1024m -Xms256m -XX:MaxMetaspaceSize=384m
```

### Permission Denied on gradlew.sh

**Error:** `Permission denied: ./gradlew.sh`

**Solution:**
```bash
chmod +x gradlew.sh
```

### Gradle Daemon Issues

If you experience build inconsistencies:
```bash
# Stop all Gradle daemons
./gradlew.sh --stop

# Rebuild
./gradlew.sh clean assembleDebug
```

## Build Performance Tips

1. **Enable Gradle Daemon:** Already enabled in `gradle.properties`
2. **Enable Build Cache:** Already enabled in `gradle.properties`
3. **Incremental Builds:** Only changed files are recompiled
4. **Parallel Builds:** Can be enabled by uncommenting in `gradle.properties`:
   ```
   org.gradle.parallel=true
   ```

## CI/CD Integration

For automated builds in CI/CD pipelines:

```bash
# Non-interactive build with stacktrace
./gradlew.sh assembleDebug --stacktrace --no-daemon

# For release builds
./gradlew.sh assembleRelease --stacktrace --no-daemon
```

## Additional Resources

- [Android Gradle Plugin Documentation](https://developer.android.com/build)
- [Gradle Build Tool](https://gradle.org/)
- [Android Developer Guide](https://developer.android.com/)

## Build Artifacts

After a successful build, you'll find:

```
app/build/outputs/apk/
├── debug/
│   ├── WayfindR-debug-v1.0.apk
│   └── output-metadata.json
└── release/
    ├── WayfindR-release-v1.0.apk (if built)
    └── output-metadata.json
```

The APK file is ready for installation on Android devices running Android 11 (API 30) or higher.
