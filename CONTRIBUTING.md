# Contributing

Thanks for contributing to **PHP Dead & Zombie Code Detector**.

---

## Development setup

- JDK **17**
- IntelliJ IDEA **Ultimate** (recommended) or a compatible IntelliJ SDK for Gradle IntelliJ Plugin

Build:

```bash
./gradlew build
```

Run sandbox IDE:

```bash
./gradlew runIde
```

---

## Code style

- Kotlin: keep changes small and performance-aware (this is a background analyzer plugin)
- Avoid heavy PSI work in:
  - inspections
  - UI callbacks
- Prefer:
  - background `Task.Backgroundable`
  - smart-mode read actions
  - cached snapshots

---

## What we accept in v1.x

- Better entry point configuration UI
- More precise method dispatch modeling (within static PSI limits)
- Improved incremental invalidation keyed by file-level changes

---

## Pull request checklist

- Plugin builds: `./gradlew buildPlugin`
- Sandbox runs: `./gradlew runIde`
- Verified on a medium PHP project (no freezes / UI hangs)

