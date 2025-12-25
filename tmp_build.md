# Build Instructions

## Build Command

```bash
export JAVA_HOME=$HOME/jdk-17.0.2 && export PATH=$JAVA_HOME/bin:$PATH && ./gradlew assembleDebug --no-daemon
```

## Output

The APK will be generated at:
```
app/build/outputs/apk/debug/WayfindR-debug-v1.0.apk
```

## Prerequisites

- Java 17 installed at `$HOME/jdk-17.0.2`
- Android SDK installed at `$HOME/android-sdk`
- `local.properties` configured with SDK path
- `gradle.properties` configured with Java home

## Build Time

Approximate build time: 5-6 minutes
