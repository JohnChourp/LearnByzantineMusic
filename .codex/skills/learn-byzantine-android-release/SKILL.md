---
name: learn-byzantine-android-release
description: Create a full LearnByzantineMusic Android release with version bump, signed build, commit, tag, push, and GitHub Release publish.
---

# LearnByzantineMusic Android Release

Use this skill when the user asks for a new `LearnByzantineMusic` Android release.

## Command

```bash
.codex/bin/run-skill learn-byzantine-android-release --bump patch
```

## What it does

- Loads signing credentials from `~/.android/learnbyzantine/release-signing.env`.
- Runs `scripts/setup-release-signing.sh` if local signing env is missing.
- Chooses GNU Bash >= 4 for `scripts/release-and-tag.sh`.
- Runs the existing project release script for version bump, signed APK/AAB build, release commit, tag, push, release notes, and GitHub Release publish.
- Adds `--skip-gh-release` automatically only when `gh` is missing or unauthenticated, so tag push remains the fallback path.

## Options

- `--bump patch|minor|major`: Semantic version bump. Default release usage is `--bump patch`.
- `--version X.Y.Z`: Explicit version instead of a bump.
- `--code N`: Explicit Android versionCode.
- `--no-push`: Keep commit and tag local.
- `--skip-gh-release`: Skip direct GitHub Release publish.

## Rules

- Full release can commit, tag, push, and publish. Treat it as an intentional release operation.
- Do not put signing secrets in tracked files.
- If a release run fails after bumping version, rerun with explicit `--version X.Y.Z --code N` to avoid a second bump.
