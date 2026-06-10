import csv
import math
import re
import sys
from pathlib import Path


def parse_float(value, default=0.0):
    if value is None or value == "":
        return default
    try:
        return float(value)
    except ValueError:
        return default


def parse_int(value, default=0):
    if value is None or value == "":
        return default
    try:
        return int(float(value))
    except ValueError:
        return default


def read_aggregated_row(stats_csv: Path):
    with stats_csv.open("r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row.get("Name") == "Aggregated":
                return row
    raise RuntimeError(f"Aggregated row not found in {stats_csv}")


def discover_results(results_dir: Path):
    rows = []
    for stats_csv in sorted(results_dir.glob("users-*_stats.csv")):
        match = re.search(r"users-(\d+)_stats\.csv$", stats_csv.name)
        if not match:
            continue
        users = int(match.group(1))
        row = read_aggregated_row(stats_csv)
        request_count = parse_int(row.get("Request Count"))
        failure_count = parse_int(row.get("Failure Count"))
        avg_response = parse_float(row.get("Average Response Time"))
        median_response = parse_float(row.get("Median Response Time"))
        p95_response = parse_float(row.get("95%"))
        rps = parse_float(row.get("Requests/s"))
        fail_rate = (failure_count / request_count * 100.0) if request_count else 0.0
        rows.append(
            {
                "users": users,
                "request_count": request_count,
                "failure_count": failure_count,
                "error_rate_pct": fail_rate,
                "avg_response_ms": avg_response,
                "median_response_ms": median_response,
                "p95_response_ms": p95_response,
                "rps": rps,
                "report_html": f"users-{users:03}.html",
            }
        )
    if not rows:
        raise RuntimeError(f"No stats files found in {results_dir}")
    rows.sort(key=lambda item: item["users"])
    return rows


def write_summary_csv(rows, output_path: Path):
    fieldnames = [
        "users",
        "request_count",
        "failure_count",
        "error_rate_pct",
        "avg_response_ms",
        "median_response_ms",
        "p95_response_ms",
        "rps",
        "report_html",
    ]
    with output_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def write_summary_md(rows, output_path: Path):
    lines = [
        "# Load test summary",
        "",
        "| Users | Requests | Failures | Error rate, % | Avg response, ms | P95 response, ms | RPS |",
        "|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for row in rows:
        lines.append(
            f"| {row['users']} | {row['request_count']} | {row['failure_count']} | "
            f"{row['error_rate_pct']:.2f} | {row['avg_response_ms']:.2f} | "
            f"{row['p95_response_ms']:.2f} | {row['rps']:.2f} |"
        )
    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "The table shows how increasing the number of simultaneous users affects the backend.",
            "The main indicators for the diploma are average response time, P95 response time,",
            "throughput in requests per second, and the percentage of failed requests.",
        ]
    )
    output_path.write_text("\n".join(lines), encoding="utf-8")


def _scale_points(values, x0, y0, width, height):
    if not values:
        return []
    max_value = max(values)
    min_value = min(values)
    if math.isclose(max_value, min_value):
        max_value += 1.0
        min_value = 0.0
    step_x = width / max(1, len(values) - 1)
    points = []
    for idx, value in enumerate(values):
        x = x0 + idx * step_x
        ratio = (value - min_value) / (max_value - min_value)
        y = y0 + height - ratio * height
        points.append((x, y))
    return points, min_value, max_value


def _polyline(points):
    return " ".join(f"{x:.1f},{y:.1f}" for x, y in points)


