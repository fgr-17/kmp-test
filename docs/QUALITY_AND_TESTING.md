# Quality & testing — how to run everything

OSS-only tooling, **CLI + plain HTML/XML/TXT/SARIF** reports (no SaaS, no paid tiers).

One shared Gherkin feature: [`e2e/features/offline_notes.feature`](../e2e/features/offline_notes.feature).  
One Behave **driver per app**: `web` | `android` | `ios` | `desktop`.

---

## Quick map

From the repo root, `make` wraps the commands below. `make all` runs the headless gate (quality + Go contract + the summary table). Per app: `make e2e-web`, `make e2e-android`, `make e2e-ios`, `make e2e-desktop`. Each of those starts the Go backend on :8081 when it is down, and starts the app runtime that driver needs (Wasm server, desktop bridge, emulator + Appium, or a booted simulator + Appium). They stay headless unless you pass `SHOW_UI=1` (`make e2e-web SHOW_UI=1` opens the browser, `e2e-desktop` the window, `e2e-android` the emulator, `e2e-ios` the simulator window, DeviceHub on Xcode 27). `e2e-android` shuts the emulator down when the run ends. A desktop or Wasm process that is already running keeps the visibility it was started with. `make e2e` runs all four. `make summary` reprints `build/reports/summary.md`. `make clean` deletes E2E reports and that summary. `make down` stops the cloud (Go or Docker), the Wasm app (:8080), the desktop bridge (:18081), Appium (:4723), running Android emulators, and booted iOS simulators. It does not stop the Gradle daemon.

| Layer | Command | Reports |
|-------|---------|---------|
| Unit tests (sharedLogic) | `./gradlew :sharedLogic:jvmTest` | `sharedLogic/build/reports/tests/` |
| Coverage (JaCoCo, JVM) | `./gradlew :sharedLogic:jacocoJvmTestReport` | `sharedLogic/build/reports/jacoco/jacocoJvmTestReport/` |
| Lint (ktlint) | `./gradlew ktlintCheck` | `*/build/reports/ktlint/` |
| Static analysis (Detekt) | `./gradlew detekt` | `*/build/reports/detekt/` |
| Duplication (PMD CPD) | `./gradlew cpdCheck` | `build/reports/cpd/` |
| All of the above (bundle) | `./gradlew qualityCheck` | paths above |
| Go contract test | `cd backend && go test ./...` | CLI |
| Cloud API (local) | `cd backend && go run .` or `docker compose up --build api` | — |
| E2E (per app) | `./e2e/run.sh <web\|android\|ios\|desktop>` | `e2e/reports/` |

---

## 1. One-time setup

`./e2e/run.sh` creates `e2e/.venv` from `e2e/requirements.txt` when it is missing, and installs Playwright Chromium the first time the web driver runs. The venv stays on the machine (`e2e/.venv/` is gitignored).

The agent image still needs the tools the scripts call: a JDK, Go, and `python3`. Android also needs an SDK (set `ANDROID_HOME`; otherwise Mac uses `~/Library/Android/sdk`, Linux `~/Android/Sdk`, Windows `%LOCALAPPDATA%\Android\Sdk`), one AVD (`GREETING_ANDROID_AVD`, or the first AVD if `Pixel_9_Pro` is absent), and Appium with the UiAutomator2 driver. iOS needs Xcode and Appium with the XCUITest driver. Node is only required because Appium is a Node program.

---

## 2. Static analysis + unit tests + coverage

From the repo root:

```bash
./gradlew qualityCheck
```

Or piece by piece:

```bash
./gradlew detekt
./gradlew ktlintCheck
./gradlew cpdCheck
./gradlew :sharedLogic:jvmTest
./gradlew :sharedLogic:jacocoJvmTestReport
```

Platform host tests (optional):

```bash
./gradlew :sharedLogic:testAndroidHostTest :sharedUI:testAndroidHostTest
./gradlew :sharedLogic:iosSimulatorArm64Test   # macOS + Xcode
```

