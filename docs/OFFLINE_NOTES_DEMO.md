# Offline notes demo

Tiny extension of GreetingKMP that mimics a **cellar device**: capture data with no network, sync later to the Go cloud.

## Story

1. Device starts **offline** (default).
2. Operator adds a field note → stored only on device (`pending`).
3. Flip **Online**, tap **Sync** → pending notes POST to `backend/`.
4. Notes show as `synced`. Inspect cloud with `GET http://localhost:8081/notes`.

## Run

Terminal 1 — cloud:

```bash
cd backend && go run .
```

Terminal 2 — desktop client (easiest):

```bash
./gradlew :desktopApp:run
```

Or Android (emulator → host machine):

```bash
./gradlew :androidApp:installDebug
```

Android uses `http://127.0.0.1:8081`. Forward it once per emulator boot:

```bash
adb reverse tcp:8081 tcp:8081
```

## Pieces

| Layer | Code |
|-------|------|
| Models + local store + sync | `sharedLogic/.../notes/` |
| Cloud | `backend/` (Go) |
| Desktop UI | `desktopApp/.../DesktopApp.kt` |
| Android UI | `sharedUI/.../App.kt` + `MainViewModel.kt` |
| Web UI | `webApp/.../WebApp.kt` |
| iOS UI | `iosApp/.../ContentView.swift` |

Local persistence in this demo is **in-memory** (resets on process kill). Production would swap `LocalNoteStore` for SQLDelight.

**URLs:** desktop / web / iOS Simulator → `http://localhost:8081`. Android emulator → `http://127.0.0.1:8081` after `adb reverse tcp:8081 tcp:8081`.

## Architecture reminder

See [CLOUD_VS_LOCAL_BACKEND.md](CLOUD_VS_LOCAL_BACKEND.md).
