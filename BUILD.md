# Build & Release Guide

This document describes how to build, run, sign, and publish **PHP Dead & Zombie Code Detector** as a JetBrains plugin.

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

## Signing (Marketplace)

To sign the plugin, provide these environment variables:

- `CERTIFICATE_CHAIN`
- `PRIVATE_KEY`
- `PRIVATE_KEY_PASSWORD`

Then run:

```bash
./gradlew signPlugin
```

---

## Publishing (Marketplace)

Set:

- `PUBLISH_TOKEN`

Then run:

```bash
./gradlew publishPlugin
```

---

## Troubleshooting

### “PHP plugin not found”

This plugin depends on `com.jetbrains.php`. Ensure you run against:

- IntelliJ IDEA **Ultimate** (IU) OR
- PhpStorm (in production install)

### Slow first build

First build downloads IntelliJ SDK + dependencies. Subsequent builds are much faster.