**Report locations**

- Detekt: `sharedLogic/build/reports/detekt/detekt.html` (also `.xml`, `.txt`, `.sarif`, `.md`)
- ktlint: `*/build/reports/ktlint/`
- CPD: `build/reports/cpd/`
- JaCoCo: `sharedLogic/build/reports/jacoco/jacocoJvmTestReport/html/index.html` + XML
- JUnit HTML: `sharedLogic/build/reports/tests/jvmTest/`

---

## 3. Contracts + cloud backend

```bash
# Fixture-aligned Go test
cd backend && go test ./...

# Run API locally
cd backend && go run .
# or
docker compose up --build api
```

OpenAPI + golden JSON: [`contracts/`](../contracts/).

---

## 4. E2E (same Gherkin, one driver per app)

`make e2e-*` starts the cloud when :8081 is down. To run it yourself:

```bash
cd backend && go run .
```

### Web

Terminal A — Wasm app (keep running):

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Terminal B:

```bash
source e2e/.venv/bin/activate
./e2e/run.sh web
```

Uses `http://127.0.0.1:8080/?e2e=1` (DOM overlay; Compose canvas alone is not automatable). Override with `GREETING_WEB_URL`.

### Desktop

Terminal A:

```bash
GREETING_E2E=1 ./gradlew :desktopApp:run
```

Terminal B:

```bash
source e2e/.venv/bin/activate
./e2e/run.sh desktop
```

Talks to the JDK bridge on `http://127.0.0.1:18081`.

### Android

```bash
# Emulator running + Appium server on :4723
./gradlew :androidApp:assembleDebug
appium   # separate terminal
source e2e/.venv/bin/activate
./e2e/run.sh android
```

Optional: `GREETING_ANDROID_APP`, `APPIUM_SERVER_URL`. The driver runs `adb reverse tcp:8081 tcp:8081` so the app can reach the host backend at `http://127.0.0.1:8081`.

### iOS

```bash
# Simulator + Appium + built/installed iosApp
appium
source e2e/.venv/bin/activate
./e2e/run.sh ios
```

Optional: `GREETING_IOS_BUNDLE_ID`, `GREETING_IOS_DEVICE`, `GREETING_IOS_UDID`, `GREETING_IOS_PLATFORM_VERSION`, `APPIUM_SERVER_URL`. A booted Simulator is reused; set `GREETING_IOS_UDID` when more than one is booted.

### E2E reports

- HTML: `e2e/reports/html/<driver>.html`
- JUnit XML: `e2e/reports/junit/<driver>/`
- Combined table: `make summary` → `build/reports/summary.md`

---

## 5. Suggested “run everything” order (laptop)

```bash
# quality gate
./gradlew qualityCheck
cd backend && go test ./... && cd ..

# cloud
cd backend && go run . &

# web e2e (after wasm run is up in another terminal)
source e2e/.venv/bin/activate
./e2e/run.sh web

# desktop e2e (after GREETING_E2E=1 ./gradlew :desktopApp:run)
./e2e/run.sh desktop
```

Android/iOS when emulator/simulator + Appium are available.

---

## 6. New runners

The repo does not contain a Jenkinsfile. Which job runs on which machine is the Jenkinsfile (agent labels). The commands those jobs call are already here: `make quality`, `make contract`, `make e2e-web`, `make e2e-desktop`, `make e2e-android`, `make e2e-ios`.

| Agent | What it can run |
|-------|-----------------|
| Linux | quality, Go contract, web, desktop, and Android if the image has KVM, an SDK, an AVD, and Appium |
| macOS | iOS, plus anything in the Linux row |
| Windows | desktop and web. The helper scripts are bash, so the agent needs Git Bash or WSL |

iOS cannot run on the Linux or Windows agents. That split is the Jenkinsfile. A fresh agent does not need this laptop's venv or hand-started backend: `make e2e-*` installs the Python env if needed and starts the Go server when `:8081` is down. It does not install Xcode, the Android SDK, or Appium.
