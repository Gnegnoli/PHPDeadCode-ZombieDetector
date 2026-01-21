# PHP Dead & Zombie Code Detector (JetBrains Plugin)

Project-wide dead code + zombie code detection for **PHP** in **PhpStorm / IntelliJ** using **PHP PSI**.

This plugin performs a **project-wide** static analysis (not file-by-file): it indexes symbol definitions, builds a PSI-resolved call graph, computes reachability from configurable entry points, and reports:

- **Dead code**: defined but never called / referenced anywhere in the project
- **Zombie code**: defined and referenced, but only from code that is itself unreachable from the entry points

> v1.0 scope: generic PHP (no framework routing/DI modeling). Reflection/dynamic invocation is ignored by design (documented below).

---

## Features

- **Project-wide indexing** of:
  - Classes (`PhpClass`)
  - Traits (`PhpTrait`)
  - Global functions (`Function`)
  - Methods (`Method`)
- **Call graph construction** via PHP PSI resolution:
  - `new ClassName()`
  - `$obj->method()`
  - `Class::method()`
  - global function calls
- **Reachability analysis** from **entry points**:
  - `index.php`
  - `public/*.php`
  - root `*.php`
  - `bin/*.php`
  - tests (optional, default enabled)
- **Performance-safe background analysis**:
  - background task
  - smart mode read action
  - cached snapshot consumed by inspections/UI
  - `vendor/` is ignored
- **IDE integration**
  - 3 real `LocalInspectionTool` inspections
  - Tool Window with categorized results + navigation

---

## What “Dead” vs “Zombie” Means Here

Given a call graph \(G\) and a set of entry points \(E\):

- **Reachable**: visited by traversing \(G\) starting from \(E\)
- **Dead**: not reachable AND has no inbound references (inbound degree = 0)
- **Zombie**: not reachable BUT has inbound references (only from unreachable code)

---

## Entry Points (v1.0)

Symbols become “live” only if reachable from **files considered entry points**.

Default entry points:

- `index.php` (anywhere)
- `public/*.php`
- root-level `*.php` in project base dir
- `bin/*.php`

Optional entry points:

- tests (`tests/`, `test/`, paths containing `/tests/`) — **enabled by default**

---

## Quick Start (Developer)

### Requirements

- **JDK 17**
- Gradle (the IntelliJ Gradle plugin will run tasks; wrapper optional)
- IntelliJ IDEA **Ultimate** 2024.3.x (recommended for development)
  - PHP PSI comes from the bundled PHP plugin (`com.jetbrains.php`)

### Build the plugin ZIP

```bash
./gradlew buildPlugin
```

Output:

```text
build/distributions/php-dead-zombie-detector-1.0.0.zip
```

### Run a sandbox IDE with the plugin

```bash
./gradlew runIde
```

---

## Install in PhpStorm (Local)

See [`INSTALL.md`](INSTALL.md).

---

## How It Works (Architecture)

Directory layout (matches the required architecture):

```text
src/main/kotlin/
  analysis/
    DefinitionIndexer.kt
    CallGraphBuilder.kt
    ReachabilityAnalyzer.kt
    DeadCodeAnalyzer.kt
  psi/
    PhpCallVisitor.kt
    PsiUtils.kt
  inspections/
    DeadMethodInspection.kt
    DeadFunctionInspection.kt
    DeadClassInspection.kt
  ui/
    DeadCodeToolWindow.kt
    DeadCodeTreeModel.kt
  settings/
    DeadCodeSettings.kt
```

### Core pipeline

- **Collect PHP files** (project scope, excludes `vendor/`)
- **Definition indexing** (`DefinitionIndexer`)
  - builds a map of FQNs → Smart PSI pointers
- **Call graph building** (`CallGraphBuilder`)
  - resolves call targets with `PhpCallVisitor` and links symbols
  - models top-level code as a *file node* (entry point roots are files)
- **Reachability** (`ReachabilityAnalyzer`)
  - BFS from entry-point file nodes
  - classifies dead vs zombie for classes/traits/functions/methods
- **Caching** (`DeadCodeAnalyzer`)
  - stores a `DeadCodeSnapshot`
  - recomputes in background when PSI changes
  - inspections and UI are snapshot consumers only

---

## Tool Window

Open: **View → Tool Windows → Dead & Zombie Code**

Tree structure:

```text
Dead & Zombie Code
├── Dead Classes (N)       // includes traits too
├── Dead Methods (N)
├── Dead Functions (N)
├── Zombie Classes (N)     // includes traits too
├── Zombie Methods (N)
└── Zombie Functions (N)
```

Double-click an item to navigate to the declaration.

---

## Inspections

Enabled by default:

- **Dead PHP method (project-wide)** (`DeadMethodInspection`)
  - ignores magic methods `__*` (implicit runtime calls)
- **Dead PHP function (project-wide)** (`DeadFunctionInspection`)
  - global functions only
- **Dead PHP class/trait (project-wide)** (`DeadClassInspection`)
  - both `PhpClass` and `PhpTrait`

Important:

- Inspections **do not** perform heavy analysis inline.
- They only read the cached `DeadCodeSnapshot`.

---

## Known Limitations (v1.0)

Ignored on purpose:

- reflection / dynamic dispatch:
  - `call_user_func`, `call_user_func_array`
  - variable function names: `$fn()`
  - magic routing: `__call`, `__callStatic`
- dynamic include/require resolution
- framework routing / DI containers / config-based wiring

This keeps the analyzer fast and deterministic, but may report false positives in highly dynamic codebases.

---

## License

See [`LICENSE`](LICENSE).