def write_summary_svg(rows, output_path: Path):
    width = 1100
    height = 760
    margin_left = 90
    margin_right = 40
    plot_width = width - margin_left - margin_right
    chart_height = 240
    top1 = 70
    top2 = 410

    users = [row["users"] for row in rows]
    avg_values = [row["avg_response_ms"] for row in rows]
    p95_values = [row["p95_response_ms"] for row in rows]
    rps_values = [row["rps"] for row in rows]
    err_values = [row["error_rate_pct"] for row in rows]

    avg_points, avg_min, avg_max = _scale_points(avg_values, margin_left, top1, plot_width, chart_height)
    p95_points, p95_min, p95_max = _scale_points(p95_values, margin_left, top1, plot_width, chart_height)
    rps_points, rps_min, rps_max = _scale_points(rps_values, margin_left, top2, plot_width, chart_height)
    err_points, err_min, err_max = _scale_points(err_values, margin_left, top2, plot_width, chart_height)

    x_labels = []
    if len(users) == 1:
        x_positions = [margin_left + plot_width / 2]
    else:
        step_x = plot_width / (len(users) - 1)
        x_positions = [margin_left + idx * step_x for idx in range(len(users))]
    for x, user in zip(x_positions, users):
        x_labels.append(f'<text x="{x:.1f}" y="{top1 + chart_height + 35}" text-anchor="middle" class="axis">{user}</text>')

    x_labels_bottom = []
    for x, user in zip(x_positions, users):
        x_labels_bottom.append(f'<text x="{x:.1f}" y="{top2 + chart_height + 35}" text-anchor="middle" class="axis">{user}</text>')

    top_grid = []
    bottom_grid = []
    for idx in range(5):
        y1 = top1 + idx * chart_height / 4
        y2 = top2 + idx * chart_height / 4
        top_grid.append(f'<line x1="{margin_left}" y1="{y1:.1f}" x2="{margin_left + plot_width}" y2="{y1:.1f}" class="grid"/>')
        bottom_grid.append(f'<line x1="{margin_left}" y1="{y2:.1f}" x2="{margin_left + plot_width}" y2="{y2:.1f}" class="grid"/>')

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <style>
    .title {{ font: 700 28px Arial, sans-serif; fill: #1f2937; }}
    .subtitle {{ font: 600 18px Arial, sans-serif; fill: #374151; }}
    .axis {{ font: 14px Arial, sans-serif; fill: #4b5563; }}
    .legend {{ font: 14px Arial, sans-serif; fill: #374151; }}
    .grid {{ stroke: #e5e7eb; stroke-width: 1; }}
    .border {{ stroke: #9ca3af; stroke-width: 1; fill: none; }}
    .avg {{ fill: none; stroke: #2563eb; stroke-width: 3; }}
    .p95 {{ fill: none; stroke: #dc2626; stroke-width: 3; }}
    .rps {{ fill: none; stroke: #059669; stroke-width: 3; }}
    .err {{ fill: none; stroke: #d97706; stroke-width: 3; stroke-dasharray: 7 5; }}
    .point-blue {{ fill: #2563eb; }}
    .point-red {{ fill: #dc2626; }}
    .point-green {{ fill: #059669; }}
    .point-orange {{ fill: #d97706; }}
  </style>
  <rect x="0" y="0" width="{width}" height="{height}" fill="#ffffff"/>
  <text x="{margin_left}" y="35" class="title">Dependence of backend performance on the number of users</text>

  <text x="{margin_left}" y="{top1 - 20}" class="subtitle">Response time, ms</text>
  {''.join(top_grid)}
  <rect x="{margin_left}" y="{top1}" width="{plot_width}" height="{chart_height}" class="border"/>
  <polyline points="{_polyline(avg_points)}" class="avg"/>
  <polyline points="{_polyline(p95_points)}" class="p95"/>
  {''.join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="4" class="point-blue"/>' for x, y in avg_points)}
  {''.join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="4" class="point-red"/>' for x, y in p95_points)}
  <text x="{margin_left + plot_width + 10}" y="{top1 + 10}" class="axis">max {max(avg_max, p95_max):.1f}</text>
  <text x="{margin_left + plot_width + 10}" y="{top1 + chart_height}" class="axis">min {min(avg_min, p95_min):.1f}</text>
  {''.join(x_labels)}
  <text x="{margin_left + plot_width / 2:.1f}" y="{top1 + chart_height + 60}" text-anchor="middle" class="axis">Concurrent users</text>

  <text x="{margin_left}" y="{top2 - 20}" class="subtitle">Throughput and error rate</text>
  {''.join(bottom_grid)}
  <rect x="{margin_left}" y="{top2}" width="{plot_width}" height="{chart_height}" class="border"/>
  <polyline points="{_polyline(rps_points)}" class="rps"/>
  <polyline points="{_polyline(err_points)}" class="err"/>
  {''.join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="4" class="point-green"/>' for x, y in rps_points)}
  {''.join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="4" class="point-orange"/>' for x, y in err_points)}
  <text x="{margin_left + plot_width + 10}" y="{top2 + 10}" class="axis">RPS max {rps_max:.1f}</text>
  <text x="{margin_left + plot_width + 10}" y="{top2 + 30}" class="axis">Errors max {err_max:.2f}%</text>
  {''.join(x_labels_bottom)}
  <text x="{margin_left + plot_width / 2:.1f}" y="{top2 + chart_height + 60}" text-anchor="middle" class="axis">Concurrent users</text>

  <line x1="{margin_left}" y1="705" x2="{margin_left + 24}" y2="705" class="avg"/><text x="{margin_left + 32}" y="710" class="legend">Average response time</text>
  <line x1="{margin_left + 260}" y1="705" x2="{margin_left + 284}" y2="705" class="p95"/><text x="{margin_left + 292}" y="710" class="legend">P95 response time</text>
  <line x1="{margin_left + 500}" y1="705" x2="{margin_left + 524}" y2="705" class="rps"/><text x="{margin_left + 532}" y="710" class="legend">Requests per second</text>
  <line x1="{margin_left + 740}" y1="705" x2="{margin_left + 764}" y2="705" class="err"/><text x="{margin_left + 772}" y="710" class="legend">Error rate</text>
</svg>
"""
    output_path.write_text(svg, encoding="utf-8")


def main():
    if len(sys.argv) != 2:
        print("Usage: summarize_results.py <results_dir>", file=sys.stderr)
        sys.exit(1)

    results_dir = Path(sys.argv[1]).resolve()
    rows = discover_results(results_dir)

    write_summary_csv(rows, results_dir / "summary.csv")
    write_summary_md(rows, results_dir / "summary.md")
    write_summary_svg(rows, results_dir / "summary.svg")

    print(f"Summary written to {results_dir}")


if __name__ == "__main__":
    main()
