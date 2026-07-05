#!/usr/bin/env python3
"""Extracts codebase metrics and renders a badge dashboard into ./out.

Sources:
  - Kover XML aggregate report (coverage counters)
  - detekt markdown reports (complexity trend metrics)
  - detekt SARIF reports (issue counts, always 0 on a gated green build)
  - Kotlin sources (size/shape heuristics)
  - git history (churn and hotspots)
"""
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from xml.etree import ElementTree

import anybadge

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "out"
KOVER_XML = ROOT / "target/site/kover/report.xml"
CHURN_DAYS = 90

COVERAGE_LABELS = {
    "LINE": "lines",
    "BRANCH": "branches",
    "CLASS": "classes",
    "INSTRUCTION": "instructions",
    "METHOD": "methods",
}

# Phrases from detekt's "Complexity Report" section mapped to metric keys.
DETEKT_TRENDS = {
    "cyclomatic complexity": "mcc",
    "cognitive complexity": "cognitive_complexity",
    "number of total code smells": "code_smells",
    "comment source ratio": "comment_ratio_pct",
    "mcc per": "mcc_per_1000_lloc",
    "code smells per": "smells_per_1000_lloc",
}


def coverage_metrics():
    root = ElementTree.parse(KOVER_XML).getroot()
    result = {}
    # Root-level counters are direct children (module/package counters are nested deeper).
    for counter in root.findall("counter"):
        label = COVERAGE_LABELS.get(counter.get("type"))
        if label:
            missed = int(counter.get("missed"))
            covered = int(counter.get("covered"))
            total = missed + covered
            result[label] = round(100 * covered / total, 1) if total else 0.0
    return result


def kotlin_files(subdir):
    return sorted(ROOT.glob(f"*/src/{subdir}/**/*.kt"))


def loc(path):
    return sum(1 for line in path.read_text().splitlines() if line.strip())


def function_lengths(text):
    """Approximate: counts lines from a `fun` declaration's opening brace to its match."""
    lengths, depth, start = [], 0, None
    for i, line in enumerate(text.splitlines()):
        if start is None and re.search(r"\bfun\b.*\{\s*$", line):
            start, depth = i, 0
        if start is not None:
            depth += line.count("{") - line.count("}")
            if depth <= 0:
                lengths.append(i - start + 1)
                start = None
    return lengths


def complexity_metrics():
    main_files, test_files = kotlin_files("main"), kotlin_files("test")
    main_loc = sum(loc(f) for f in main_files)
    test_loc = sum(loc(f) for f in test_files)
    func_lens, class_count, largest = [], 0, 0
    for f in main_files:
        text = f.read_text()
        func_lens += function_lengths(text)
        class_count += len(re.findall(r"^\s*(?:\w+\s+)*(?:class|object|interface)\s", text, re.M))
        largest = max(largest, loc(f))
    n_funcs = len(func_lens)
    return {
        "main_loc": main_loc,
        "test_loc": test_loc,
        "test_ratio": round(100 * test_loc / main_loc, 1) if main_loc else 0.0,
        "avg_func_lines": round(sum(func_lens) / n_funcs, 1) if n_funcs else 0.0,
        "max_func_lines": max(func_lens, default=0),
        "avg_class_loc": round(main_loc / class_count, 1) if class_count else 0.0,
        "func_per_class": round(n_funcs / class_count, 1) if class_count else 0.0,
        "largest_file": largest,
    }


def detekt_metrics():
    totals = {}
    for md in sorted(ROOT.glob("*/target/detekt.md")):
        for line in md.read_text().splitlines():
            m = re.match(r"\*\s+([\d,]+)%?\s+(.*)", line.strip())
            if not m:
                continue
            value = int(m.group(1).replace(",", ""))
            for phrase, key in DETEKT_TRENDS.items():
                if m.group(2).startswith(phrase):
                    totals[key] = totals.get(key, 0) + value
    issues = 0
    for sarif in ROOT.glob("*/target/detekt.sarif"):
        data = json.loads(sarif.read_text())
        for run in data.get("runs", []):
            issues += len(run.get("results", []))
    totals["detekt_issues"] = issues
    return totals


def churn_metrics():
    log = subprocess.run(
        ["git", "log", f"--since={CHURN_DAYS} days", "--numstat", "--format=", "--", "*.kt"],
        capture_output=True, text=True, cwd=ROOT, check=True).stdout
    added = deleted = 0
    per_file = {}
    for line in log.splitlines():
        parts = line.split("\t")
        if len(parts) == 3 and parts[0].isdigit():
            added += int(parts[0])
            deleted += int(parts[1])
            per_file[parts[2]] = per_file.get(parts[2], 0) + 1
    hotspots = sorted(per_file.items(), key=lambda kv: -kv[1])[:10]
    return {
        "churn_added": added,
        "churn_deleted": deleted,
        "hotspots": [{"file": f, "changes": c} for f, c in hotspots],
    }


