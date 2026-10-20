#!/usr/bin/env python3
"""One table over the reports each target already writes."""

from __future__ import annotations

import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build" / "reports" / "summary.md"


def junit_totals(directory: Path) -> tuple[int, int] | None:
    files = sorted(directory.glob("*.xml")) if directory.is_dir() else []
    if not files:
        return None
    tests = failed = 0
    for path in files:
        root = ET.parse(path).getroot()
        suites = [root] if root.tag == "testsuite" else list(root.iter("testsuite"))
        for suite in suites:
            tests += int(suite.attrib.get("tests", 0))
            failed += int(suite.attrib.get("failures", 0)) + int(suite.attrib.get("errors", 0))
    return tests, failed


def jacoco_line_percent(path: Path) -> str:
    if not path.is_file():
        return "—"
    root = ET.parse(path).getroot()
    for counter in root.findall("counter"):
        if counter.attrib.get("type") != "LINE":
            continue
        missed = int(counter.attrib.get("missed", 0))
        covered = int(counter.attrib.get("covered", 0))
        total = missed + covered
        if total == 0:
            return "—"
        return f"{100 * covered / total:.1f}%"
    return "—"


def count_tags(path: Path, tag: str) -> str:
    if not path.is_file():
        return "—"
    return str(len(list(ET.parse(path).getroot().iter(tag))))


def go_totals(path: Path) -> tuple[int, int] | None:
    if not path.is_file():
        return None
    tests = failed = 0
    for line in path.read_text(errors="replace").splitlines():
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if not event.get("Test"):
            continue
        action = event.get("Action")
        if action == "pass":
            tests += 1
        elif action == "fail":
            tests += 1
            failed += 1
    if tests == 0 and failed == 0:
        return None
    return tests, failed


def cell(value: tuple[int, int] | None, index: int) -> str:
    if value is None:
        return "—"
    return str(value[index])


def main() -> None:
    logic_unit = junit_totals(ROOT / "sharedLogic" / "build" / "test-results" / "jvmTest")
    ui_unit = junit_totals(ROOT / "sharedUI" / "build" / "test-results" / "jvmTest")
    backend = go_totals(ROOT / "build" / "reports" / "backend-test.json")
    coverage = jacoco_line_percent(
        ROOT
        / "sharedLogic"
        / "build"
        / "reports"
        / "jacoco"
        / "jacocoJvmTestReport"
        / "jacocoJvmTestReport.xml",
    )
    detekt = count_tags(ROOT / "sharedLogic" / "build" / "reports" / "detekt" / "detekt.xml", "error")
    cpd = count_tags(ROOT / "build" / "reports" / "cpd" / "cpdCheck.xml", "duplication")

    rows = [
        ("sharedLogic", logic_unit, coverage, None),
        ("sharedUI", ui_unit, "—", None),
        ("backend", backend, "—", None),
    ]
    for app in ("web", "android", "ios", "desktop"):
        e2e = junit_totals(ROOT / "e2e" / "reports" / "junit" / app)
        rows.append((app, None, "—", e2e))

    lines = [
        "| Target | Unit tests | Unit failed | Line coverage | E2E scenarios | E2E failed |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for name, unit, cov, e2e in rows:
        lines.append(
            "| {name} | {tests} | {failed} | {cov} | {e2e_tests} | {e2e_failed} |".format(
                name=name,
                tests=cell(unit, 0),
                failed=cell(unit, 1),
                cov=cov,
                e2e_tests=cell(e2e, 0),
                e2e_failed=cell(e2e, 1),
            ),
        )
    lines.append("")
    lines.append(f"Detekt issues (sharedLogic): {detekt}. CPD duplications: {cpd}.")
    lines.append("A dash means that report has not been generated yet.")
    text = "\n".join(lines) + "\n"
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text)
    print(text, end="")
    print(f"Wrote {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
