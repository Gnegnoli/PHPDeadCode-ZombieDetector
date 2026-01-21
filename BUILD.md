# Build & Release Guide

This document describes how to build and run  **HP Dead & Zombie Code Detector** as a JetBrains plugin.

---

## Requirements

- **JDK 17**
- IntelliJ IDEA **Ultimate** (recommended for development)
  - required because the plugin depends on `com.jetbrains.php` (PHP PSI)
- A working internet connection for first Gradle dependency resolution

---

## Key Gradle Tasks

All commands below are run from the repository root.

### Build

```bash
./gradlew build
```

### Build plugin ZIP

```bash
./gradlew buildPlugin
```

Artifact output:

```text
build/distributions/php-dead-zombie-detector-1.0.0.zip
```

### Run sandbox IDE with the plugin

```bash
./gradlew runIde
```

The sandbox environment is isolated from your real IDE config, so you can test safely.

---

## Versioning

- Plugin version is defined in `build.gradle.kts`:
  - `version = "1.0.0"`
- Compatibility range is set via `patchPluginXml`:
  - `sinceBuild`
  - `untilBuild`

---

## Troubleshooting

### “PHP plugin not found”

This plugin depends on `com.jetbrains.php`.

### Slow first build

First build downloads IntelliJ SDK + dependencies. Subsequent builds are much faster.