def color(value, green, yellow, invert=False):
    ok, warn = (value <= green, value <= yellow) if invert else (value >= green, value >= yellow)
    return "green" if ok else ("orange" if warn else "red")


def badge(name, label, value, badge_color):
    anybadge.Badge(label=label, value=value, default_color=badge_color).write_badge(
        str(OUT / "badges" / f"{name}.svg"), overwrite=True)


def render_badges(m):
    for key in COVERAGE_LABELS.values():
        badge(key, key, f"{m[key]}%", color(m[key], 80, 60))
    badge("test_ratio", "test ratio", f"{m['test_ratio']}%", color(m["test_ratio"], 100, 60))
    badge("avg_func_lines", "avg func lines", m["avg_func_lines"],
          color(m["avg_func_lines"], 15, 25, invert=True))
    badge("max_func_lines", "max func lines", m["max_func_lines"],
          color(m["max_func_lines"], 30, 60, invert=True))
    badge("avg_class_loc", "avg class loc", m["avg_class_loc"],
          color(m["avg_class_loc"], 100, 200, invert=True))
    badge("largest_file", "largest file", m["largest_file"],
          color(m["largest_file"], 300, 500, invert=True))
    badge("func_per_class", "func/class", m["func_per_class"], "gray")
    badge("main_loc", "main loc", m["main_loc"], "gray")
    badge("test_loc", "test loc", m["test_loc"], "gray")
    badge("churn", "churn 90d", f"+{m['churn_added']}/-{m['churn_deleted']}", "gray")
    # Absolute complexity grows with any codebase; the trend in history.ndjson
    # is the signal, so these stay neutral.
    badge("mcc", "cyclomatic", m.get("mcc", 0), "gray")
    badge("cognitive", "cognitive", m.get("cognitive_complexity", 0), "gray")
    badge("detekt", "detekt issues", m["detekt_issues"],
          color(m["detekt_issues"], 0, 10, invert=True))


def render_markdown(m):
    hotspot_rows = "\n".join(
        f"| `{h['file']}` | {h['changes']} |" for h in m["hotspots"]) or "| - | - |"
    metrics_md = f"""# Metrics Overview

Updated: {m['timestamp']} (`{m['sha']}`)

## Test Coverage

![](badges/lines.svg) ![](badges/branches.svg) ![](badges/classes.svg) ![](badges/instructions.svg) ![](badges/methods.svg)

## Code Complexity Indicators

![](badges/main_loc.svg) ![](badges/test_loc.svg) ![](badges/test_ratio.svg) ![](badges/avg_func_lines.svg)
![](badges/max_func_lines.svg) ![](badges/avg_class_loc.svg) ![](badges/func_per_class.svg) ![](badges/largest_file.svg)

## Static Analysis

![](badges/detekt.svg) ![](badges/mcc.svg) ![](badges/cognitive.svg)

[Full detekt report](DETEKT.md)

## Code Churn (last {CHURN_DAYS} days)

![](badges/churn.svg)

| Hotspot | Changes |
|---|---|
{hotspot_rows}
"""
    (OUT / "METRICS.md").write_text(metrics_md)
    (OUT / "README.md").write_text(
        "# Metrics branch\n\nAuto-generated by the Metrics workflow. "
        "See [METRICS.md](METRICS.md).\n")
    detekt_md = "\n\n---\n\n".join(
        f"## Module `{p.parent.parent.name}`\n\n{p.read_text()}"
        for p in sorted(ROOT.glob("*/target/detekt.md"))) or "No detekt reports found."
    (OUT / "DETEKT.md").write_text(f"# Detekt Report\n\n{detekt_md}\n")


def main():
    (OUT / "badges").mkdir(parents=True, exist_ok=True)
    sha = subprocess.run(["git", "rev-parse", "--short", "HEAD"],
                         capture_output=True, text=True, cwd=ROOT, check=True).stdout.strip()
    metrics = {"sha": sha, "timestamp": datetime.now(timezone.utc).isoformat(timespec="seconds")}
    metrics |= coverage_metrics() | complexity_metrics() | detekt_metrics() | churn_metrics()

    render_badges(metrics)
    render_markdown(metrics)
    (OUT / "metrics.json").write_text(json.dumps(metrics))
    print(json.dumps(metrics, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
