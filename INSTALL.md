# Install & Verify (PhpStorm)

This guide installs the plugin locally into **PhpStorm** and verifies it on a real PHP project.

---

## 1) Build the plugin ZIP

From the repository root:

```bash
./gradlew buildPlugin
```

You will get:

```text
build/distributions/php-dead-zombie-detector-1.0.0.zip
```

---

## 2) Install in PhpStorm

1. Open **PhpStorm**
2. Go to **Settings → Plugins**
3. Click the gear icon **(⚙)** → **Install Plugin from Disk…**
4. Select:
   - `build/distributions/php-dead-zombie-detector-1.0.0.zip`
5. Restart PhpStorm

---

## 3) Verify inspections

1. Open a PHP project (any non-trivial project is best).
2. Wait for indexing to finish.
3. Ensure these inspections are enabled:
   - Settings → Editor → Inspections → PHP → **PHP Dead & Zombie Code Detector**
4. Open a PHP file that contains a clearly unused class/function/method.
5. You should see a highlight on the symbol name:
   - dead = never referenced
   - zombie = referenced only from unreachable code

Notes:

- Analysis runs in background and is cached. If you don’t see highlights immediately, wait a few seconds or make a small edit to trigger recomputation.
- `vendor/` is ignored.

---

## 4) Verify Tool Window navigation

1. Open: **View → Tool Windows → Dead & Zombie Code**
2. Expand categories:
   - Dead Classes / Methods / Functions
   - Zombie Classes / Methods / Functions
3. Double-click an item:
   - it should navigate to the declaration in the editor

---

## 5) Entry points (what is considered “live”)

In v1.0, the analyzer treats these as entry points:

- any `index.php`
- `public/*.php`
- root-level `*.php`
- `bin/*.php`
- tests (optional, default enabled)

If code is only reachable via dynamic invocation (reflection, variable function names, magic dispatch), it may be flagged.

