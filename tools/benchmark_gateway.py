#!/usr/bin/env python3
"""Lightweight gateway benchmark for Stark Industries microservices.

It sends real HTTP requests to gateway endpoints and reports:
- total events sent
- total elapsed time
- average response time
- approximate throughput
- alerts generated during each scenario
"""

from __future__ import annotations

import argparse
import base64
import csv
import json
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any


@dataclass
class ScenarioResult:
    name: str
    endpoint: str
    total_events: int
    successful_events: int
    failed_events: int
    total_seconds: float
    average_ms: float
    throughput_eps: float
    alerts_generated: int


def auth_header(username: str, password: str) -> str:
    raw = f"{username}:{password}".encode("utf-8")
    return "Basic " + base64.b64encode(raw).decode("utf-8")


def http_post_json(url: str, payload: dict[str, Any], auth: str, timeout: float) -> int:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url=url, data=data, method="POST")
    request.add_header("Content-Type", "application/json")
    request.add_header("Authorization", auth)

    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.status


def http_get_json(url: str, auth: str, timeout: float) -> Any:
    request = urllib.request.Request(url=url, method="GET")
    request.add_header("Authorization", auth)

    with urllib.request.urlopen(request, timeout=timeout) as response:
        charset = response.headers.get_content_charset() or "utf-8"
        body = response.read().decode(charset)
        return json.loads(body)


def fetch_alert_count(base_url: str, operator_auth: str, timeout: float) -> int:
    alerts_url = f"{base_url}/api/alerts"
    alerts = http_get_json(alerts_url, operator_auth, timeout)
    if isinstance(alerts, list):
        return len(alerts)
    return 0


def run_scenario(
    name: str,
    endpoint: str,
    event_count: int,
    base_url: str,
    sensor_auth: str,
    operator_auth: str,
    timeout: float,
    settle_seconds: float,
) -> ScenarioResult:
    scenario_url = f"{base_url}{endpoint}"
    source_prefix = f"bench-{name.lower()}-{int(time.time() * 1000)}"

    alerts_before = fetch_alert_count(base_url, operator_auth, timeout)

    successful = 0
    failed = 0
    latencies_ms: list[float] = []

    started_at = time.perf_counter()

    for i in range(event_count):
        payload = {
            "source": f"{source_prefix}-{i}",
            "details": f"benchmark-{name.lower()}-{i}",
        }

        if name == "MOVEMENT":
            payload["value"] = 1.2
        elif name == "TEMPERATURE":
            payload["value"] = 24.0
        elif name == "ACCESS":
            payload["value"] = 0.0
        else:
            raise ValueError(f"Unsupported scenario: {name}")

        req_start = time.perf_counter()
        try:
            status = http_post_json(scenario_url, payload, sensor_auth, timeout)
            if status == 202:
                successful += 1
            else:
                failed += 1
        except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError):
            failed += 1
        finally:
            req_end = time.perf_counter()
            latencies_ms.append((req_end - req_start) * 1000.0)

    ended_at = time.perf_counter()

    # Allow async alert paths to flush into alert-service before counting.
    if settle_seconds > 0:
        time.sleep(settle_seconds)

    alerts_after = fetch_alert_count(base_url, operator_auth, timeout)

    total_seconds = max(ended_at - started_at, 1e-9)
    avg_ms = (sum(latencies_ms) / len(latencies_ms)) if latencies_ms else 0.0
    throughput = successful / total_seconds

    return ScenarioResult(
        name=name,
        endpoint=endpoint,
        total_events=event_count,
        successful_events=successful,
        failed_events=failed,
        total_seconds=total_seconds,
        average_ms=avg_ms,
        throughput_eps=throughput,
        alerts_generated=max(alerts_after - alerts_before, 0),
    )


