---
name: android-app
description: Build or update a small Android app on this phone with the on-phone toolchain, no computer needed.
---

# Android app on the phone

Use when the user asks to make, build, change or install a small Android app.

1. Read the android-app instructions in the work folder first: `AGENTS.md`, or `.claude/skills/android-app/SKILL.md` when it exists. They have the toolchain, project layout and build script.
2. Put the project in its own folder under the work folder. Choose a package name and a version, and keep them consistent across builds.
3. Build with the provided script. Fix compile errors before installing anything.
4. Install with the installer tool, then wait for the install to finish. Open the app and read the screen to check it launches.
5. When updating, bump the version code so Android accepts the update. Say what changed in one line.
6. Save the APK to the phone's Downloads folder when the user wants to keep or share it.
7. Never push to git, publish or share the APK without an explicit approval.
