---
name: learn-byzantine-android-debug-run
description: Build, install, archive, and launch the LearnByzantineMusic debug APK on exactly one connected Android adb device.
---

# LearnByzantineMusic Android Debug Run

Use this skill when the user wants to run the local `LearnByzantineMusic` Android app on a connected Android phone or adb-visible device.

## Command

```bash
.codex/bin/run-skill learn-byzantine-android-debug-run
```

## What it does

- Verifies `adb` from `PATH`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or the default macOS Android SDK path.
- Requires exactly one connected `adb` device unless `--serial` is passed.
- Runs `./gradlew installDebug --warning-mode all` from the project root.
- Retries once for `INSTALL_FAILED_UPDATE_INCOMPATIBLE` by uninstalling `com.johnchourp.learnbyzantinemusic`.
- Archives the generated debug APK under `build-artifacts/apk/`.
- Sends the launcher intent for `com.johnchourp.learnbyzantinemusic`.

## Options

- `--serial <device-serial>`: Target one connected adb device.
- `--clean`: Run Gradle `clean` before `installDebug`.
- `--skip-launch`: Install and archive only.
- `--archive-dir <path>`: Override the archive directory.

## Rules

- Do not auto-start an emulator for this project-specific skill.
- If no device is connected, stop and ask the user to connect the phone with USB debugging enabled.
- If multiple devices are connected, stop and rerun with `--serial`.