def print_console_report(results: list[ScenarioResult]) -> None:
    print("\n=== Gateway Performance Benchmark ===")
    print(
        "{:<12} {:<22} {:>7} {:>7} {:>7} {:>12} {:>12} {:>14} {:>10}".format(
            "SCENARIO",
            "ENDPOINT",
            "TOTAL",
            "OK",
            "FAIL",
            "TOTAL(s)",
            "AVG(ms)",
            "THROUGHPUT(e/s)",
            "ALERTS",
        )
    )

    for row in results:
        print(
            "{:<12} {:<22} {:>7} {:>7} {:>7} {:>12.3f} {:>12.2f} {:>14.2f} {:>10}".format(
                row.name,
                row.endpoint,
                row.total_events,
                row.successful_events,
                row.failed_events,
                row.total_seconds,
                row.average_ms,
                row.throughput_eps,
                row.alerts_generated,
            )
        )


def write_csv(path: Path, results: list[ScenarioResult]) -> None:
    with path.open("w", newline="", encoding="utf-8") as fp:
        writer = csv.writer(fp)
        writer.writerow(
            [
                "scenario",
                "endpoint",
                "total_events",
                "successful_events",
                "failed_events",
                "total_seconds",
                "average_ms",
                "throughput_events_per_second",
                "alerts_generated",
            ]
        )

        for r in results:
            writer.writerow(
                [
                    r.name,
                    r.endpoint,
                    r.total_events,
                    r.successful_events,
                    r.failed_events,
                    f"{r.total_seconds:.6f}",
                    f"{r.average_ms:.3f}",
                    f"{r.throughput_eps:.3f}",
                    r.alerts_generated,
                ]
            )


def write_markdown(path: Path, results: list[ScenarioResult], base_url: str, events: int) -> None:
    lines: list[str] = []
    lines.append("# Gateway Performance Benchmark")
    lines.append("")
    lines.append(f"- Date: {datetime.now().isoformat(timespec='seconds')}")
    lines.append(f"- Gateway base URL: `{base_url}`")
    lines.append(f"- Events per scenario: `{events}`")
    lines.append("")
    lines.append("| Scenario | Endpoint | Total | OK | Fail | Total (s) | Avg (ms) | Throughput (e/s) | Alerts |")
    lines.append("|---|---|---:|---:|---:|---:|---:|---:|---:|")

    for r in results:
        lines.append(
            f"| {r.name} | `{r.endpoint}` | {r.total_events} | {r.successful_events} | {r.failed_events} "
            f"| {r.total_seconds:.3f} | {r.average_ms:.2f} | {r.throughput_eps:.2f} | {r.alerts_generated} |"
        )

    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run a lightweight benchmark through gateway-service")
    parser.add_argument("--base-url", default="http://localhost:8080", help="Gateway base URL")
    parser.add_argument("-n", "--events", type=int, default=100, help="Events per scenario")
    parser.add_argument("--timeout", type=float, default=5.0, help="HTTP timeout in seconds")
    parser.add_argument(
        "--settle-seconds",
        type=float,
        default=1.0,
        help="Wait time after each scenario to let async alerts flush",
    )
    parser.add_argument(
        "--output-prefix",
        default=None,
        help="Output file prefix (without extension). Default: benchmark-results-<timestamp>",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if args.events <= 0:
        raise SystemExit("--events must be > 0")

    sensor_auth = auth_header("sensor-node", "sensor-pass")
    operator_auth = auth_header("operator", "operator-pass")

    scenarios = [
        ("MOVEMENT", "/api/movement/events"),
        ("TEMPERATURE", "/api/temperature/events"),
        ("ACCESS", "/api/access/events"),
    ]

    results: list[ScenarioResult] = []

    for name, endpoint in scenarios:
        result = run_scenario(
            name=name,
            endpoint=endpoint,
            event_count=args.events,
            base_url=args.base_url.rstrip("/"),
            sensor_auth=sensor_auth,
            operator_auth=operator_auth,
            timeout=args.timeout,
            settle_seconds=args.settle_seconds,
        )
        results.append(result)

    print_console_report(results)

    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    prefix = args.output_prefix or f"benchmark-results-{timestamp}"

    csv_path = Path(f"{prefix}.csv")
    md_path = Path(f"{prefix}.md")

    write_csv(csv_path, results)
    write_markdown(md_path, results, args.base_url.rstrip("/"), args.events)

    print(f"\nCSV report: {csv_path}")
    print(f"Markdown report: {md_path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

