# Thin targets over Gradle, Go, and the Behave drivers.
# `make all` is the headless gate (no emulator, simulator, or browser).
# `make e2e` runs every app and needs the cloud plus that app's runtime.
# SHOW_UI=1 opens the browser, desktop window, emulator, or Simulator.
# The default is headless. `make e2e-android` always stops the emulator at the end.

export SHOW_UI ?= 0

.PHONY: all quality unit contract e2e e2e-web e2e-android e2e-ios e2e-desktop summary clean down

all: quality contract summary

quality:
	./gradlew qualityCheck

unit:
	./gradlew :sharedLogic:jvmTest :sharedLogic:jacocoJvmTestReport

contract:
	mkdir -p build/reports
	cd backend && go test ./... -json > ../build/reports/backend-test.json

e2e-web:
	./e2e/run.sh web

e2e-android:
	./e2e/run.sh android

e2e-ios:
	./e2e/run.sh ios

e2e-desktop:
	./e2e/run.sh desktop

e2e: e2e-web e2e-desktop e2e-android e2e-ios

summary:
	python3 scripts/metrics_summary.py

clean:
	rm -rf e2e/reports build/reports/summary.md build/reports/backend-test.json

down:
	bash scripts/down.sh
