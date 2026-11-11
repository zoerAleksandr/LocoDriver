#!/usr/bin/env python3
"""Build the advisor reference JSON for the Chum–Labytnangi branch."""

import argparse
import json
import math
import xml.etree.ElementTree as ET
from pathlib import Path


def f(node: ET.Element, name: str) -> float:
    return float((node.findtext(name) or "0").replace(",", "."))


def interp(points, km):
    if km <= points[0][0]:
        return points[0][1]
    if km >= points[-1][0]:
        return points[-1][1]
    for a, b in zip(points, points[1:]):
        if a[0] <= km <= b[0]:
            q = (km - a[0]) / (b[0] - a[0])
            return a[1] + q * (b[1] - a[1])
    return points[-1][1]


def distance_km(a, b):
    r = 6_371_000.0
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dp = p2 - p1
    dl = math.radians(b[1] - a[1])
    q = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(q)) / 1000.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("source", type=Path)
    ap.add_argument("output", type=Path)
    args = ap.parse_args()
    source = args.source

    data_root = ET.parse(source / "data.xml").getroot()
    data_sector = next(s for s in data_root.findall("./sector") if s.attrib.get("id") == "47")
    raw_route = [(f(w, "lat"), f(w, "lon"), f(w, "ord") / 1000.0) for w in data_sector.findall("./wpt")]
    start_km = raw_route[0][2]
    chum = (67.091687, 63.183759)  # published Inta–Vorkuta section, station Chum
    axis_offset = distance_km(chum, raw_route[0][:2])
    route = [(chum[0], chum[1], 0.0)] + [
        (lat, lon, round(km - start_km + axis_offset, 3)) for lat, lon, km in raw_route
    ]
    length = route[-1][2]

    profile_root = ET.parse(source / "profile.xml").getroot()
    profile_sector = next(s for s in profile_root.findall("./sector") if s.attrib.get("id") == "47")
    prof = []
    height = 0.0
    for pt in profile_sector.findall("./pt"):
        km = (float(pt.findtext("start") or 0) + 1000.0) / 1000.0 + axis_offset
        height = round(height, 3)
        grad = float((pt.findtext("grad") or "0").replace(",", "."))
        length_m = float((pt.findtext("len") or "0").replace(",", "."))
        prof.append([round(km, 3), height, grad])
        height += length_m / 1000.0 * grad
    # The source profile has a short pre-start point at -1 km; trim it to the axis.
    prof = [p for p in prof if p[0] <= length + 0.001]
    if prof and prof[0][0] > 0:
        prof.insert(0, [0.0, prof[0][1], prof[0][2]])

    # The two source files describe the two physical tracks in the Chum→Labytnangi direction.
    signals = []
    stations = []
    crossings = []
    for filename, facing in (
        ("1n.xml", 1),
        ("2.xml", 1),
        ("1.xml", -1),
        ("2n.xml", -1),
    ):
        root = ET.parse(source / filename).getroot()
        sector = next(s for s in root.findall("./sector") if s.attrib.get("id") == "47")
        for w in sector.findall("./wpt"):
            name = (w.findtext("name") or "").strip()
            km = (float(w.findtext("coordinate") or 0) - 90.0) / 1000.0 + axis_offset
            if km < -0.01 or km > length + 0.5:
                continue
            kind = (w.findtext("type") or "").strip()
            if kind == "2" and name:
                if name == "ЧУМ":
                    km = 0.0
                stations.append([km, name])
            elif kind == "1" and name:
                # In the app `track` is the signal's parity/liter, not the
                # physical rail: Ч = even, Н = odd. The two source files for
                # parallel tracks therefore describe one logical signal pair
                # and are deduplicated below.
                track = "E" if name.startswith("Ч") else "O" if name.startswith("Н") else "E"
                signals.append({"km": km, "label": name, "track": track, "kind": "pass", "facing": facing})
            elif kind == "5":
                crossings.append({"km": km, "label": "Переезд", "kind": "crossing"})

    # Deduplicate stations/signals present on both track descriptions.
    # The two tracks contain the same station twice (entrance/exit). Keep the
    # first point along the section as the station marker; signals preserve
    # both track-specific positions.
    stations = sorted({name: (round(km, 3), name) for km, name in stations}.values())
    signals = sorted({(round(x["km"], 3), x["label"], x["track"], x["facing"]): x for x in signals}.values(), key=lambda x: (x["km"], x["track"], x["facing"]))
    crossings = sorted({x["label"]: x for x in crossings}.values(), key=lambda x: x["km"])

    # Add source-side fields for the follow-up DB update; the legacy importer ignores them.
    geometry = [[lat, lon, km] for lat, lon, km in route]
    payload = {
        "id": "severny-chum-labytnangi-v1",
        "name": "Чум — Лабытнанги",
        "total": length,
        # Chum is the 2202 km anchor; the source distance is kilometre-based,
        # so the second anchor keeps the continuous numbering to Labytnangi.
        "km": {"mode": "anchor", "anchors": [[0.0, 2202.0], [length, 2202.0 + length]]},
        "route": geometry,
        "prof": prof,
        "stations": stations,
        "signals": signals,
        "crossings": crossings,
        "brakeTests": [],
        "gsrc": "reference_xml",
        "gcov": 1.0,
    }
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=1), encoding="utf-8")
    print(json.dumps({"lengthKm": length, "stations": len(stations), "signals": len(signals), "crossings": len(crossings), "profilePoints": len(prof)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
